package app.birdo.vpn.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Auth ────────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class TokenPair(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

/**
 * FIX C-2: The backend returns EITHER a 2FA challenge or a success with tokens.
 * Use a sealed class to represent both outcomes from the login endpoint.
 */
sealed class LoginResult {
    data class Success(val ok: Boolean, val tokens: TokenPair) : LoginResult()
    data class TwoFactorRequired(
        @SerialName("requires_two_factor") val requiresTwoFactor: Boolean,
        @SerialName("challenge_token") val challengeToken: String,
    ) : LoginResult()
}

/**
 * Raw login response used for deserialization — the actual LoginResult
 * is determined by checking the fields present in the response.
 */
@Serializable
data class LoginResponse(
    val ok: Boolean? = null,
    val tokens: TokenPair? = null,
    @SerialName("requires_two_factor") val requiresTwoFactor: Boolean? = null,
    @SerialName("challenge_token") val challengeToken: String? = null,
) {
    fun toLoginResult(): LoginResult {
        return if (requiresTwoFactor == true && challengeToken != null) {
            LoginResult.TwoFactorRequired(true, challengeToken)
        } else if (tokens != null) {
            LoginResult.Success(ok ?: false, tokens)
        } else {
            // Return a failed Success instead of throwing — callers should not
            // need to catch exceptions for a well-formed but unexpected response.
            LoginResult.Success(false, TokenPair("", ""))
        }
    }
}

/** FIX C-2: Request body for 2FA verification */
@Serializable
data class TwoFactorVerifyRequest(
    @SerialName("challenge_token") val challengeToken: String,
    val token: String,
)

/** FIX C-2: Response from 2FA verification */
@Serializable
data class TwoFactorVerifyResponse(
    val ok: Boolean,
    val tokens: TokenPair? = null,
    @SerialName("backup_code_used") val backupCodeUsed: Boolean = false,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

/** FIX C-1: RefreshResponse now includes rotated refresh_token */
@Serializable
data class RefreshResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 3600,
)

// ─── User ────────────────────────────────────────────────────────────────────

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val name: String? = null,
    val emailVerified: Boolean = false,
    val createdAt: String = "",
)

@Serializable
data class SubscriptionStatus(
    // M-13 FIX: Default must match backend tier name "RECON" (not "free")
    val plan: String = "RECON",
    val status: String = "active",
    val expiresAt: String? = null,
    val devicesUsed: Int = 0,
    val devicesLimit: Int = 1,
    val bandwidthUsed: Long = 0,
    val bandwidthLimit: Long? = null,
)

// ─── Anonymous Login ─────────────────────────────────────────────────────────

@Serializable
data class AnonymousLoginRequest(
    @SerialName("deviceId") val deviceId: String,
)

@Serializable
data class AnonymousLoginResponse(
    val ok: Boolean = false,
    @SerialName("anonymousId") val anonymousId: String? = null,
    val tokens: TokenPair? = null,
)

// ─── GDPR / Account Deletion ─────────────────────────────────────────────────

@Serializable
data class DeleteAccountRequest(
    val password: String,
)

@Serializable
data class DeleteAccountResponse(
    val success: Boolean = false,
    val message: String? = null,
    val deletedItems: Int = 0,
    val anonymizedItems: Int = 0,
)

/** GDPR Art. 20: Data export response wrapping all user data. */
@Serializable
data class GdprExportResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: kotlinx.serialization.json.JsonObject? = null,
)

// ─── VPN Servers ─────────────────────────────────────────────────────────────

@Serializable
data class VpnServer(
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val city: String = "",
    val hostname: String = "",
    val ipAddress: String = "",
    val port: Int = 51820,
    val load: Int = 0,
    val isPremium: Boolean = false,
    val isStreaming: Boolean = false,
    val isP2p: Boolean = false,
    val isOnline: Boolean = true,
)

// ─── VPN Connect ─────────────────────────────────────────────────────────────

@Serializable
data class ConnectRequest(
    val serverNodeId: String? = null,
    val deviceName: String? = null,
    val preferredRegion: String? = null,
    /** FIX-1-1: Client-generated public key. Server won't return a private key. */
    val clientPublicKey: String? = null,
    /** Request stealth mode (Xray Reality tunnel) */
    val stealthMode: Boolean = false,
    /** Request quantum protection (Rosenpass PQ-PSK) */
    val quantumProtection: Boolean = false,
)

@Serializable
data class ServerNodeInfo(
    val id: String,
    val name: String,
    val region: String = "",
    val country: String = "",
    val hostname: String = "",
)

@Serializable
data class ConnectResponse(
    val success: Boolean = false,
    val message: String? = null,
    val config: String? = null,
    val keyId: String? = null,
    val privateKey: String? = null,
    val publicKey: String? = null,
    val presharedKey: String? = null,
    val assignedIp: String? = null,
    val serverPublicKey: String? = null,
    val endpoint: String? = null,
    val dns: List<String>? = null,
    val allowedIps: List<String>? = null,
    val mtu: Int? = null,
    val persistentKeepalive: Int? = null,
    val serverNode: ServerNodeInfo? = null,
    // ── Stealth Mode (Xray Reality) ──────────────────────────────
    /** Whether server supports stealth connections via Xray Reality */
    val stealthEnabled: Boolean = false,
    /** Xray Reality server endpoint (ip:port), e.g. "144.172.110.131:8443" */
    val xrayEndpoint: String? = null,
    /** VLESS UUID for Xray authentication */
    val xrayUuid: String? = null,
    /** Reality public key for TLS fingerprint */
    val xrayPublicKey: String? = null,
    /** Reality short ID */
    val xrayShortId: String? = null,
    /** SNI domain for Reality (e.g. "www.microsoft.com") */
    val xraySni: String? = null,
    /** Xray flow control (e.g. "xtls-rprx-vision") */
    val xrayFlow: String? = null,
    // ── Quantum Protection (Rosenpass PQ-PSK) ────────────────────
    /** Whether quantum protection is available for this connection */
    val quantumEnabled: Boolean = false,
    /** Server's Rosenpass public key (Base64) for PQ key exchange */
    val rosenpassPublicKey: String? = null,
    /** Rosenpass key exchange endpoint (ip:port) */
    val rosenpassEndpoint: String? = null,
)

// ─── Multi-Hop (Double VPN) ──────────────────────────────────────────────────

@Serializable
data class MultiHopRoute(
    val entryNodeId: String,
    val exitNodeId: String,
    val entryCountry: String,
    val exitCountry: String,
)

@Serializable
data class MultiHopConnectRequest(
    val entryNodeId: String,
    val exitNodeId: String,
    val deviceName: String? = null,
    val clientPublicKey: String? = null,
)

@Serializable
data class MultiHopNodeInfo(
    val id: String,
    val name: String,
    val country: String,
    val region: String = "",
)

@Serializable
data class MultiHopInfo(
    val entryNode: MultiHopNodeInfo,
    val exitNode: MultiHopNodeInfo,
    val route: String,
)

@Serializable
data class MultiHopConnectResponse(
    val success: Boolean = false,
    val message: String? = null,
    val config: String? = null,
    val keyId: String? = null,
    val privateKey: String? = null,
    val publicKey: String? = null,
    val presharedKey: String? = null,
    val assignedIp: String? = null,
    val serverPublicKey: String? = null,
    val endpoint: String? = null,
    val dns: List<String>? = null,
    val allowedIps: List<String>? = null,
    val mtu: Int? = null,
    val persistentKeepalive: Int? = null,
    val multiHop: MultiHopInfo? = null,
)

// ─── Port Forwarding ─────────────────────────────────────────────────────────

@Serializable
data class PortForward(
    val id: String,
    val externalPort: Int,
    val internalPort: Int,
    val protocol: String = "tcp",
    val enabled: Boolean = true,
)

@Serializable
data class CreatePortForwardRequest(
    val internalPort: Int,
    val protocol: String = "tcp",
)

@Serializable
data class CreatePortForwardResponse(
    val success: Boolean = false,
    val portForward: PortForward? = null,
    val message: String? = null,
)

// ─── Key Rotation (P3-25) ────────────────────────────────────────────────────

@Serializable
data class KeyRotationRequest(
    val clientPublicKey: String,
)

@Serializable
data class KeyRotationResponse(
    val success: Boolean = false,
    val newKeyId: String = "",
    val serverPublicKey: String = "",
    val presharedKey: String? = null,
    val expiresAt: String = "",
)

// ─── Protocol Error Codes (from birdo-shared/protocol.json) ──────────────────

/**
 * Standardized error codes for cross-platform consistency.
 * Must match the ErrorCode enum in protocol.json exactly.
 */
@Serializable
enum class ProtocolErrorCode {
    @SerialName("AUTH_REQUIRED") AUTH_REQUIRED,
    @SerialName("AUTH_EXPIRED") AUTH_EXPIRED,
    @SerialName("SUBSCRIPTION_REQUIRED") SUBSCRIPTION_REQUIRED,
    @SerialName("SUBSCRIPTION_EXPIRED") SUBSCRIPTION_EXPIRED,
    @SerialName("DEVICE_LIMIT_REACHED") DEVICE_LIMIT_REACHED,
    @SerialName("RATE_LIMITED") RATE_LIMITED,
    @SerialName("SERVER_OFFLINE") SERVER_OFFLINE,
    @SerialName("SERVER_FULL") SERVER_FULL,
    @SerialName("NO_SERVERS_AVAILABLE") NO_SERVERS_AVAILABLE,
    @SerialName("TUNNEL_CREATION_FAILED") TUNNEL_CREATION_FAILED,
    @SerialName("TUNNEL_START_FAILED") TUNNEL_START_FAILED,
    @SerialName("DNS_CONFIGURATION_FAILED") DNS_CONFIGURATION_FAILED,
    @SerialName("ROUTE_CONFIGURATION_FAILED") ROUTE_CONFIGURATION_FAILED,
    @SerialName("KILL_SWITCH_FAILED") KILL_SWITCH_FAILED,
    @SerialName("IPV6_BLOCK_FAILED") IPV6_BLOCK_FAILED,
    @SerialName("STEALTH_TUNNEL_FAILED") STEALTH_TUNNEL_FAILED,
    @SerialName("QUANTUM_HANDSHAKE_FAILED") QUANTUM_HANDSHAKE_FAILED,
    @SerialName("ADMIN_REQUIRED") ADMIN_REQUIRED,
    @SerialName("NETWORK_UNREACHABLE") NETWORK_UNREACHABLE,
    @SerialName("HANDSHAKE_TIMEOUT") HANDSHAKE_TIMEOUT,
    @SerialName("DLL_INTEGRITY_FAILED") DLL_INTEGRITY_FAILED,
    @SerialName("JNI_INTEGRITY_FAILED") JNI_INTEGRITY_FAILED,
    @SerialName("SETTINGS_TAMPERED") SETTINGS_TAMPERED,
    @SerialName("BIOMETRIC_FAILED") BIOMETRIC_FAILED,
    @SerialName("UNKNOWN") UNKNOWN;

    /** User-facing message */
    val userMessage: String get() = when (this) {
        AUTH_REQUIRED -> "Please sign in to continue"
        AUTH_EXPIRED -> "Your session has expired — please sign in again"
        SUBSCRIPTION_REQUIRED -> "A subscription is required for this feature"
        SUBSCRIPTION_EXPIRED -> "Your subscription has expired"
        DEVICE_LIMIT_REACHED -> "Device limit reached — remove a device to connect"
        RATE_LIMITED -> "Too many requests — please wait a moment"
        SERVER_OFFLINE -> "This server is currently offline"
        SERVER_FULL -> "This server is at capacity — try another"
        NO_SERVERS_AVAILABLE -> "No servers available — check back shortly"
        TUNNEL_CREATION_FAILED -> "Failed to create VPN tunnel"
        TUNNEL_START_FAILED -> "Failed to start VPN tunnel"
        DNS_CONFIGURATION_FAILED -> "Failed to configure DNS"
        ROUTE_CONFIGURATION_FAILED -> "Failed to configure routing"
        KILL_SWITCH_FAILED -> "Kill switch activation failed"
        IPV6_BLOCK_FAILED -> "IPv6 leak protection failed"
        STEALTH_TUNNEL_FAILED -> "Stealth tunnel failed — try without stealth mode"
        QUANTUM_HANDSHAKE_FAILED -> "Post-quantum handshake failed — try without quantum protection"
        ADMIN_REQUIRED -> "Administrator privileges are required"
        NETWORK_UNREACHABLE -> "Network is unreachable — check your connection"
        HANDSHAKE_TIMEOUT -> "Connection timed out — try a closer server"
        DLL_INTEGRITY_FAILED -> "Security check failed — application files may be corrupted"
        JNI_INTEGRITY_FAILED -> "Security check failed — application files may be corrupted"
        SETTINGS_TAMPERED -> "Settings integrity check failed"
        BIOMETRIC_FAILED -> "Biometric authentication failed"
        UNKNOWN -> "An unexpected error occurred"
    }
}

/** Error body returned by the backend in non-2xx responses */
@Serializable
data class ApiErrorBody(
    val errorCode: ProtocolErrorCode? = null,
    val message: String? = null,
)

// ─── Heartbeat ───────────────────────────────────────────────────────────────

/** P1-9: Heartbeat response from the backend */
@Serializable
data class HeartbeatResponse(
    val valid: Boolean = true,
    val serverOnline: Boolean = true,
    val message: String? = null,
)

// ─── Connection Quality Reporting (P2-15) ────────────────────────────────────

/** Client-reported quality telemetry sent periodically while connected. */
@Serializable
data class QualityReport(
    val keyId: String,
    val latencyMs: Double,
    val jitterMs: Double,
    val packetLossPercent: Double,
    val bytesIn: Long,
    val bytesOut: Long,
    val handshakeAgeSeconds: Long,
    val connectionState: String,
    val platform: String,
)
