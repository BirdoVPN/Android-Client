package app.birdo.vpn.data.network

import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

/**
 * Unit tests for [DohResolver].
 *
 * Since DohResolver is a singleton with hardcoded DoH endpoints (Cloudflare, Quad9),
 * full resolution tests require network access. These tests validate the object's
 * public API contract and the Dns adapter interface.
 */
class DohResolverTest {

    @Test
    fun `dns property is not null`() {
        assertNotNull(DohResolver.dns)
    }

    @Test
    fun `dns property implements okhttp3 Dns`() {
        val dns = DohResolver.dns
        assertTrue(dns is okhttp3.Dns)
    }

    @Test
    fun `resolve throws on empty hostname`() {
        try {
            DohResolver.resolve("")
            fail("Expected exception for empty hostname")
        } catch (_: Exception) {
            // Expected — invalid hostname should throw
        }
    }

    @Test
    fun `dns lookup delegates to resolve`() {
        // Verify the dns adapter and resolve() behave the same for invalid input
        var resolveException: Exception? = null
        var dnsException: Exception? = null

        try {
            DohResolver.resolve("invalid..hostname..test")
        } catch (e: Exception) {
            resolveException = e
        }

        try {
            DohResolver.dns.lookup("invalid..hostname..test")
        } catch (e: Exception) {
            dnsException = e
        }

        // Both should either succeed or fail — they share the same code path
        assertEquals(resolveException == null, dnsException == null)
        if (resolveException != null && dnsException != null) {
            assertEquals(resolveException::class, dnsException::class)
        }
    }

    @Test
    fun `cloudflare and quad9 clients are initialized`() {
        // Use reflection to verify both DoH clients were constructed
        val cfField = DohResolver::class.java.getDeclaredField("cloudflare")
        cfField.isAccessible = true
        assertNotNull(cfField.get(DohResolver))

        val q9Field = DohResolver::class.java.getDeclaredField("quad9")
        q9Field.isAccessible = true
        assertNotNull(q9Field.get(DohResolver))
    }

    @Test
    fun `bootstrap client is initialized`() {
        val field = DohResolver::class.java.getDeclaredField("bootstrapClient")
        field.isAccessible = true
        val client = field.get(DohResolver)
        assertNotNull(client)
        assertTrue(client is okhttp3.OkHttpClient)
    }
}
