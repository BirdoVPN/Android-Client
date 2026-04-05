import SwiftUI

/// Birdo VPN colour palette — matches Android BirdoTheme exactly.
enum BirdoTheme {
    static let black       = Color(hex: 0x0A0A0A)
    static let background  = Color(hex: 0x111111)
    static let surface     = Color(hex: 0x1A1A1A)
    static let card        = Color(hex: 0x161616)
    static let border      = Color.white.opacity(0.06)

    static let purple      = Color(hex: 0x8B5CF6)
    static let purpleLight = Color(hex: 0xA78BFA)

    static let green       = Color(hex: 0x22C55E)
    static let greenLight  = Color(hex: 0x4ADE80)
    static let greenBg     = Color(hex: 0x22C55E).opacity(0.1)
    static let greenShadow = Color(hex: 0x22C55E).opacity(0.5)

    static let red         = Color(hex: 0xEF4444)
    static let redBg       = Color(hex: 0xEF4444).opacity(0.1)

    static let yellow      = Color(hex: 0xEAB308)
    static let yellowLight = Color(hex: 0xFACC15)
    static let yellowBg    = Color(hex: 0xEAB308).opacity(0.1)

    static let blue        = Color(hex: 0x3B82F6)

    static let white       = Color.white
    static let white80     = Color.white.opacity(0.8)
    static let white60     = Color.white.opacity(0.6)
    static let white40     = Color.white.opacity(0.4)
    static let white20     = Color.white.opacity(0.2)
    static let white10     = Color.white.opacity(0.1)
    static let white05     = Color.white.opacity(0.05)

    static let glassStrong = Color(hex: 0x111111).opacity(0.85)
    static let glassLight  = Color.white.opacity(0.05)
    static let glassInput  = Color.white.opacity(0.04)
}

extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red:   Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8)  & 0xFF) / 255,
            blue:  Double(hex         & 0xFF) / 255,
            opacity: alpha
        )
    }
}
