package app.birdo.vpn.shared

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.inet_pton

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun platformName(): String = "iOS"

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun isValidDnsAddress(address: String): Boolean {
    if (address.isBlank()) return false
    // Guard: only allow numeric IP literals
    if (!address.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' || it == '.' || it == '%' }) {
        return false
    }

    // Try parsing as IPv4
    kotlinx.cinterop.memScoped {
        val buf4 = allocArray<kotlinx.cinterop.ByteVar>(16)
        if (inet_pton(AF_INET, address, buf4.reinterpret<kotlinx.cinterop.ByteVar>()) == 1) {
            return !isLoopbackOrSpecialV4(address)
        }
    }

    // Try parsing as IPv6
    kotlinx.cinterop.memScoped {
        val buf6 = allocArray<kotlinx.cinterop.ByteVar>(28)
        if (inet_pton(AF_INET6, address, buf6.reinterpret<kotlinx.cinterop.ByteVar>()) == 1) {
            return !isLoopbackOrSpecialV6(address)
        }
    }

    return false
}

private fun isLoopbackOrSpecialV4(address: String): Boolean {
    val parts = address.split(".")
    if (parts.size != 4) return false
    val first = parts[0].toIntOrNull() ?: return false
    // 127.x.x.x = loopback, 0.x.x.x = wildcard, 169.254.x.x = link-local, 224+ = multicast
    return first == 127 || first == 0 ||
        (first == 169 && (parts[1].toIntOrNull() ?: 0) == 254) ||
        first >= 224
}

private fun isLoopbackOrSpecialV6(address: String): Boolean {
    val lower = address.lowercase()
    return lower == "::1" || lower == "::" || lower.startsWith("fe80") || lower.startsWith("ff")
}
