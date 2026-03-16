package app.birdo.vpn.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.birdo.vpn.R
import app.birdo.vpn.service.VpnState
import app.birdo.vpn.ui.TestTags
import app.birdo.vpn.ui.theme.*
import app.birdo.vpn.ui.viewmodel.VpnUiState
import app.birdo.vpn.utils.FormatUtils
import app.birdo.vpn.utils.countryCodeToFlag
import kotlinx.coroutines.delay

/**
 * Home screen / Connect tab — matches the Windows client design:
 * - Header with "Birdo VPN" title + logout
 * - Centered: Status badge → Server location → Big power button → Stats
 * - Kill switch indicator
 * - Server selector card at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: VpnUiState,
    userEmail: String?,
    killSwitchEnabled: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenServers: () -> Unit,
    onLogout: () -> Unit,
) {
    val isConnected = state.vpnState is VpnState.Connected
    val isConnecting = state.vpnState is VpnState.Connecting
    val isDisconnecting = state.vpnState is VpnState.Disconnecting
    val isKillSwitchActive = state.killSwitchActive
    // Read tick to force recomposition every second (for duration timer)
    @Suppress("UNUSED_VARIABLE") val tick = state.tick

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        // ── Header bar (matches glass-strong with border-b) ──
        Surface(
            color = GlassStrong,
            tonalElevation = 0.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(Modifier.weight(1f))
                    if (userEmail != null) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = BirdoWhite40,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .widthIn(max = 140.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            stringResource(R.string.logout),
                            tint = BirdoWhite40,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                // Subtle bottom border line
                HorizontalDivider(
                    color = BirdoWhite05,
                    thickness = 1.dp,
                )
            }
        }

        // ── Main content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Status Badge ──
            StatusBadge(
                isConnected = isConnected,
                isConnecting = isConnecting,
                isDisconnecting = isDisconnecting,
                isError = state.vpnState is VpnState.Error,
            )

            // ── Server location (when connected) ──
            if (isConnected && state.connectedServer != null) {
                Text(
                    text = state.connectedServer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BirdoWhite60,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Big Power Button ──
            ConnectionButton(
                isConnected = isConnected,
                isConnecting = isConnecting,
                isDisconnecting = isDisconnecting,
                onClick = {
                    when {
                        isConnected -> onDisconnect()
                        isConnecting || isDisconnecting -> {}
                        else -> onConnect()
                    }
                },
            )

            Spacer(Modifier.height(24.dp))

            // ── Stats grid (when connected) ──
            AnimatedVisibility(
                visible = isConnected,
                enter = fadeIn(tween(300, delayMillis = 100)) + slideInVertically(initialOffsetY = { 10 }),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatsCard(stringResource(R.string.stats_duration), FormatUtils.formatDuration(state.connectedSince), Modifier.weight(1f))
                    StatsCard(stringResource(R.string.stats_download), FormatUtils.formatBytes(state.rxBytes), Modifier.weight(1f))
                    StatsCard(stringResource(R.string.stats_upload), FormatUtils.formatBytes(state.txBytes), Modifier.weight(1f))
                }
            }

            // ── Security indicators (Kill Switch + Stealth + Quantum) ──
            AnimatedVisibility(visible = isConnected) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (killSwitchEnabled) {
                        SecurityBadge(
                            icon = Icons.Default.Shield,
                            label = stringResource(R.string.kill_switch_active),
                            tintColor = BirdoGreenLight,
                        )
                    }
                    if (state.stealthActive) {
                        SecurityBadge(
                            icon = Icons.Default.VisibilityOff,
                            label = "Stealth",
                            tintColor = BirdoBlue,
                        )
                    }
                    if (state.quantumActive) {
                        SecurityBadge(
                            icon = Icons.Default.Lock,
                            label = "Quantum",
                            tintColor = BirdoPurpleLight,
                        )
                    }
                }
            }

            // ── Kill Switch blocking alert ──
            AnimatedVisibility(visible = isKillSwitchActive) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = BirdoRedBg,
                    border = BorderStroke(1.dp, BirdoRed.copy(alpha = 0.2f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Shield, stringResource(R.string.cd_kill_switch), tint = BirdoRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.kill_switch_blocking),
                            style = MaterialTheme.typography.bodySmall,
                            color = BirdoRed,
                        )
                    }
                }
            }

            // ── Error messages ──
            if (state.vpnState is VpnState.Error) {
                ErrorBanner(state.vpnState.message)
            }
            if (state.error != null) {
                ErrorBanner(state.error)
            }

            Spacer(Modifier.weight(1f))

            // ── Server Selector Card ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isConnected && !isConnecting, role = Role.Button) { onOpenServers() }
                    .testTag(TestTags.SERVER_SELECTOR),
                shape = RoundedCornerShape(12.dp),
                color = GlassLight,
                border = BorderStroke(1.dp, BirdoBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val server = state.selectedServer

                    // Country flag or globe
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BirdoWhite10),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (server != null) countryCodeToFlag(server.countryCode) else "🌐",
                            fontSize = 20.sp,
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = server?.name ?: stringResource(R.string.select_server),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                        )
                        if (server != null) {
                            Text(
                                text = "${server.city.ifBlank { server.country }} · ${stringResource(R.string.server_load, server.load)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BirdoWhite60,
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        stringResource(R.string.cd_select_server),
                        tint = BirdoWhite40,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Status Badge ────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(
    isConnected: Boolean,
    isConnecting: Boolean,
    isDisconnecting: Boolean,
    isError: Boolean,
) {
    val bgColor = when {
        isConnected -> BirdoGreenBg
        isConnecting || isDisconnecting -> BirdoYellowBg
        isError -> BirdoRedBg
        else -> BirdoWhite05
    }
    val textColor = when {
        isConnected -> BirdoGreenLight
        isConnecting || isDisconnecting -> BirdoYellowLight
        isError -> BirdoRed
        else -> BirdoWhite40
    }
    val borderColor = when {
        isConnected -> BirdoGreen.copy(alpha = 0.2f)
        isConnecting || isDisconnecting -> BirdoYellow.copy(alpha = 0.2f)
        isError -> BirdoRed.copy(alpha = 0.2f)
        else -> BirdoWhite10
    }
    val icon = when {
        isConnected -> Icons.Default.Wifi
        else -> Icons.Default.WifiOff
    }
    val label = when {
        isConnected -> stringResource(R.string.status_protected)
        isConnecting -> stringResource(R.string.connecting)
        isDisconnecting -> stringResource(R.string.disconnecting)
        isError -> stringResource(R.string.status_error)
        else -> stringResource(R.string.status_not_connected)
    }

    Surface(
        modifier = Modifier.testTag(TestTags.VPN_STATUS),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isConnecting || isDisconnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = textColor,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(icon, label, tint = textColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
            )
        }
    }
}

// ── Connection Button ───────────────────────────────────────────────────────

@Composable
private fun ConnectionButton(
    isConnected: Boolean,
    isConnecting: Boolean,
    isDisconnecting: Boolean,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Pulse rings for connected state
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "r1s",
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "r1a",
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 300),
            repeatMode = RepeatMode.Restart,
        ), label = "r2s",
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 300),
            repeatMode = RepeatMode.Restart,
        ), label = "r2a",
    )

    val buttonSize = 128.dp
    val bgColor = when {
        isConnected -> BirdoGreen
        isConnecting || isDisconnecting -> BirdoYellow
        else -> BirdoWhite10
    }

    Box(
        modifier = Modifier.size(buttonSize * 1.6f),
        contentAlignment = Alignment.Center,
    ) {
        // Pulse rings when connected
        if (isConnected) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .scale(ring1Scale)
                    .clip(CircleShape)
                    .background(BirdoGreen.copy(alpha = ring1Alpha)),
            )
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .scale(ring2Scale)
                    .clip(CircleShape)
                    .background(BirdoGreen.copy(alpha = ring2Alpha)),
            )
        }

        // Main button
        Box(
            modifier = Modifier
                .size(buttonSize)
                .shadow(
                    elevation = if (isConnected) 24.dp else 0.dp,
                    shape = CircleShape,
                    ambientColor = if (isConnected) BirdoGreenShadow else Color.Transparent,
                    spotColor = if (isConnected) BirdoGreenShadow else Color.Transparent,
                )
                .clip(CircleShape)
                .background(bgColor)
                .then(
                    if (!isConnected && !isConnecting && !isDisconnecting)
                        Modifier.border(1.dp, BirdoWhite20, CircleShape)
                    else Modifier
                )
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(TestTags.CONNECT_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            if (isConnecting || isDisconnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeWidth = 4.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isConnected) stringResource(R.string.disconnect) else stringResource(R.string.connect),
                    tint = if (isConnected) Color.White else BirdoWhite60,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}

// ── Stats Card ──────────────────────────────────────────────────────────────

@Composable
private fun StatsCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = BirdoCard,
        border = BorderStroke(1.dp, BirdoBorder),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = BirdoWhite60,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

// ── Security Badge ──────────────────────────────────────────────────────────

@Composable
private fun SecurityBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tintColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = BirdoWhite60,
        )
    }
}

// ── Error Banner ────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = BirdoRedBg,
        border = BorderStroke(1.dp, BirdoRed.copy(alpha = 0.2f)),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = BirdoRed,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Helpers (delegated to shared FormatUtils) ──────────────────────────────

private fun formatDuration(since: Long): String = FormatUtils.formatDuration(since)

private fun formatBytes(bytes: Long): String = FormatUtils.formatBytes(bytes)
