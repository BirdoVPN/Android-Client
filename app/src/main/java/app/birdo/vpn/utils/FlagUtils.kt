package app.birdo.vpn.utils

/**
 * Convert a 2-letter ISO country code to a flag emoji.
 * Uses Unicode Regional Indicator symbols.
 */
fun countryCodeToFlag(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val first = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
