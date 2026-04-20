import SwiftUI

/// Speed test screen — latency, jitter, download, upload.
struct SpeedTestView: View {
    @EnvironmentObject var vpnVM: VpnViewModel

    @State private var latencyMs: Int?
    @State private var jitterMs: Int?
    @State private var downloadMbps: Double?
    @State private var uploadMbps: Double?
    @State private var phase: TestPhase = .idle
    @State private var progress: Double = 0

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Progress ring
                ZStack {
                    Circle()
                        .stroke(BirdoTheme.white10, lineWidth: 8)
                        .frame(width: 140, height: 140)

                    Circle()
                        .trim(from: 0, to: progress)
                        .stroke(phaseColor, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                        .frame(width: 140, height: 140)
                        .rotationEffect(.degrees(-90))
                        .animation(.easeInOut(duration: 0.3), value: progress)

                    VStack(spacing: 4) {
                        if phase == .idle {
                            Image(systemName: "speedometer")
                                .font(.title)
                                .foregroundColor(BirdoTheme.white40)
                        } else if phase == .complete {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.title)
                                .foregroundColor(BirdoTheme.green)
                        } else {
                            Text(phaseLabel)
                                .font(.caption2)
                                .foregroundColor(BirdoTheme.white40)
                            Text("\(Int(progress * 100))%")
                                .font(.title3.weight(.bold))
                                .foregroundColor(.white)
                        }
                    }
                }
                .padding(.top, 8)

                // Results grid
                LazyVGrid(columns: [
                    GridItem(.flexible(), spacing: 12),
                    GridItem(.flexible(), spacing: 12),
                ], spacing: 12) {
                    resultCard(
                        icon: "waveform.path",
                        label: "Latency",
                        value: latencyMs.map { "\($0) ms" } ?? "—",
                        color: BirdoTheme.blue
                    )
                    resultCard(
                        icon: "waveform",
                        label: "Jitter",
                        value: jitterMs.map { "\($0) ms" } ?? "—",
                        color: BirdoTheme.purple
                    )
                    resultCard(
                        icon: "arrow.down.circle.fill",
                        label: "Download",
                        value: downloadMbps.map { String(format: "%.1f Mbps", $0) } ?? "—",
                        color: BirdoTheme.green
                    )
                    resultCard(
                        icon: "arrow.up.circle.fill",
                        label: "Upload",
                        value: uploadMbps.map { String(format: "%.1f Mbps", $0) } ?? "—",
                        color: BirdoTheme.yellow
                    )
                }

                // Action button
                Button {
                    if phase == .idle || phase == .complete {
                        startTest()
                    }
                } label: {
                    HStack {
                        if phase != .idle && phase != .complete {
                            ProgressView()
                                .progressViewStyle(.circular)
                                .tint(.white)
                                .scaleEffect(0.8)
                        }
                        Text(phase == .idle ? "Start Speed Test" :
                                phase == .complete ? "Run Again" : "Testing…")
                            .font(.headline)
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        (phase == .idle || phase == .complete)
                            ? BirdoTheme.purple
                            : BirdoTheme.white10
                    )
                    .cornerRadius(14)
                }
                .disabled(phase != .idle && phase != .complete)

                // Server info
                if vpnVM.isConnected, let server = vpnVM.selectedServer {
                    HStack(spacing: 8) {
                        Image(systemName: "info.circle")
                            .foregroundColor(BirdoTheme.white40)
                            .font(.caption)
                        Text("Testing through \(server.flag) \(server.name)")
                            .font(.caption)
                            .foregroundColor(BirdoTheme.white60)
                    }
                } else {
                    HStack(spacing: 8) {
                        Image(systemName: "exclamationmark.triangle")
                            .foregroundColor(BirdoTheme.yellow)
                            .font(.caption)
                        Text("Connect to a VPN server first for accurate results.")
                            .font(.caption)
                            .foregroundColor(BirdoTheme.white60)
                    }
                }
            }
            .padding()
        }
        .background(BirdoTheme.black.ignoresSafeArea())
        .navigationTitle("Speed Test")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Result Card

    private func resultCard(icon: String, label: String, value: String, color: Color) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            Text(label)
                .font(.caption2)
                .foregroundColor(BirdoTheme.white40)
            Text(value)
                .font(.title3.weight(.bold))
                .foregroundColor(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(BirdoTheme.surface)
        .cornerRadius(16)
    }

    // MARK: - Test Logic

    private var phaseLabel: String {
        switch phase {
        case .latency: return "Latency"
        case .download: return "Download"
        case .upload: return "Upload"
        default: return ""
        }
    }

    private var phaseColor: Color {
        switch phase {
        case .latency: return BirdoTheme.blue
        case .download: return BirdoTheme.green
        case .upload: return BirdoTheme.yellow
        case .complete: return BirdoTheme.green
        default: return BirdoTheme.white40
        }
    }

    private func startTest() {
        // Reset
        latencyMs = nil
        jitterMs = nil
        downloadMbps = nil
        uploadMbps = nil
        progress = 0
        phase = .latency

        // Delegate to ViewModel — phases update via callback
        vpnVM.runSpeedTest { result in
            withAnimation {
                self.latencyMs = result.latencyMs
                self.jitterMs = result.jitterMs
                self.downloadMbps = result.downloadMbps
                self.uploadMbps = result.uploadMbps
                self.phase = .complete
                self.progress = 1.0
            }
        } onProgress: { currentPhase, currentProgress in
            withAnimation {
                self.phase = currentPhase
                self.progress = currentProgress
            }
        }
    }
}

// MARK: - Types

enum TestPhase {
    case idle, latency, download, upload, complete
}

struct SpeedTestResult {
    let latencyMs: Int
    let jitterMs: Int
    let downloadMbps: Double
    let uploadMbps: Double
}
