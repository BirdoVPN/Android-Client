package app.birdo.vpn.service

import android.util.Log

/**
 * JNI bridge to `librosenpass_jni.so`.
 *
 * This is the **only** Kotlin call site that talks to the native Rosenpass
 * code. [RosenpassManager] uses it via reflection in M1 so the app keeps
 * loading even on devices/builds where the .so is absent (e.g. local debug
 * builds without Rust toolchain installed).
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
 * All native methods are CPU-heavy (Classic McEliece keypair generation is
 * ~hundreds of ms on a phone) and MUST be called off the main thread.
 * [RosenpassManager] dispatches them on `Dispatchers.IO`.
 */
object RosenpassNative {

    private const val TAG = "RosenpassNative"
    private const val LIB_NAME = "rosenpass_jni"

    /**
     * `true` when the native library was loaded successfully.
     *
     * On release builds with the Rust .so packaged this is always `true`.
     * On dev builds without `cargo ndk` having been run, this is `false`
     * and all `external` methods will throw [UnsatisfiedLinkError] —
     * [RosenpassManager] checks this flag before invoking any of them.
     */
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
            // Any other failure (SecurityException, etc.) — fail safe.
            isLoaded = false
            Log.e(TAG, "unexpected failure loading $LIB_NAME", t)
        }
    }

    /** Returns the version string of the loaded native library, e.g. `"rosenpass-jni 0.1.0 (aarch64, release)"`. */
    @JvmStatic
    external fun nativeVersion(): String

    /**
     * Generates a long-lived Classic McEliece 460896 keypair.
     *
     * @return a 2-element array: `[publicKey, secretKey]`. Public key ≈ 524 KB,
     *         secret key ≈ 13 KB. Caller MUST persist via [RosenpassManager.persistKeypair]
     *         and zeroize the secret key after writing to encrypted storage.
     * @throws RuntimeException if the native KEM call fails.
     */
    @JvmStatic
    external fun nativeGenerateKeypair(): Array<ByteArray>

    /**
     * Builds the Rosenpass `InitHello` UDP frame to send to the node.
     *
     * @return `null` if the protocol body is not yet implemented (M1 stub),
     *         otherwise the on-wire bytes to send to the peer's `rosenpass_endpoint`.
     */
    @JvmStatic
    external fun nativeInitiateHandshake(
        peerStaticPublicKey: ByteArray,
        clientSecretKey: ByteArray,
    ): ByteArray?

    /**
     * Processes the peer's `RespHello` reply and derives the 32-byte WireGuard PSK.
     *
     * @return the 32-byte PSK on success, `null` on stub-fallback (M1) or
     *         malformed/unauthenticated response.
     */
    @JvmStatic
    external fun nativeHandleResponse(
        responseMessage: ByteArray,
        clientSecretKey: ByteArray,
    ): ByteArray?

    /** Recommended re-key interval in seconds (per Rosenpass spec §5). */
    @JvmStatic
    external fun nativeRekeyIntervalSeconds(): Int

    /** Convenience accessor that returns the version, or a placeholder when the lib isn't loaded. */
    fun getNativeVersion(): String =
        if (isLoaded) runCatching { nativeVersion() }.getOrDefault("<error>") else "<not loaded>"
}
