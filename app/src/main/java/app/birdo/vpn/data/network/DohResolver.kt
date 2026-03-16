package app.birdo.vpn.data.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * DNS-over-HTTPS resolver with Cloudflare primary, Google fallback, and Quad9 final fallback.
 * Matches the Windows client's 3-provider DoH chain.
 * Prevents DNS leaks and ISP snooping on domain lookups.
 */
object DohResolver {

    private val bootstrapClient = OkHttpClient.Builder().build()

    private val cloudflare: DnsOverHttps = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("1.1.1.1"),
            InetAddress.getByName("1.0.0.1"),
            InetAddress.getByName("2606:4700:4700::1111"),
        )
        .build()

    private val google: DnsOverHttps = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.google/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("8.8.4.4"),
            InetAddress.getByName("2001:4860:4860::8888"),
        )
        .build()

    private val quad9: DnsOverHttps = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.quad9.net/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("9.9.9.9"),
            InetAddress.getByName("149.112.112.112"),
            InetAddress.getByName("2620:fe::fe"),
        )
        .build()

    /**
     * Resolves using Cloudflare DoH first, then Google, then Quad9 as final fallback.
     */
    fun resolve(hostname: String): List<InetAddress> {
        return try {
            cloudflare.lookup(hostname)
        } catch (_: UnknownHostException) {
            try {
                google.lookup(hostname)
            } catch (_: UnknownHostException) {
                quad9.lookup(hostname)
            }
        }
    }

    /**
     * Returns a Dns implementation for use with OkHttpClient.Builder().dns().
     */
    val dns: okhttp3.Dns = object : okhttp3.Dns {
        override fun lookup(hostname: String): List<InetAddress> = resolve(hostname)
    }
}
