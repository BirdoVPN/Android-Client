import SwiftUI

/// Subscription & plan selection screen.
struct SubscriptionView: View {
    @EnvironmentObject var authVM: AuthViewModel

    @State private var billingPeriod: BillingPeriod = .monthly
    @State private var selectedPlan: Plan?

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Current plan badge
                if let current = authVM.currentPlan {
                    HStack(spacing: 10) {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundColor(BirdoTheme.green)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Current Plan")
                                .font(.caption)
                                .foregroundColor(BirdoTheme.white40)
                            Text(current.rawValue)
                                .font(.headline)
                                .foregroundColor(.white)
                        }
                        Spacer()
                        if let expiry = authVM.subscriptionExpiry {
                            Text("Renews \(expiry)")
                                .font(.caption2)
                                .foregroundColor(BirdoTheme.white40)
                        }
                    }
                    .padding()
                    .background(BirdoTheme.surface)
                    .cornerRadius(16)
                }

                // Billing toggle
                Picker("Billing", selection: $billingPeriod) {
                    Text("Monthly").tag(BillingPeriod.monthly)
                    Text("Yearly • Save 30%").tag(BillingPeriod.yearly)
                }
                .pickerStyle(.segmented)

                // Plan cards
                ForEach(Plan.allCases) { plan in
                    planCard(plan)
                }

                // Subscribe button
                if let plan = selectedPlan {
                    Button {
                        authVM.subscribe(plan: plan, billing: billingPeriod)
                    } label: {
                        HStack {
                            Text("Subscribe to \(plan.rawValue)")
                                .font(.headline)
                            Spacer()
                            Text(plan.price(for: billingPeriod))
                                .font(.headline)
                        }
                        .foregroundColor(.white)
                        .padding(.vertical, 14)
                        .padding(.horizontal, 20)
                        .background(BirdoTheme.purple)
                        .cornerRadius(14)
                    }
                }

                // Footnote
                Text("Subscriptions are managed through the App Store. You can cancel anytime in your Apple ID settings.")
                    .font(.caption2)
                    .foregroundColor(BirdoTheme.white40)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }
            .padding()
        }
        .background(BirdoTheme.black.ignoresSafeArea())
        .navigationTitle("Subscription")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Plan Card

    private func planCard(_ plan: Plan) -> some View {
        let isSelected = selectedPlan == plan
        let isCurrent = authVM.currentPlan == plan

        return Button {
            if !isCurrent { selectedPlan = plan }
        } label: {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: plan.icon)
                        .font(.title3)
                        .foregroundColor(plan.color)
                    Text(plan.rawValue)
                        .font(.headline)
                        .foregroundColor(.white)
                    Spacer()
                    if isCurrent {
                        Text("CURRENT")
                            .font(.caption2.weight(.bold))
                            .foregroundColor(BirdoTheme.green)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(BirdoTheme.green.opacity(0.15))
                            .cornerRadius(6)
                    }
                    Text(plan.price(for: billingPeriod))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(BirdoTheme.white80)
                }

                // Features
                ForEach(plan.features, id: \.self) { feature in
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark")
                            .font(.caption2)
                            .foregroundColor(plan.color)
                        Text(feature)
                            .font(.caption)
                            .foregroundColor(BirdoTheme.white60)
                    }
                }
            }
            .padding()
            .background(
                isSelected
                    ? plan.color.opacity(0.08)
                    : BirdoTheme.surface
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? plan.color : Color.clear, lineWidth: 2)
            )
            .cornerRadius(16)
        }
        .disabled(isCurrent)
    }
}

// MARK: - Supporting Types

enum BillingPeriod {
    case monthly, yearly
}

enum Plan: String, CaseIterable, Identifiable {
    case recon = "RECON"
    case spectre = "SPECTRE"
    case phantom = "PHANTOM"
    case eagle = "EAGLE"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .recon: return "binoculars.fill"
        case .spectre: return "eye.slash.fill"
        case .phantom: return "theatermasks.fill"
        case .eagle: return "bolt.shield.fill"
        }
    }

    var color: Color {
        switch self {
        case .recon: return BirdoTheme.blue
        case .spectre: return BirdoTheme.purple
        case .phantom: return Color(hex: "#FF6B6B")
        case .eagle: return BirdoTheme.yellow
        }
    }

    var features: [String] {
        switch self {
        case .recon:
            return ["1 Device", "Standard Servers", "WireGuard Protocol"]
        case .spectre:
            return ["3 Devices", "All Servers", "Stealth Mode", "Kill Switch"]
        case .phantom:
            return ["5 Devices", "All Servers", "Multi-Hop", "Port Forwarding", "Stealth Mode"]
        case .eagle:
            return ["10 Devices", "All Servers", "All Features", "Quantum Protection", "Priority Support"]
        }
    }

    func price(for billing: BillingPeriod) -> String {
        switch (self, billing) {
        case (.recon, .monthly): return "$4.99/mo"
        case (.recon, .yearly): return "$3.49/mo"
        case (.spectre, .monthly): return "$7.99/mo"
        case (.spectre, .yearly): return "$5.59/mo"
        case (.phantom, .monthly): return "$11.99/mo"
        case (.phantom, .yearly): return "$8.39/mo"
        case (.eagle, .monthly): return "$14.99/mo"
        case (.eagle, .yearly): return "$10.49/mo"
        }
    }
}
