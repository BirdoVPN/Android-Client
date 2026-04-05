package app.birdo.vpn.utils

/**
 * Re-export from shared KMP module.
 * Preserves the existing import path for the Android codebase.
 */
fun countryCodeToFlag(countryCode: String): String =
    app.birdo.vpn.shared.util.countryCodeToFlag(countryCode)
