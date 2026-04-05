import SwiftUI
import BirdoShared

/// Main connect screen with power button, stats, and server selector.
struct HomeView: View {
    @EnvironmentObject var vpnVM: VpnViewModel
    @EnvironmentObject var authVM: AuthViewModel
    @EnvironmentObject var settingsVM: SettingsViewModel

    var body: some View {
        VStack(spacing: 0) {
            // Header
            header

            ScrollView {
                VStack(spacing: 24) {
                    Spacer().frame(height: 32)

                    statusBadge

                    if vpnVM.isConnected, let server = vpnVM.connectedServerName {
                        Text(server)
                            .font(.subheadline)
                            .foregroundColor(BirdoTheme.white60)
                    }

                    Spacer().frame(height: 4)

                    connectionButton

                    if vpnVM.isConnected {
                        statsRow
                        securityBadges
                    }

                    if vpnVM.killSwitchActive {
                        killSwitchBanner
                    }

                    if let error = vpnVM.errorMessage {
                        errorBanner(error)
                    }

                    Spacer()

                    serverSelector
                }
                .padding(.horizontal, 32)
            }
        }
        .background(BirdoTheme.black)
    }

    // MARK: - Header

    private var header: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Birdo VPN")
                    .font(.headline)
                    .foregroundColor(.white)
                Spacer()
                if let email = authVM.userEmail {
                    Text(email)
                        .font(.caption2)
                        .foregroundColor(BirdoTheme.white40)
                        .lineLimit(1)
                        .frame(maxWidth: 140, alignment: .trailing)
                }
                Button(action: { authVM.logout() }) {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .foregroundColor(BirdoTheme.white40)
                }
            }
            .padding(.horizontal, 16)
            .frame(height: 48)

            Divider().background(BirdoTheme.white05)
        }
        .background(BirdoTheme.glassStrong)
    }

    // MARK: - Status Badge

    private var statusBadge: some View {
        let (bg, fg, icon, label) = statusConfig
        return HStack(spacing: 8) {
            if vpnVM.isConnecting || vpnVM.isDisconnecting {
                ProgressView()
                    .scaleEffect(0.7)
                    .tint(fg)
            } else {
                Image(systemName: icon)
                    .font(.caption)
            }
            Text(label)
                .font(.subheadline.weight(.medium))
        }
        .foregroundColor(fg)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(bg)
        .clipShape(Capsule())
        .overlay(Capsule().stroke(fg.opacity(0.2), lineWidth: 1))
    }

    private var statusConfig: (Color, Color, String, String) {
        if vpnVM.isConnected {
            return (BirdoTheme.greenBg, BirdoTheme.greenLight, "wifi", "Protected")
        } else if vpnVM.isConnecting {
            return (BirdoTheme.yellowBg, BirdoTheme.yellowLight, "wifi", "Connecting…")
        } else if vpnVM.isDisconnecting {
            return (BirdoTheme.yellowBg, BirdoTheme.yellowLight, "wifi.slash", "Disconnecting…")
        } else {
            return (BirdoTheme.white05, BirdoTheme.white40, "wifi.slash", "Not Connected")
        }
    }

    // MARK: - Connection Button

    private var connectionButton: some View {
        let size: CGFloat = 128
        let isActive = vpnVM.isConnected
        let bg = isActive ? BirdoTheme.green : (vpnVM.isConnecting ? BirdoTheme.yellow : BirdoTheme.white10)

        return ZStack {
            if isActive {
                Circle()
                    .fill(BirdoTheme.green.opacity(0.15))
                    .frame(width: size * 1.4, height: size * 1.4)
                    .scaleEffect(1.0)
                    .animation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true), value: isActive)
            }

            Button(action: {
                if vpnVM.isConnected {
                    vpnVM.disconnect()
                } else if !vpnVM.isConnecting && !vpnVM.isDisconnecting {
                    vpnVM.connect()
                }
            }) {
                ZStack {
                    Circle()
                        .fill(bg)
                        .frame(width: size, height: size)
                        .shadow(color: isActive ? BirdoTheme.greenShadow : .clear, radius: 24)

                    if !isActive && !vpnVM.isConnecting {
                        Circle()
                            .stroke(BirdoTheme.white20, lineWidth: 1)
                            .frame(width: size, height: size)
                    }

                    if vpnVM.isConnecting || vpnVM.isDisconnecting {
                        ProgressView()
                            .scaleEffect(1.5)
                            .tint(.white)
                    } else {
                        Image(systemName: "power")
                            .font(.system(size: 42))
                            .foregroundColor(isActive ? .white : BirdoTheme.white60)
                    }
                }
            }
            .buttonStyle(.plain)
        }
        .frame(width: size * 1.6, height: size * 1.6)
    }

    // MARK: - Stats

    private var statsRow: some View {
        HStack(spacing: 8) {
            StatsCard(label: "Duration", value: vpnVM.formattedDuration)
            StatsCard(label: "Download", value: vpnVM.formattedRx)
            StatsCard(label: "Upload", value: vpnVM.formattedTx)
        }
        .transition(.opacity.combined(with: .move(edge: .bottom)))
    }

    // MARK: - Security Badges

    private var securityBadges: some View {
        HStack(spacing: 12) {
            if settingsVM.killSwitchEnabled {
                SecurityBadge(icon: "shield.fill", label: "Kill Switch", color: BirdoTheme.greenLight)
            }
            if vpnVM.stealthActive {
                SecurityBadge(icon: "eye.slash.fill", label: "Stealth", color: BirdoTheme.blue)
            }
            if vpnVM.quantumActive {
                SecurityBadge(icon: "lock.fill", label: "Quantum", color: BirdoTheme.purpleLight)
            }
        }
    }

    // MARK: - Kill Switch Banner

    private var killSwitchBanner: some View {
        HStack(spacing: 8) {
            Image(systemName: "shield.fill")
                .font(.caption)
            Text("Kill switch is blocking all traffic")
                .font(.caption)
        }
        .foregroundColor(BirdoTheme.red)
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(BirdoTheme.redBg)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(BirdoTheme.red.opacity(0.2), lineWidth: 1))
    }

    // MARK: - Error Banner

    private func errorBanner(_ message: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.caption)
            Text(message)
                .font(.caption)
        }
        .foregroundColor(BirdoTheme.red)
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(BirdoTheme.redBg)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Server Selector

    private var serverSelector: some View {
        NavigationLink(destination: ServerListView()) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 8)
                    .fill(BirdoTheme.white10)
                    .frame(width: 40, height: 40)
                    .overlay(
                        Text(vpnVM.selectedServer.map { flagEmoji($0.countryCode) } ?? "🌐")
                            .font(.title3)
                    )

                VStack(alignment: .leading, spacing: 2) {
                    Text(vpnVM.selectedServer?.name ?? "Select Server")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.white)

                    if let server = vpnVM.selectedServer {
                        Text("\(server.city.isEmpty ? server.country : server.city) · \(server.load)% load")
                            .font(.caption)
                            .foregroundColor(BirdoTheme.white60)
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .foregroundColor(BirdoTheme.white40)
                    .font(.caption)
            }
            .padding(16)
            .background(BirdoTheme.glassLight)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(BirdoTheme.border, lineWidth: 1))
        }
        .disabled(vpnVM.isConnected || vpnVM.isConnecting)
        .padding(.bottom, 16)
    }
}

// MARK: - Supporting Views

private struct StatsCard: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 4) {
            Text(label)
                .font(.caption2)
                .foregroundColor(BirdoTheme.white60)
            Text(value)
                .font(.caption.weight(.semibold))
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(BirdoTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(BirdoTheme.border, lineWidth: 1))
    }
}

private struct SecurityBadge: View {
    let icon: String
    let label: String
    let color: Color

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption2)
                .foregroundColor(color)
            Text(label)
                .font(.caption2)
                .foregroundColor(BirdoTheme.white60)
        }
    }
}

/// Convert a 2-letter country code to a flag emoji.
func flagEmoji(_ code: String) -> String {
    guard code.count == 2 else { return "🌐" }
    let base: UInt32 = 0x1F1E6
    let chars = code.uppercased().unicodeScalars.compactMap {
        UnicodeScalar(base + $0.value - UnicodeScalar("A").value)
    }
    return String(chars.map { Character($0) })
}
