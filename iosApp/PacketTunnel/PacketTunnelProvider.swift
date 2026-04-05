import NetworkExtension
import os.log

/// WireGuard packet tunnel provider.
/// In production, this links against WireGuardKit to run the Go tunnel.
class PacketTunnelProvider: NEPacketTunnelProvider {
    private let log = OSLog(subsystem: "app.birdo.vpn.tunnel", category: "tunnel")

    override func startTunnel(
        options: [String: NSObject]?,
        completionHandler: @escaping (Error?) -> Void
    ) {
        os_log("Starting Birdo VPN tunnel", log: log, type: .info)

        guard let proto = protocolConfiguration as? NETunnelProviderProtocol,
              let config = proto.providerConfiguration?["wg-config"] as? String else {
            os_log("Missing WireGuard config", log: log, type: .error)
            completionHandler(TunnelError.missingConfig)
            return
        }

        // Parse WireGuard configuration
        guard let parsed = parseWireGuardConfig(config) else {
            os_log("Invalid WireGuard config", log: log, type: .error)
            completionHandler(TunnelError.invalidConfig)
            return
        }

        // Configure tunnel network settings
        let settings = NEPacketTunnelNetworkSettings(tunnelRemoteAddress: parsed.endpoint)
        settings.mtu = NSNumber(value: parsed.mtu)

        let ipv4 = NEIPv4Settings(
            addresses: parsed.addresses.filter { $0.contains(".") },
            subnetMasks: parsed.addresses.filter { $0.contains(".") }.map { _ in "255.255.255.0" }
        )
        ipv4.includedRoutes = [NEIPv4Route.default()]
        settings.ipv4Settings = ipv4

        let ipv6Addrs = parsed.addresses.filter { $0.contains(":") }
        if !ipv6Addrs.isEmpty {
            let ipv6 = NEIPv6Settings(
                addresses: ipv6Addrs,
                networkPrefixLengths: ipv6Addrs.map { _ in 128 as NSNumber }
            )
            ipv6.includedRoutes = [NEIPv6Route.default()]
            settings.ipv6Settings = ipv6
        }

        if !parsed.dns.isEmpty {
            settings.dnsSettings = NEDNSSettings(servers: parsed.dns)
        }

        setTunnelNetworkSettings(settings) { [weak self] error in
            if let error {
                os_log("Failed to set tunnel settings: %{public}@",
                       log: self?.log ?? .default, type: .error,
                       error.localizedDescription)
                completionHandler(error)
                return
            }

            // TODO: Start WireGuardKit tunnel adapter here
            // In production: WireGuardAdapter(with: self!, logHandler: ...)
            // adapter.start(tunnelConfiguration: ...) { ... }
            os_log("Tunnel settings applied, adapter start pending", log: self?.log ?? .default)
            completionHandler(nil)
        }
    }

    override func stopTunnel(
        with reason: NEProviderStopReason,
        completionHandler: @escaping () -> Void
    ) {
        os_log("Stopping tunnel, reason: %{public}d", log: log, type: .info, reason.rawValue)
        // TODO: adapter.stop { completionHandler() }
        completionHandler()
    }

    override func handleAppMessage(_ messageData: Data, completionHandler: ((Data?) -> Void)?) {
        // IPC from the main app (e.g., stats queries)
        if let command = String(data: messageData, encoding: .utf8) {
            switch command {
            case "stats":
                // Return transfer stats as JSON
                let stats = """
                {"rx": 0, "tx": 0}
                """
                completionHandler?(stats.data(using: .utf8))
            default:
                completionHandler?(nil)
            }
        } else {
            completionHandler?(nil)
        }
    }

    // MARK: - Config Parsing

    private struct ParsedConfig {
        let addresses: [String]
        let dns: [String]
        let mtu: Int
        let endpoint: String
    }

    private func parseWireGuardConfig(_ raw: String) -> ParsedConfig? {
        var addresses: [String] = []
        var dns: [String] = []
        var mtu = 1420
        var endpoint = ""

        for line in raw.components(separatedBy: "\n") {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            if trimmed.hasPrefix("Address = ") {
                let val = String(trimmed.dropFirst("Address = ".count))
                // Strip CIDR suffix for the address list
                addresses.append(contentsOf: val.components(separatedBy: ",").map {
                    $0.trimmingCharacters(in: .whitespaces)
                })
            } else if trimmed.hasPrefix("DNS = ") {
                let val = String(trimmed.dropFirst("DNS = ".count))
                dns.append(contentsOf: val.components(separatedBy: ",").map {
                    $0.trimmingCharacters(in: .whitespaces)
                })
            } else if trimmed.hasPrefix("MTU = ") {
                mtu = Int(String(trimmed.dropFirst("MTU = ".count))) ?? 1420
            } else if trimmed.hasPrefix("Endpoint = ") {
                endpoint = String(trimmed.dropFirst("Endpoint = ".count))
            }
        }

        guard !endpoint.isEmpty else { return nil }
        return ParsedConfig(addresses: addresses, dns: dns, mtu: mtu, endpoint: endpoint)
    }
}

// MARK: - Errors

enum TunnelError: Error, LocalizedError {
    case missingConfig
    case invalidConfig
    case adapterFailed(String)

    var errorDescription: String? {
        switch self {
        case .missingConfig: return "Missing tunnel configuration"
        case .invalidConfig: return "Invalid tunnel configuration"
        case .adapterFailed(let msg): return "Tunnel adapter failed: \(msg)"
        }
    }
}
