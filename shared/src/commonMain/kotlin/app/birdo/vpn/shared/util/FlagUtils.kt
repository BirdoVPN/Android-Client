package app.birdo.vpn.shared.util

/**
 * Convert a 2-letter ISO 3166-1 alpha-2 country code to a flag emoji.
 * Uses Unicode Regional Indicator Symbol pairs (U+1F1E6 .. U+1F1FF).
 */
fun countryCodeToFlag(countryCode: String): String {
    if (countryCode.length != 2) return "\uD83C\uDF10" // 🌐
    val upper = countryCode.uppercase()
    val first = 0x1F1E6 + (upper[0].code - 'A'.code)
    val second = 0x1F1E6 + (upper[1].code - 'A'.code)
    return String(intArrayOf(first, second), 0, 2)
}
