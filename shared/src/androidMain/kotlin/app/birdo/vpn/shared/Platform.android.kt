package app.birdo.vpn.shared

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun platformName(): String = "Android"

actual fun isValidDnsAddress(address: String): Boolean {
    if (address.isBlank()) return false
    // Guard: only allow numeric IP literals — never resolve hostnames
    if (!address.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' || it == '.' || it == '%' }) {
        return false
    }
    return try {
        val addr = InetAddress.getByName(address)
        (addr is Inet4Address || addr is Inet6Address) &&
            !addr.isLoopbackAddress &&
            !addr.isLinkLocalAddress &&
            !addr.isMulticastAddress &&
            !addr.isAnyLocalAddress
    } catch (_: Exception) {
        false
    }
}
