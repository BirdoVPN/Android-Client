package app.birdo.vpn.utils

/**
 * Shared formatting utilities used by UI (HomeScreen) and
 * background services (VpnNotificationManager, BirdoVpnService).
 *
 * Centralised here to eliminate duplicated format logic.
 */
object FormatUtils {

    /**
     * Format a byte count into a human-readable string.
     * Examples: "0 B", "1.2 KB", "45.3 MB", "1.23 GB".
     */
    fun formatBytes(bytes: Long): String = when {
        bytes <= 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }

    /**
     * Format elapsed time since [sinceMillis] as "HH:MM:SS" or "MM:SS".
     * Returns "00:00" if [sinceMillis] is <= 0.
     */
    fun formatDuration(sinceMillis: Long): String {
        if (sinceMillis <= 0) return "00:00"
        val elapsed = (System.currentTimeMillis() - sinceMillis) / 1000
        val h = elapsed / 3600
        val m = (elapsed % 3600) / 60
        val s = elapsed % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}
