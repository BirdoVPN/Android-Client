package app.birdo.vpn.service

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pure-JVM unit tests for [RosenpassManager].
 *
 * The native lib isn't loaded in robolectric/host JVM, so all tests
 * exercise the fall-through paths (DISABLED / SERVER_PROVIDED modes
 * + crypto helpers). Bilateral mode requires a real device + server
 * and is covered by the M5 integration test plan in `native/ROADMAP.md`.
 */
class RosenpassManagerTest {

    @Before
    fun setup() {
        RosenpassManager.stop()
    }

    @After
    fun tearDown() {
        RosenpassManager.stop()
    }

    // ── State machine ──────────────────────────────────────────────────────

    @Test
    fun `initial state is DISABLED with null PSK`() {
        assertFalse(RosenpassManager.isQuantumProtected())
        assertFalse(RosenpassManager.isBilateral())
        assertNull(RosenpassManager.getCurrentPsk())
        assertEquals(RosenpassManager.Mode.DISABLED, RosenpassManager.modeFlow.value)
    }

    @Test
    fun `stop clears PSK and resets mode`() {
        RosenpassManager.stop()
        assertEquals(RosenpassManager.Mode.DISABLED, RosenpassManager.modeFlow.value)
        assertNull(RosenpassManager.getCurrentPsk())
    }

    @Test
    fun `stop is idempotent`() {
        repeat(3) { RosenpassManager.stop() }
        assertEquals(RosenpassManager.Mode.DISABLED, RosenpassManager.modeFlow.value)
    }

    @Test
    fun `nativeLibVersion returns placeholder when lib not loaded`() {
        // In the host JVM there's no librosenpass_jni.so — getter must not throw.
        val v = RosenpassManager.nativeLibVersion()
        assertNotNull(v)
        assertTrue("got: $v", v == "<not loaded>" || v.startsWith("rosenpass-jni"))
    }

    // ── Mode enum ──────────────────────────────────────────────────────────

    @Test
    fun `Mode enum has exactly three values`() {
        assertEquals(3, RosenpassManager.Mode.values().size)
        assertNotNull(RosenpassManager.Mode.valueOf("DISABLED"))
        assertNotNull(RosenpassManager.Mode.valueOf("SERVER_PROVIDED"))
        assertNotNull(RosenpassManager.Mode.valueOf("BILATERAL"))
    }

    // ── Endpoint parsing ──────────────────────────────────────────────────

    @Test
    fun `parseEndpoint accepts ipv4 host port`() {
        val pair = invokeParseEndpoint("198.51.100.7:9999")
        assertEquals("198.51.100.7" to 9999, pair)
    }

    @Test
    fun `parseEndpoint accepts hostname`() {
        val pair = invokeParseEndpoint("rp.example.com:443")
        assertEquals("rp.example.com" to 443, pair)
    }

    @Test
    fun `parseEndpoint strips ipv6 brackets`() {
        val pair = invokeParseEndpoint("[2001:db8::1]:9999")
        assertEquals("2001:db8::1" to 9999, pair)
    }

    @Test
    fun `parseEndpoint rejects missing port`() {
        assertNull(invokeParseEndpoint("host.example.com"))
    }

    @Test
    fun `parseEndpoint rejects empty port`() {
        assertNull(invokeParseEndpoint("host:"))
    }

    @Test
    fun `parseEndpoint rejects out-of-range port`() {
        assertNull(invokeParseEndpoint("host:0"))
        assertNull(invokeParseEndpoint("host:70000"))
        assertNull(invokeParseEndpoint("host:-1"))
    }

    @Test
    fun `parseEndpoint rejects non-numeric port`() {
        assertNull(invokeParseEndpoint("host:abc"))
    }

    @Test
    fun `parseEndpoint rejects empty host`() {
        assertNull(invokeParseEndpoint(":9999"))
    }

    // ── HKDF / HMAC helpers ────────────────────────────────────────────────

    @Test
    fun `hmacSha256 matches Java reference impl`() {
        val key = "test-key".toByteArray()
        val data = "test-data".toByteArray()
        val ours = RosenpassManager.hmacSha256(key, data)

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val expected = mac.doFinal(data)

        assertEquals(32, ours.size)
        assertEquals(expected.toList(), ours.toList())
    }

    @Test
    fun `hkdfExpand produces requested length`() {
        val prk = ByteArray(32) { (it + 1).toByte() }
        val out = RosenpassManager.hkdfExpand(prk, "wg psk".toByteArray(), 32)
        assertEquals(32, out.size)
        assertFalse(out.all { it == 0.toByte() })
    }

    @Test
    fun `hkdfExpand is deterministic`() {
        val prk = ByteArray(32) { 0x42.toByte() }
        val a = RosenpassManager.hkdfExpand(prk, "ctx".toByteArray(), 32)
        val b = RosenpassManager.hkdfExpand(prk, "ctx".toByteArray(), 32)
        assertEquals(a.toList(), b.toList())
    }

    @Test
    fun `hkdfExpand different info produces different output`() {
        val prk = ByteArray(32) { 0x42.toByte() }
        val a = RosenpassManager.hkdfExpand(prk, "info-A".toByteArray(), 32)
        val b = RosenpassManager.hkdfExpand(prk, "info-B".toByteArray(), 32)
        assertFalse(a.contentEquals(b))
    }

    // ── helpers ────────────────────────────────────────────────────────────

    /** Reaches into the private parseEndpoint via reflection for direct unit testing. */
    @Suppress("UNCHECKED_CAST")
    private fun invokeParseEndpoint(input: String): Pair<String, Int>? {
        val m = RosenpassManager::class.java.getDeclaredMethod("parseEndpoint", String::class.java)
        m.isAccessible = true
        return m.invoke(RosenpassManager, input) as Pair<String, Int>?
    }
}
