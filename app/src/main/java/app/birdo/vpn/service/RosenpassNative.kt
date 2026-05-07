package app.birdo.vpn.service

import android.util.Log

/**
 * JNI bridge to `librosenpass_jni.so` — BirdoPQ v1 (ML-KEM-1024).
 *
 * This is the **only** Kotlin call site that talks to the native PQ KEM
 * code. [RosenpassManager] uses it via reflection so the app keeps loading
 * even on devices/builds where the .so is absent (e.g. local debug builds
 * without the Rust toolchain installed).
 *
 * ## ABI contract
 *
 * The native signatures live in `native/rosenpass-jni/src/lib.rs`. They are
 * stable across patch versions and verified at runtime by [getNativeVersion].
 * If you change a signature here, you MUST update the matching Rust function
 * AND bump the crate version in `native/rosenpass-jni/Cargo.toml`.
 *
 * ## Threading
 *
 * `nativeGenerateKeypair` is the only CPU-heavy call (~10–50 ms on a phone
 * for ML-KEM-1024). All native methods MUST be called off the main thread —
 * [RosenpassManager] dispatches them on `Dispatchers.IO`.
 *
 * ## Why not upstream Rosenpass?
 *
 * See `native/rosenpass-jni/src/lib.rs` module docs. tl;dr libsodium can't
 * be cross-compiled to Android without weeks of build infra work, and we
 * get the same HNDL guarantee from a much simpler ML-KEM-only construction.
 */
object RosenpassNative {

    private const val TAG = "RosenpassNative"
    private const val LIB_NAME = "rosenpass_jni"

    /** ML-KEM-1024 public key length (FIPS 203). */
    const val PUBLIC_KEY_BYTES = 1568

    /** ML-KEM-1024 secret key length (FIPS 203). */
    const val SECRET_KEY_BYTES = 3168

    /** ML-KEM-1024 ciphertext length (FIPS 203). */
    const val CIPHERTEXT_BYTES = 1568

    /** WireGuard PresharedKey length. */
    const val PSK_BYTES = 32

    @Volatile
    var isLoaded: Boolean = false
        private set

    init {
        try {
            System.loadLibrary(LIB_NAME)
            isLoaded = true
            Log.i(TAG, "loaded $LIB_NAME successfully — version=${nativeVersion()}")
        } catch (e: UnsatisfiedLinkError) {
            isLoaded = false
            Log.w(TAG, "native lib $LIB_NAME not present — falling back to server-provided PSK path", e)
        } catch (t: Throwable) {
            isLoaded = false
            Log.e(TAG, "unexpected failure loading $LIB_NAME", t)
        }
    }

    /** Returns the native lib version string, e.g. `"rosenpass-jni 0.2.0 (BirdoPQ v1, ML-KEM-1024, aarch64, release)"`. */
    @JvmStatic
    external fun nativeVersion(): String

    /**
     * Generates a long-lived ML-KEM-1024 keypair.
     *
     * @return 2-element array `[publicKey (~1568 B), secretKey (~3168 B)]`.
     *         Caller MUST persist via [RosenpassKeyStore] (Keystore-wrapped
     *         EncryptedFile) and zeroize the in-memory secret key copy after
     *         writing it to encrypted storage.
     * @throws RuntimeException if the underlying KEM call fails.
     */
    @JvmStatic
    external fun nativeGenerateKeypair(): Array<ByteArray>

    /**
     * Decapsulates the server-supplied ML-KEM ciphertext and derives the
     * 32-byte WireGuard PSK via HKDF-SHA-256 mixed with the per-connect nonce.
     *
     * @param clientSecretKey the long-lived ML-KEM-1024 sk (3168 B).
     * @param serverCiphertext the bytes from `ConnectResponse.rosenpassPublicKey`
     *                         (semantically repurposed — see Rust module doc).
     * @param serverNonce      the bytes from `ConnectResponse.rosenpassEndpoint`
     *                         (also repurposed). Mixed into HKDF so each
     *                         connect derives a fresh PSK.
     * @return 32-byte PSK on success, `null` on any malformed input.
     */
    @JvmStatic
    external fun nativeDeriveSharedPsk(
        clientSecretKey: ByteArray,
        serverCiphertext: ByteArray,
        serverNonce: ByteArray,
    ): ByteArray?

    /**
     * Server-side encapsulation, exposed via JNI ONLY for unit-test use that
     * exercises the full client↔server roundtrip in-process.
     *
     * Production server-side encapsulation runs in `native/birdo-pq-server/`
     * (a separate binary) and never touches this surface.
     *
     * @return 2-element array `[ciphertext (~1568 B), psk (32 B)]`.
     */
    @JvmStatic
    external fun nativeEncapsulateForServer(
        clientPublicKey: ByteArray,
        serverNonce: ByteArray,
    ): Array<ByteArray>

    /** Returns the canonical PSK length the native lib will produce. */
    @JvmStatic
    external fun nativePskLength(): Int

    fun getNativeVersion(): String =
        if (isLoaded) runCatching { nativeVersion() }.getOrDefault("<error>") else "<not loaded>"

    // ── Idiomatic Kotlin wrappers ──────────────────────────────────────────

    /**
     * Long-lived ML-KEM-1024 keypair. The secret key half is sensitive
     * material — callers MUST persist via [RosenpassKeyStore] (Keystore-
     * wrapped EncryptedFile) and zeroize the in-memory copy as soon as it's
     * been handed to the native derive call.
     */
    data class StaticKeypair(val publicKey: ByteArray, val secretKey: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StaticKeypair) return false
            return publicKey.contentEquals(other.publicKey) &&
                    secretKey.contentEquals(other.secretKey)
        }
        override fun hashCode(): Int =
            31 * publicKey.contentHashCode() + secretKey.contentHashCode()
    }

    /**
     * Typed wrapper around [nativeGenerateKeypair].
     *
     * @throws IllegalStateException if the native lib is not loaded.
     * @throws RuntimeException if the underlying KEM call fails.
     */
    fun generateKeypair(): StaticKeypair {
        check(isLoaded) { "rosenpass-jni not loaded" }
        val raw = nativeGenerateKeypair()
        require(raw.size == 2) { "nativeGenerateKeypair returned ${raw.size} elements, expected 2" }
        require(raw[0].size == PUBLIC_KEY_BYTES) {
            "ML-KEM-1024 pk must be $PUBLIC_KEY_BYTES B, got ${raw[0].size}"
        }
        require(raw[1].size == SECRET_KEY_BYTES) {
            "ML-KEM-1024 sk must be $SECRET_KEY_BYTES B, got ${raw[1].size}"
        }
        return StaticKeypair(publicKey = raw[0], secretKey = raw[1])
    }

    /**
     * Decapsulate + derive PSK. Returns null if the lib isn't loaded or the
     * underlying call rejected the input.
     */
    fun deriveSharedPsk(
        clientSecretKey: ByteArray,
        serverCiphertext: ByteArray,
        serverNonce: ByteArray,
    ): ByteArray? {
        if (!isLoaded) return null
        return runCatching {
            nativeDeriveSharedPsk(clientSecretKey, serverCiphertext, serverNonce)
        }.getOrNull()
    }
}
