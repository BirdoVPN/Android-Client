package app.birdo.vpn.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Birdo Brand Colors ──────────────────────────────────────────────────────
// Exactly matching the Windows desktop client's CSS variables / Tailwind config

// Core backgrounds — matches CSS --background: #050505
val BirdoBlack = Color(0xFF000000)       // App background (pure black)
val BirdoBackground = Color(0xFF050505)  // --background
val BirdoSurface = Color(0xFF0D0D0D)     // Slightly lighter surface
val BirdoSurfaceVariant = Color(0xFF1A1A1A) // rgba(26,26,26,0.95) from --card
val BirdoCard = Color(0xB314141A)        // Glass card: rgba(20,20,25,0.7)
val BirdoBorder = Color(0x14FFFFFF)      // rgba(255,255,255,0.08)

// Glass backgrounds
val GlassLight = Color(0x08FFFFFF)       // rgba(255,255,255,0.03)
val GlassStrong = Color(0x0FFFFFFF)      // rgba(255,255,255,0.06)
val GlassInput = Color(0x0AFFFFFF)       // rgba(255,255,255,0.04)

// White scale
val BirdoWhite = Color(0xFFF2F2F2)       // --foreground
val BirdoWhite80 = Color(0xCCFFFFFF)     // 80% white
val BirdoWhite60 = Color(0x99FFFFFF)     // 60% — labels
val BirdoWhite40 = Color(0x66FFFFFF)     // 40% — muted text
val BirdoWhite20 = Color(0x33FFFFFF)     // 20% — borders/toggles
val BirdoWhite10 = Color(0x1AFFFFFF)     // 10% — subtle bg
val BirdoWhite05 = Color(0x0DFFFFFF)     // 5% — very subtle

// Primary accent — purple (#A855F7) matching Windows client
val BirdoPurple = Color(0xFFA855F7)      // Primary accent
val BirdoPurpleDark = Color(0xFF7C3AED)  // Purple-600
val BirdoPurpleLight = Color(0xFFC084FC) // Purple-300
val BirdoPurpleBg = Color(0x1AA855F7)    // 10% opacity

// Status colors — matching Windows client Tailwind classes
val BirdoGreen = Color(0xFF22C55E)       // green-500 — Connected state
val BirdoGreenLight = Color(0xFF4ADE80)  // green-400 — text
val BirdoGreenBg = Color(0x1A22C55E)     // 10% opacity — status badge bg
val BirdoGreenShadow = Color(0x4D22C55E) // 30% opacity — glow

val BirdoYellow = Color(0xFFEAB308)      // yellow-500 — Connecting state
val BirdoYellowLight = Color(0xFFFACC15) // yellow-400 — text
val BirdoYellowBg = Color(0x1AEAB308)    // 10% opacity

val BirdoRed = Color(0xFFF87171)         // red-400 — Error state
val BirdoRedBg = Color(0x1AF87171)       // 10% opacity

val BirdoBlue = Color(0xFF3B82F6)        // blue-500 — Info / P2P
val BirdoBlueBg = Color(0x1A3B82F6)

// Emerald for update UI
val BirdoEmerald = Color(0xFF10B981)     // emerald-500
val BirdoEmeraldBg = Color(0x1A10B981)

// Primary button — solid white on dark (matching Windows .btn-primary)
val BirdoPrimary = Color.White
val BirdoOnPrimary = Color.Black

// Muted
val BirdoMuted = Color(0xFFA6A6A6)       // --muted-foreground
