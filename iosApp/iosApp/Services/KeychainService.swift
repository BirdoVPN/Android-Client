import Foundation
import Security

/// Secure storage for tokens and credentials using the iOS Keychain.
final class KeychainService: @unchecked Sendable {
    static let shared = KeychainService()

    private let service = "app.birdo.vpn"

    // MARK: - Public Accessors

    var accessToken: String? {
        get { read(key: "access_token") }
    }

    var refreshToken: String? {
        get { read(key: "refresh_token") }
    }

    var userEmail: String? {
        get { read(key: "user_email") }
    }

    // MARK: - Save

    func save(accessToken: String, refreshToken: String, email: String?) {
        write(key: "access_token", value: accessToken)
        write(key: "refresh_token", value: refreshToken)
        if let email {
            write(key: "user_email", value: email)
        } else {
            delete(key: "user_email")
        }
    }

    // MARK: - Clear

    func clear() {
        delete(key: "access_token")
        delete(key: "refresh_token")
        delete(key: "user_email")
    }

    // MARK: - Keychain Operations

    private func write(key: String, value: String) {
        guard let data = value.data(using: .utf8) else { return }

        // Delete existing entry first
        delete(key: key)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]

        SecItemAdd(query as CFDictionary, nil)
    }

    private func read(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    private func delete(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
        SecItemDelete(query as CFDictionary)
    }
}
