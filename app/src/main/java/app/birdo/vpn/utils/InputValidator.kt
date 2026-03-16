package app.birdo.vpn.utils

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Centralized input validation for all user-supplied data.
 *
 * Every field that enters from the UI, deep link, or API response is validated
 * here before being persisted or sent to a service. This prevents injection,
 * truncation, and malformed-data bugs.
 */
object InputValidator {

    // ── Email ────────────────────────────────────────────────────

    /**
     * Simplified RFC 5322 email pattern.
     * Rejects obviously invalid emails while accepting all real-world formats.
     */
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,64}$"
    )

    /** Max length per RFC 5321 */
    private const val EMAIL_MAX_LENGTH = 254

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.length in 1..EMAIL_MAX_LENGTH && EMAIL_REGEX.matches(trimmed)
    }

    // ── Password ─────────────────────────────────────────────────

    private const val PASSWORD_MIN_LENGTH = 6
    private const val PASSWORD_MAX_LENGTH = 256

    fun isValidPassword(password: String): Boolean {
        return password.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH
    }

    // ── Network: IP / DNS ────────────────────────────────────────

    /**
     * Validate a DNS server address (IPv4 or IPv6).
     * Must be a well-formed IP address — hostnames are NOT accepted for DNS fields.
     *
     * FIX-1-9: Rejects loopback (127.x), link-local (169.254.x, fe80::), multicast,
     * and wildcard (0.0.0.0) addresses. Uses looksLikeIpLiteral() guard to prevent
     * InetAddress.getByName() from resolving hostnames like "evil.com".
     */
    fun isValidDnsAddress(address: String): Boolean {
        val trimmed = address.trim()
        if (trimmed.isBlank()) return false
        // Guard: only allow numeric IP literals — never resolve hostnames
        if (!looksLikeIpLiteral(trimmed)) return false
        return try {
            val addr = InetAddress.getByName(trimmed)
            (addr is Inet4Address || addr is Inet6Address) &&
                !addr.isLoopbackAddress &&
                !addr.isLinkLocalAddress &&
                !addr.isMulticastAddress &&
                !addr.isAnyLocalAddress
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if a string looks like an IP address literal (not a hostname).
     * IP literals contain only digits, hex chars (a-f), colons, dots, and percent (scope ID).
     * Any other letter means it's a hostname and must be rejected.
     */
    private fun looksLikeIpLiteral(address: String): Boolean {
        return address.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' || it == '.' || it == '%' }
    }

    /**
     * Strict IPv4 check (no DNS resolution).
     */
    private val IPV4_REGEX = Regex(
        "^(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\." +
        "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\." +
        "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\." +
        "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    )

    fun isValidIpv4(address: String): Boolean = IPV4_REGEX.matches(address.trim())

    // ── Network: Port ────────────────────────────────────────────

    private const val PORT_MIN = 1
    private const val PORT_MAX = 65535

    fun isValidPort(port: Int): Boolean = port in PORT_MIN..PORT_MAX

    fun isValidPort(port: String): Boolean {
        if (port == "auto") return true
        val num = port.toIntOrNull() ?: return false
        return isValidPort(num)
    }

    // ── Network: MTU ─────────────────────────────────────────────

    private const val MTU_MIN = 1280
    private const val MTU_MAX = 1500

    /** 0 = auto (server default), otherwise [1280, 1500] */
    fun clampMtu(mtu: Int): Int = if (mtu == 0) 0 else mtu.coerceIn(MTU_MIN, MTU_MAX)

    fun isValidMtu(mtu: Int): Boolean = mtu == 0 || mtu in MTU_MIN..MTU_MAX

    // ── Sanitization ─────────────────────────────────────────────

    /**
     * Sanitize a server error body before showing it in the UI.
     * Strips HTML, stack traces, and excessive length.
     */
    fun sanitizeErrorMessage(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        val trimmed = raw.trim()
        if (trimmed.length > 200 ||
            trimmed.contains("<html", ignoreCase = true) ||
            trimmed.contains("Exception") ||
            trimmed.contains("at ") ||
            trimmed.contains("stackTrace", ignoreCase = true)
        ) {
            return fallback
        }
        return trimmed
    }

    /**
     * Sanitize a server ID from API responses.
     * Server IDs should be alphanumeric + hyphens only, max 64 chars.
     */
    private val SERVER_ID_REGEX = Regex("^[A-Za-z0-9_\\-]{1,64}$")

    fun isValidServerId(id: String): Boolean = SERVER_ID_REGEX.matches(id)
}
