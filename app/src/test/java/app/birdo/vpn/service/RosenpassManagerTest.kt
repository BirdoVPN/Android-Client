package app.birdo.vpn.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Unit tests for [RosenpassManager] — validates crypto helpers
 * (HMAC-SHA256, HKDF-Expand) and state management.
 *
 * Full performKeyExchange() requires Android context and native libraries.
 * These tests focus on the pure crypto primitives via reflection and
 * companion-level state.
 */
class RosenpassManagerTest {

    @Before
    fun setup() {
        RosenpassManager.stop()
    }

    @Test
    fun `initial state - not quantum protected, null PSK`() {
        assertFalse(RosenpassManager.isQuantumProtected())
        assertNull(RosenpassManager.getCurrentPsk())
    }

    @Test
    fun `stop clears quantum state`() {
        RosenpassManager.stop()
        assertFalse(RosenpassManager.isQuantumProtected())
        assertNull(RosenpassManager.getCurrentPsk())
    }

    @Test
    fun `stop when already stopped is no-op`() {
        RosenpassManager.stop()
        RosenpassManager.stop()
        assertFalse(RosenpassManager.isQuantumProtected())
    }

    @Test
    fun `hmacSha256 produces correct output`() {
        // Use reflection to access private hmacSha256
        val method = RosenpassManager::class.java.getDeclaredMethod(
            "hmacSha256", ByteArray::class.java, ByteArray::class.java
        )
        method.isAccessible = true

        val key = "test-key".toByteArray()
        val data = "test-data".toByteArray()
        val result = method.invoke(RosenpassManager, key, data) as ByteArray

        // Verify against standard Java HMAC-SHA256
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val expected = mac.doFinal(data)

        assertArrayEquals(expected, result)
    }

    @Test
    fun `hmacSha256 produces 32-byte output`() {
        val method = RosenpassManager::class.java.getDeclaredMethod(
            "hmacSha256", ByteArray::class.java, ByteArray::class.java
        )
        method.isAccessible = true

        val result = method.invoke(
            RosenpassManager,
            ByteArray(32) { it.toByte() },
            "hello".toByteArray()
        ) as ByteArray

        assertEquals(32, result.size)
    }

    @Test
    fun `hkdfExpand produces correct length output`() {
        val method = RosenpassManager::class.java.getDeclaredMethod(
            "hkdfExpand", ByteArray::class.java, ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val prk = ByteArray(32) { (it + 1).toByte() }
        val info = "wg psk".toByteArray()
        val result = method.invoke(RosenpassManager, prk, info, 32) as ByteArray

        assertEquals(32, result.size)
    }

    @Test
    fun `hkdfExpand with empty info`() {
        val method = RosenpassManager::class.java.getDeclaredMethod(
            "hkdfExpand", ByteArray::class.java, ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val prk = ByteArray(32) { 0xFF.toByte() }
        val result = method.invoke(RosenpassManager, prk, ByteArray(0), 32) as ByteArray

        assertEquals(32, result.size)
        // Ensure it's not all zeros (actual crypto output)
        assertFalse(result.all { it == 0.toByte() })
    }
}
