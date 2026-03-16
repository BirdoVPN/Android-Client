package app.birdo.vpn.service

import android.net.VpnService
import android.util.Log

/**
 * Monitors a wg-go WireGuard tunnel, periodically re-protecting sockets
 * so that the underlying UDP traffic bypasses the VPN tun interface.
 *
 * Extracted from [BirdoVpnService] for testability and readability.
 *
 * @param handle  The wg-go tunnel handle returned by [WgNative.turnOn]
 * @param service The [VpnService] instance providing [VpnService.protect]
 * @param isAlive Callback returning `true` while the tunnel should be monitored
 * @param onUnexpectedExit Called if the monitor loop exits while the tunnel is
 *        supposedly still alive (trigger kill switch)
 */
class TunnelMonitor(
    private val handle: Int,
    private val service: VpnService,
    private val isAlive: () -> Boolean,
    private val onUnexpectedExit: () -> Unit,
) {
    companion object {
        private const val TAG = "TunnelMonitor"
        private const val CHECK_INTERVAL_MS = 5_000L
    }

    private var thread: Thread? = null

    /** Start the monitor on a background daemon thread. */
    fun start() {
        thread = Thread({
            Log.i(TAG, "Tunnel monitor started for handle=$handle")
            try {
                while (isAlive() && !Thread.currentThread().isInterrupted) {
                    Thread.sleep(CHECK_INTERVAL_MS)
                    val v4 = WgNative.getSocketV4(handle)
                    if (v4 >= 0) service.protect(v4)
                    val v6 = WgNative.getSocketV6(handle)
                    if (v6 >= 0) service.protect(v6)
                }
            } catch (_: InterruptedException) {
                Log.i(TAG, "Tunnel monitor interrupted")
            }
            if (isAlive()) {
                Log.w(TAG, "Tunnel monitor exited while connected — triggering kill switch")
                onUnexpectedExit()
            }
            Log.i(TAG, "Tunnel monitor exiting")
        }, "birdo-tunnel-monitor").apply { isDaemon = true; start() }
    }

    /** Interrupt the monitor thread and release the reference. */
    fun stop() {
        thread?.interrupt()
        thread = null
    }
}
