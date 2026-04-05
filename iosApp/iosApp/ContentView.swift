import SwiftUI

/// Root navigation container with tab bar matching Android bottom nav.
struct ContentView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @EnvironmentObject var vpnVM: VpnViewModel

    enum Tab {
        case home, servers, settings
    }

    @State private var selectedTab: Tab = .home
    @State private var hasConsented = UserDefaults.standard.bool(forKey: "hasAcceptedPrivacy")

    var body: some View {
        Group {
            if !hasConsented {
                ConsentView(onAccept: {
                    UserDefaults.standard.set(true, forKey: "hasAcceptedPrivacy")
                    UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: "privacyConsentTimestamp")
                    hasConsented = true
                }, onDecline: {
                    // Close the app on decline (iOS doesn't truly support exit)
                })
            } else if !authVM.isLoggedIn {
                LoginView()
            } else {
                TabView(selection: $selectedTab) {
                    NavigationStack {
                        HomeView()
                    }
                    .tabItem {
                        Label("Connect", systemImage: "power")
                    }
                    .tag(Tab.home)

                    NavigationStack {
                        ServerListView()
                    }
                    .tabItem {
                        Label("Servers", systemImage: "server.rack")
                    }
                    .tag(Tab.servers)

                    NavigationStack {
                        SettingsView()
                    }
                    .tabItem {
                        Label("Settings", systemImage: "gearshape")
                    }
                    .tag(Tab.settings)
                }
                .tint(.white)
            }
        }
    }
}
