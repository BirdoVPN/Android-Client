import Foundation
import NetworkExtension

/// Wraps NETunnelProviderManager for starting/stopping the WireGuard VPN tunnel.
final class VPNManager: @unchecked Sendable {
    static let shared = VPNManager()

    var onStatusChange: ((NEVPNStatus) -> Void)?

    private var manager: NETunnelProviderManager?
    private var statusObserver: NSObjectProtocol?

    init() {
        loadManager()
    }

    deinit {
        if let observer = statusObserver {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    // MARK: - Public

    func connect(config: VPNConnectionConfig) async throws {
        let mgr = try await ensureManager()

        // Build WireGuard config string
        let wgConfig = buildWireGuardConfig(config)

        let proto = NETunnelProviderProtocol()
        proto.providerBundleIdentifier = "app.birdo.vpn.tunnel"
        proto.serverAddress = config.serverAddress
        proto.providerConfiguration = ["wg-config": wgConfig]

        mgr.protocolConfiguration = proto
        mgr.isEnabled = true
        mgr.localizedDescription = "Birdo VPN"

        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            mgr.saveToPreferences { error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume() }
            }
        }

        try mgr.connection.startVPNTunnel()
    }

    func disconnect() {
        manager?.connection.stopVPNTunnel()
    }

    func currentStats() -> (rx: Int64, tx: Int64) {
        // In production, query the tunnel extension via IPC for real bytes
        return (0, 0)
    }

    // MARK: - Private

    private func loadManager() {
        NETunnelProviderManager.loadAllFromPreferences { [weak self] managers, error in
            guard let self else { return }
            if let existing = managers?.first {
                self.manager = existing
                self.observeStatus(existing)
            }
        }
    }

    private func ensureManager() async throws -> NETunnelProviderManager {
        if let mgr = manager { return mgr }

        return try await withCheckedThrowingContinuation { cont in
            NETunnelProviderManager.loadAllFromPreferences { [weak self] managers, error in
                if let error {
                    cont.resume(throwing: error)
                    return
                }
                let mgr = managers?.first ?? NETunnelProviderManager()
                self?.manager = mgr
                self?.observeStatus(mgr)
                cont.resume(returning: mgr)
            }
        }
    }

    private func observeStatus(_ manager: NETunnelProviderManager) {
        if let existing = statusObserver {
            NotificationCenter.default.removeObserver(existing)
        }
        statusObserver = NotificationCenter.default.addObserver(
            forName: .NEVPNStatusDidChange,
            object: manager.connection,
            queue: .main
        ) { [weak self] _ in
            self?.onStatusChange?(manager.connection.status)
        }
    }

    private func buildWireGuardConfig(_ config: VPNConnectionConfig) -> String {
        var lines: [String] = []
        lines.append("[Interface]")
        lines.append("PrivateKey = \(config.privateKey)")
        for addr in config.addresses {
            lines.append("Address = \(addr)")
        }
        if !config.dns.isEmpty {
            lines.append("DNS = \(config.dns.joined(separator: ", "))")
        }
        if let mtu = config.mtu {
            lines.append("MTU = \(mtu)")
        }

        lines.append("")
        lines.append("[Peer]")
        lines.append("PublicKey = \(config.publicKey)")
        if let psk = config.presharedKey {
            lines.append("PresharedKey = \(psk)")
        }
        lines.append("Endpoint = \(config.serverAddress):\(config.serverPort)")
        for ip in config.allowedIPs {
            lines.append("AllowedIPs = \(ip)")
        }
        lines.append("PersistentKeepalive = 25")

        return lines.joined(separator: "\n")
    }
}
