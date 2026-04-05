import Foundation
import BirdoShared

/// HTTP client for the Birdo VPN API. Uses shared KMP model types.
final class APIClient: @unchecked Sendable {
    static let shared = APIClient()

    private let baseURL: URL
    private let session: URLSession
    private let keychain: KeychainService
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    /// Token refresh lock to prevent concurrent refresh races
    private let refreshLock = NSLock()
    private var isRefreshing = false

    init(
        baseURL: URL = URL(string: "https://api.birdo.app")!,
        keychain: KeychainService = .shared
    ) {
        self.baseURL = baseURL
        self.keychain = keychain
        self.decoder = JSONDecoder()
        self.encoder = JSONEncoder()

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
    }

    // MARK: - Auth

    func login(email: String, password: String) async throws -> LoginResultType {
        let body = try encoder.encode(LoginBody(email: email, password: password))
        let data = try await post(path: "/auth/login", body: body, authenticated: false)

        // Check if 2FA required
        if let parsed = try? decoder.decode(TwoFactorRequiredResponse.self, from: data),
           parsed.requiresTwoFactor {
            return .twoFactorRequired
        }

        let tokens = try decoder.decode(TokensResponse.self, from: data)
        return .success(TokenPairData(accessToken: tokens.accessToken, refreshToken: tokens.refreshToken))
    }

    func verifyTwoFactor(email: String, password: String, code: String) async throws -> TokenPairData {
        let body = try encoder.encode(TwoFactorBody(email: email, password: password, code: code))
        let data = try await post(path: "/auth/2fa/verify", body: body, authenticated: false)
        let tokens = try decoder.decode(TokensResponse.self, from: data)
        return TokenPairData(accessToken: tokens.accessToken, refreshToken: tokens.refreshToken)
    }

    func loginAnonymous() async throws -> TokenPairData {
        let data = try await post(path: "/auth/anonymous", body: nil, authenticated: false)
        let tokens = try decoder.decode(TokensResponse.self, from: data)
        return TokenPairData(accessToken: tokens.accessToken, refreshToken: tokens.refreshToken)
    }

    func deleteAccount() async throws {
        _ = try await performRequest(method: "DELETE", path: "/auth/account", body: nil, authenticated: true)
    }

    // MARK: - Servers

    func fetchServers() async throws -> [ServerInfo] {
        let data = try await get(path: "/servers")
        return try decoder.decode([ServerInfo].self, from: data)
    }

    // MARK: - VPN Config

    func getConnectConfig(serverId: String) async throws -> VPNConnectionConfig {
        let body = try encoder.encode(ConnectBody(serverId: serverId))
        let data = try await post(path: "/vpn/connect", body: body, authenticated: true)
        return try decoder.decode(VPNConnectionConfig.self, from: data)
    }

    func getMultiHopConfig(entryId: String, exitId: String) async throws -> VPNConnectionConfig {
        let body = try encoder.encode(MultiHopBody(entryId: entryId, exitId: exitId))
        let data = try await post(path: "/vpn/multi-hop", body: body, authenticated: true)
        return try decoder.decode(VPNConnectionConfig.self, from: data)
    }

    // MARK: - Port Forwarding

    func createPortForward(port: Int, proto: String) async throws -> PortForwardEntry {
        let body = try encoder.encode(PortForwardBody(internalPort: port, proto: proto))
        let data = try await post(path: "/vpn/port-forward", body: body, authenticated: true)
        return try decoder.decode(PortForwardEntry.self, from: data)
    }

    func deletePortForward(id: String) async throws {
        _ = try await performRequest(
            method: "DELETE",
            path: "/vpn/port-forward/\(id.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? id)",
            body: nil,
            authenticated: true
        )
    }

    // MARK: - Speed Test

    func measureLatency() async throws -> (latencyMs: Int, jitterMs: Int) {
        var latencies: [Int] = []
        for _ in 0..<5 {
            let start = CFAbsoluteTimeGetCurrent()
            _ = try await get(path: "/ping")
            let ms = Int((CFAbsoluteTimeGetCurrent() - start) * 1000)
            latencies.append(ms)
        }
        let avg = latencies.reduce(0, +) / max(latencies.count, 1)
        let jitter = latencies.count > 1
            ? latencies.map { abs($0 - avg) }.reduce(0, +) / (latencies.count - 1)
            : 0
        return (avg, jitter)
    }

    func measureDownload() async throws -> Double {
        let start = CFAbsoluteTimeGetCurrent()
        let data = try await get(path: "/speedtest/download")
        let elapsed = CFAbsoluteTimeGetCurrent() - start
        let bits = Double(data.count) * 8
        return bits / elapsed / 1_000_000 // Mbps
    }

    func measureUpload() async throws -> Double {
        let payload = Data(repeating: 0, count: 1_000_000) // 1 MB
        let start = CFAbsoluteTimeGetCurrent()
        _ = try await post(path: "/speedtest/upload", body: payload, authenticated: true)
        let elapsed = CFAbsoluteTimeGetCurrent() - start
        let bits = Double(payload.count) * 8
        return bits / elapsed / 1_000_000
    }

    // MARK: - Token Refresh

    private func refreshTokens() async throws {
        guard let refresh = keychain.refreshToken else {
            throw APIError.unauthorized
        }
        let body = try encoder.encode(RefreshBody(refreshToken: refresh))
        let data = try await post(path: "/auth/refresh", body: body, authenticated: false)
        let tokens = try decoder.decode(TokensResponse.self, from: data)
        keychain.save(accessToken: tokens.accessToken,
                      refreshToken: tokens.refreshToken,
                      email: keychain.userEmail)
    }

    // MARK: - Core HTTP

    private func get(path: String) async throws -> Data {
        try await performRequest(method: "GET", path: path, body: nil, authenticated: true)
    }

    private func post(path: String, body: Data?, authenticated: Bool) async throws -> Data {
        try await performRequest(method: "POST", path: path, body: body, authenticated: authenticated)
    }

    private func performRequest(
        method: String,
        path: String,
        body: Data?,
        authenticated: Bool
    ) async throws -> Data {
        guard let url = URL(string: path, relativeTo: baseURL) else {
            throw APIError.invalidURL
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = body

        if authenticated, let token = keychain.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await session.data(for: request)

        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }

        // Handle 401 — attempt token refresh once
        if http.statusCode == 401 && authenticated {
            var shouldRefresh = false
            refreshLock.lock()
            if !isRefreshing {
                isRefreshing = true
                shouldRefresh = true
            }
            refreshLock.unlock()

            if shouldRefresh {
                defer {
                    refreshLock.lock()
                    isRefreshing = false
                    refreshLock.unlock()
                }
                try await refreshTokens()
            }

            // Retry with new token
            var retry = URLRequest(url: url)
            retry.httpMethod = method
            retry.setValue("application/json", forHTTPHeaderField: "Content-Type")
            retry.httpBody = body
            if let newToken = keychain.accessToken {
                retry.setValue("Bearer \(newToken)", forHTTPHeaderField: "Authorization")
            }
            let (retryData, retryResponse) = try await session.data(for: retry)
            guard let retryHttp = retryResponse as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            guard (200...299).contains(retryHttp.statusCode) else {
                throw APIError.httpError(retryHttp.statusCode)
            }
            return retryData
        }

        guard (200...299).contains(http.statusCode) else {
            throw APIError.httpError(http.statusCode)
        }
        return data
    }
}

// MARK: - API Models

enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case unauthorized
    case httpError(Int)

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "Invalid URL"
        case .invalidResponse: return "Invalid server response"
        case .unauthorized: return "Session expired. Please log in again."
        case .httpError(let code): return "Server error (\(code))"
        }
    }
}

private struct LoginBody: Encodable {
    let email: String
    let password: String
}

private struct TwoFactorBody: Encodable {
    let email: String
    let password: String
    let code: String
}

private struct ConnectBody: Encodable {
    let serverId: String
}

private struct MultiHopBody: Encodable {
    let entryId: String
    let exitId: String
}

private struct PortForwardBody: Encodable {
    let internalPort: Int
    let proto: String
}

private struct RefreshBody: Encodable {
    let refreshToken: String
}

private struct TokensResponse: Decodable {
    let accessToken: String
    let refreshToken: String
}

private struct TwoFactorRequiredResponse: Decodable {
    let requiresTwoFactor: Bool
}

/// VPN connection configuration returned by server.
struct VPNConnectionConfig: Decodable {
    let serverAddress: String
    let serverPort: Int
    let privateKey: String
    let publicKey: String
    let presharedKey: String?
    let addresses: [String]
    let dns: [String]
    let allowedIPs: [String]
    let mtu: Int?
}
