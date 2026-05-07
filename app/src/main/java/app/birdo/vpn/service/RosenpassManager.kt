package app.birdo.vpn.service

import android.content.Context
import android.util.Base64
import android.util.Log
import app.birdo.vpn.data.model.ConnectResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Coordinates Rosenpass post-quantum WireGuard PSK exchange.
 *
 * ## Operating modes
 *
 * The manager picks the strongest mode that's feasible for the current
 * connect attempt and reports it via [modeFlow]:
 *
 * | Mode | When | Provides HNDL resistance? |
 * |------|------|---------------------------|
 * | [Mode.BILATERAL] | Native lib loaded AND the live UDP exchange against `rosenpass_endpoint` succeeds | ✅ yes (post-quantum) |
 * | [Mode.SERVER_PROVIDED] | Server returned a WireGuard-format PSK in [ConnectResponse.presharedKey] | ⚠️ partial — relies on TLS for PSK delivery |
 * | [Mode.DISABLED] | Neither bilateral exchange nor server PSK available | ❌ no |
 *
 * **Honest disclaimer for marketing copy:** Only [Mode.BILATERAL] provides
 * genuine post-quantum protection against Harvest-Now-Decrypt-Later attackers.
 * [Mode.SERVER_PROVIDED] is the v1.3.x production behaviour and is fine for
 * day-to-day traffic but does NOT defeat HNDL. See `native/ROADMAP.md`.
 *
 * ## Lifecycle
 *
 * ```
 * connect()  ──▶ performKeyExchange(config)         // returns initial PSK
 *            ──▶ startRekeyLoop(config, onPskReady) // schedules every ~120 s
 * disconnect() ──▶ stop()                            // cancels loop + zeroes secrets
 * ```
 *
 * The rekey loop calls back into [BirdoVpnService] via [onPskReady] which
 * applies the new PSK atomically with `wg set <iface> peer <pk> preshared-key`.
 */
object RosenpassManager {

    private const val TAG = "RosenpassManager"

    /** WireGuard PresharedKey is exactly 32 bytes. */
    private const val PSK_LENGTH_BYTES = 32

    /** UDP read timeout for a single handshake message (ms). */
    private const val UDP_RECV_TIMEOUT_MS = 5_000

    /** Maximum size of a single Rosenpass UDP frame on the wire. */
    private const val UDP_MTU = 65_507

    /**
     * Magic bytes prefixed to **all** wire frames. Bumped when we change
     * frame layout in a way that's not backwards-compatible. Today (M1)
     * the bilateral exchange is gated on the native lib returning non-null,
     * which it doesn't yet, so this magic isn't actually used on the wire —
     * but the constant is wired through so M2 doesn't need to retro-fit it.
     */
    private const val WIRE_MAGIC = "BRP1"

    enum class Mode { DISABLED, SERVER_PROVIDED, BILATERAL }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var rekeyJob: Job? = null

    @Volatile
    private var currentPsk: ByteArray? = null

    @Volatile
    private var keyStore: RosenpassKeyStore? = null

    private val _modeFlow = MutableStateFlow(Mode.DISABLED)
    val modeFlow: StateFlow<Mode> = _modeFlow.asStateFlow()

    /** True when we're currently providing some form of PQ-flavoured protection. */
    fun isQuantumProtected(): Boolean = currentPsk != null && _modeFlow.value != Mode.DISABLED

    /** True iff we're running the genuine bilateral PQ exchange. */
    fun isBilateral(): Boolean = _modeFlow.value == Mode.BILATERAL

    fun getCurrentPsk(): String? {
        val psk = currentPsk ?: return null
        return Base64.encodeToString(psk, Base64.NO_WRAP)
    }

    /** Diagnostic: returns the loaded native lib version, or `<not loaded>`. */
    fun nativeLibVersion(): String = RosenpassNative.getNativeVersion()

    // ── public API ─────────────────────────────────────────────────────────

    /**
     * Performs the initial key exchange and returns the PSK as Base64, or
     * `null` if no PQ-flavoured PSK is available at all.
     *
     * Implementation order:
     *   1. Try bilateral native exchange (loads/generates persistent keypair,
     *      sends UDP, awaits response).
     *   2. Fall back to the server-supplied PSK if present.
     *   3. Otherwise return `null` and the caller MUST use plain WireGuard.
     */
    suspend fun performKeyExchange(context: Context, config: ConnectResponse): String? = withContext(Dispatchers.IO) {
        // Cancel any rekey loop from a previous connect.
        rekeyJob?.cancel()
        rekeyJob = null

        if (config.rosenpassPublicKey == null) {
            Log.w(TAG, "no rosenpassPublicKey in connect response — PQ unavailable")
            return@withContext fallbackToServerPsk(config)
        }

        val bilateralPsk = tryBilateralExchange(context, config)
        if (bilateralPsk != null) {
            currentPsk = bilateralPsk
            _modeFlow.value = Mode.BILATERAL
            Log.i(TAG, "bilateral PQ key exchange succeeded — quantum-resistant PSK active")
            return@withContext Base64.encodeToString(bilateralPsk, Base64.NO_WRAP)
        }

        // Native unavailable or stub returned null — use server PSK.
        return@withContext fallbackToServerPsk(config)
    }

    private fun fallbackToServerPsk(config: ConnectResponse): String? {
        val serverPsk = config.presharedKey
        if (serverPsk == null) {
            currentPsk = null
            _modeFlow.value = Mode.DISABLED
            Log.w(TAG, "no server-provided PSK either — quantum protection unavailable")
            return null
        }
        currentPsk = Base64.decode(serverPsk, Base64.NO_WRAP)
        _modeFlow.value = Mode.SERVER_PROVIDED
        Log.i(TAG, "using server-provided PSK (TLS-delivered, NOT HNDL-safe)")
        return serverPsk
    }

    /**
     * Starts the periodic rekey loop. Should be called once per connect after
     * the initial [performKeyExchange] succeeds.
     *
     * @param onPskReady invoked on the IO dispatcher each time a fresh PSK is
     *                   derived. Must apply the new PSK atomically to the live
     *                   wg interface — typically via `WgNative.setPresharedKey`.
     */
    fun startRekeyLoop(
        context: Context,
        config: ConnectResponse,
        onPskReady: suspend (ByteArray) -> Unit,
    ) {
        rekeyJob?.cancel()
        if (_modeFlow.value != Mode.BILATERAL) {
            // Server-provided PSK doesn't rotate — only bilateral mode rekeys.
            Log.d(TAG, "rekey loop not started (mode=${_modeFlow.value})")
            return
        }
        val intervalSec = RosenpassNative.rekeyIntervalSeconds().coerceAtLeast(30)
        Log.i(TAG, "starting rekey loop, interval=${intervalSec}s")
        rekeyJob = scope.launch {
            while (isActive) {
                delay(intervalSec * 1_000L)
                try {
                    val freshPsk = tryBilateralExchange(context, config)
                    if (freshPsk != null) {
                        // Zero the prior PSK before swapping.
                        currentPsk?.fill(0)
                        currentPsk = freshPsk
                        onPskReady(freshPsk)
                        Log.i(TAG, "rekey OK — fresh PQ-PSK applied")
                    } else {
                        Log.w(TAG, "rekey returned no PSK — keeping previous one until next interval")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "rekey iteration failed; will retry next interval", e)
                }
            }
        }
    }

    /**
     * Stops PQ protection, cancels the rekey loop, and zeroes all sensitive
     * key material in process memory.
     *
     * Persistent keys on disk are NOT touched — call [resetPersistedKeypair]
     * for that (e.g. on user-initiated logout/reset).
     */
    fun stop() {
        Log.i(TAG, "stopping Rosenpass PQ protection")
        rekeyJob?.cancel()
        rekeyJob = null
        currentPsk?.fill(0)
        currentPsk = null
        _modeFlow.value = Mode.DISABLED
    }

    /** Permanently deletes the persisted Rosenpass keypair. Use on logout. */
    fun resetPersistedKeypair(context: Context) {
        ensureKeyStore(context).clear()
    }

    // ── bilateral PQ exchange ──────────────────────────────────────────────

    /**
     * One full Rosenpass round-trip.
     *
     * 1. Load (or lazily generate + persist) our static Classic McEliece keypair.
     * 2. Ask native for the InitHello frame to send.
     * 3. UDP send → blocking recv with [UDP_RECV_TIMEOUT_MS] timeout.
     * 4. Hand the response to native and receive the 32-byte PSK.
     *
     * Returns `null` when:
     *   - the native lib isn't loaded,
     *   - the native protocol body is still M1-stub (returns null),
     *   - the UDP exchange times out or the peer rejects the frame,
     *   - the response isn't a valid PSK.
     *
     * On `null` the caller falls back to the server-provided PSK; on real
     * crypto errors we log and bail out (don't silently downgrade in a way
     * that hides a misconfigured peer).
     */
    private suspend fun tryBilateralExchange(context: Context, config: ConnectResponse): ByteArray? {
        if (!RosenpassNative.isLoaded) {
            Log.d(TAG, "rosenpass-jni not loaded — bilateral exchange unavailable")
            return null
        }
        val endpoint = config.rosenpassEndpoint
        if (endpoint.isNullOrBlank()) {
            Log.w(TAG, "rosenpassEndpoint missing — cannot run bilateral exchange")
            return null
        }
        val serverPubKey = try {
            Base64.decode(config.rosenpassPublicKey, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "malformed rosenpassPublicKey", e)
            return null
        }

        val keypair = loadOrGenerateKeypair(context) ?: return null

        val initFrame = try {
            RosenpassNative.initiateHandshake(serverPubKey, keypair.secretKey)
        } catch (e: Throwable) {
            Log.e(TAG, "native initiateHandshake threw", e)
            return null
        }
        if (initFrame == null) {
            // M1: protocol body is still a stub. The integration is wired but
            // there's nothing to send yet. Caller will fall back to server PSK.
            // When M2 lands and initiateHandshake returns real bytes, this
            // path will start exchanging real frames with no further changes.
            Log.d(TAG, "native initiateHandshake returned null (M1 stub) — fallback to server PSK")
            return null
        }

        val response = exchangeUdp(endpoint, initFrame) ?: return null

        val psk = try {
            RosenpassNative.handleResponse(response, keypair.secretKey)
        } catch (e: Throwable) {
            Log.e(TAG, "native handleResponse threw", e)
            return null
        }
        if (psk == null) {
            Log.w(TAG, "handleResponse returned null (stub or malformed response)")
            return null
        }
        if (psk.size != PSK_LENGTH_BYTES) {
            Log.e(TAG, "handleResponse returned wrong-sized PSK (${psk.size} != $PSK_LENGTH_BYTES)")
            psk.fill(0)
            return null
        }
        return psk
    }

    /**
     * UDP single-shot request/response with [UDP_RECV_TIMEOUT_MS] timeout.
     *
     * Endpoint format: `host:port`. Parsed defensively — bad format → null.
     */
    private suspend fun exchangeUdp(endpoint: String, frame: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        val (host, port) = parseEndpoint(endpoint) ?: run {
            Log.e(TAG, "malformed rosenpassEndpoint=$endpoint (expected host:port)")
            return@withContext null
        }
        var socket: DatagramSocket? = null
        try {
            withTimeoutOrNull(UDP_RECV_TIMEOUT_MS.toLong() + 1_000) {
                socket = DatagramSocket().apply { soTimeout = UDP_RECV_TIMEOUT_MS }
                val target = InetSocketAddress(host, port)
                socket!!.send(DatagramPacket(frame, frame.size, target))

                val buf = ByteArray(UDP_MTU)
                val pkt = DatagramPacket(buf, buf.size)
                socket!!.receive(pkt)  // blocks until soTimeout
                buf.copyOfRange(0, pkt.length)
            }
        } catch (e: Exception) {
            Log.w(TAG, "UDP exchange failed against $endpoint: ${e.message}")
            null
        } finally {
            runCatching { socket?.close() }
        }
    }

    private fun parseEndpoint(endpoint: String): Pair<String, Int>? {
        val idx = endpoint.lastIndexOf(':')
        if (idx <= 0 || idx == endpoint.length - 1) return null
        val host = endpoint.substring(0, idx).trim('[', ']')   // strip IPv6 brackets if present
        val port = endpoint.substring(idx + 1).toIntOrNull() ?: return null
        if (port !in 1..65535) return null
        if (host.isBlank()) return null
        return host to port
    }

    private fun loadOrGenerateKeypair(context: Context): RosenpassNative.StaticKeypair? {
        val store = ensureKeyStore(context)
        val existing = store.load()
        if (existing != null) return existing

        Log.i(TAG, "no persisted Rosenpass keypair — generating new (this can take a few seconds)")
        val fresh = try {
            RosenpassNative.generateKeypair()
        } catch (e: Throwable) {
            Log.e(TAG, "native generateKeypair failed", e)
            return null
        }
        try {
            store.save(fresh)
        } catch (e: Exception) {
            Log.e(TAG, "failed to persist new keypair — not caching, will regenerate next time", e)
        }
        return fresh
    }

    private fun ensureKeyStore(context: Context): RosenpassKeyStore {
        return keyStore ?: synchronized(this) {
            keyStore ?: RosenpassKeyStore(context.applicationContext).also { keyStore = it }
        }
    }

    // ── HKDF helpers (kept for future RFC-5869 PSK derivations on the wire) ─

    @Suppress("unused")  // Used by tests + reserved for M2 wire framing.
    internal fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    @Suppress("unused")
    internal fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val result = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var counter: Byte = 1
        while (offset < length) {
            val input = ByteArray(t.size + info.size + 1)
            System.arraycopy(t, 0, input, 0, t.size)
            System.arraycopy(info, 0, input, t.size, info.size)
            input[input.size - 1] = counter
            t = hmacSha256(prk, input)
            val copyLen = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, copyLen)
            offset += copyLen
            counter++
        }
        return result
    }

    /** For test teardown only. NOT for runtime use. */
    internal fun resetForTesting() {
        scope.coroutineContext.cancel()
        rekeyJob = null
        currentPsk?.fill(0)
        currentPsk = null
        keyStore = null
        _modeFlow.value = Mode.DISABLED
    }
}
