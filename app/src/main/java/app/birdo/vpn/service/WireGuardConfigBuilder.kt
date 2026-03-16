package app.birdo.vpn.service

import android.util.Log
import app.birdo.vpn.data.model.ConnectResponse
import app.birdo.vpn.data.preferences.AppPreferences
import com.wireguard.config.*
import com.wireguard.crypto.Key
import java.net.InetAddress

/**
 * Builds a wg-go [Config] from a server [ConnectResponse] and user preferences.
 *
 * Extracted from [BirdoVpnService] for testability and readability.
 */
object WireGuardConfigBuilder {

    private const val TAG = "WgConfigBuilder"

    /**
     * Build a WireGuard [Config] from the API connect response and user prefs.
     * Zeroes key material from intermediate objects after the config snapshot is taken.
     */
    fun build(response: ConnectResponse, prefs: AppPreferences): Config {
        val privateKey = Key.fromBase64(response.privateKey!!)
        val peerPublicKey = Key.fromBase64(response.serverPublicKey!!)

        val interfaceBuilder = Interface.Builder()
            .parsePrivateKey(privateKey.toBase64())
            .addAddress(InetNetwork.parse("${response.assignedIp}/32"))

        for (dns in resolveDnsServers(response, prefs)) {
            try { interfaceBuilder.addDnsServer(InetAddress.getByName(dns)) } catch (_: Exception) {}
        }

        val userMtu = prefs.wireGuardMtu
        val effectiveMtu = (if (userMtu > 0) userMtu else (response.mtu ?: 1420)).coerceIn(1280, 1500)
        try { interfaceBuilder.parseMtu(effectiveMtu.toString()) } catch (_: Exception) {}

        val effectiveEndpoint = applyPortOverride(response.endpoint!!, prefs)

        val peerBuilder = Peer.Builder()
            .parsePublicKey(peerPublicKey.toBase64())
            .parseEndpoint(effectiveEndpoint)
            .parsePersistentKeepalive("${(response.persistentKeepalive ?: 25).coerceIn(1, 300)}")
        for (cidr in response.allowedIps ?: listOf("0.0.0.0/0", "::/0")) {
            try { peerBuilder.addAllowedIp(InetNetwork.parse(cidr)) } catch (_: Exception) {}
        }
        response.presharedKey?.let {
            try { peerBuilder.parsePreSharedKey(it) } catch (_: Exception) {}
        }

        val config = Config.Builder()
            .setInterface(interfaceBuilder.build())
            .addPeer(peerBuilder.build())
            .build()

        // Zero key material after config is built.
        try { privateKey.bytes.fill(0) } catch (_: Exception) {}
        try { peerPublicKey.bytes.fill(0) } catch (_: Exception) {}

        return config
    }

    /**
     * Apply the user's WireGuard port override to the endpoint string.
     * "auto" keeps the server-provided port.
     */
    fun applyPortOverride(endpoint: String, prefs: AppPreferences): String {
        val portPref = prefs.wireGuardPort
        if (portPref == "auto") return endpoint
        val overridePort = portPref.toIntOrNull() ?: return endpoint
        if (overridePort !in 1..65535) return endpoint
        val lastColon = endpoint.lastIndexOf(':')
        return if (lastColon > 0) endpoint.substring(0, lastColon + 1) + overridePort
        else "$endpoint:$overridePort"
    }

    /** Resolve DNS servers, preferring user overrides when enabled. */
    private fun resolveDnsServers(config: ConnectResponse, prefs: AppPreferences): List<String> {
        val fallback = listOf("1.1.1.1", "1.0.0.1")
        if (!prefs.customDnsEnabled) {
            val serverDns = config.dns?.filter { isValidDnsAddress(it) } ?: emptyList()
            return serverDns.ifEmpty { fallback }
        }
        val custom = buildList {
            val p = prefs.customDnsPrimary.trim()
            if (p.isNotBlank() && isValidDnsAddress(p)) add(p)
            val s = prefs.customDnsSecondary.trim()
            if (s.isNotBlank() && isValidDnsAddress(s)) add(s)
        }
        if (custom.isEmpty()) {
            Log.w(TAG, "Custom DNS addresses invalid or empty — falling back to defaults")
        }
        return custom.ifEmpty { fallback }
    }

    private fun isValidDnsAddress(address: String): Boolean {
        return try {
            val addr = InetAddress.getByName(address)
            !addr.isLoopbackAddress && !addr.isAnyLocalAddress
        } catch (_: Exception) {
            false
        }
    }
}
