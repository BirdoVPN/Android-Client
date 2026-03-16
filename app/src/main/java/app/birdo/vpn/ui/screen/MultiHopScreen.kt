package app.birdo.vpn.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.birdo.vpn.data.model.VpnServer
import app.birdo.vpn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiHopScreen(
    servers: List<VpnServer>,
    isConnecting: Boolean,
    error: String?,
    onConnect: (entryNodeId: String, exitNodeId: String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedEntry by remember { mutableStateOf<VpnServer?>(null) }
    var selectedExit by remember { mutableStateOf<VpnServer?>(null) }
    var entryExpanded by remember { mutableStateOf(false) }
    var exitExpanded by remember { mutableStateOf(false) }

    val canConnect = selectedEntry != null &&
        selectedExit != null &&
        selectedEntry?.id != selectedExit?.id &&
        !isConnecting

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Multi-Hop",
                        fontWeight = FontWeight.Bold,
                        color = BirdoWhite80,
                        fontFamily = FontFamily.SansSerif,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BirdoWhite60,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Description ──────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = BirdoWhite10,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = BirdoWhite40,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Route your traffic through two VPN servers for extra privacy. " +
                                "Your connection enters at the first server and exits from the second, " +
                                "so neither server sees both your real IP and your destination.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BirdoWhite60,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // ── Entry Server ─────────────────────────────────────
            item {
                Text(
                    text = "ENTRY SERVER",
                    style = MaterialTheme.typography.labelMedium,
                    color = BirdoWhite40,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                    letterSpacing = 0.5.sp,
                )
            }

            item {
                ServerSelector(
                    selected = selectedEntry,
                    expanded = entryExpanded,
                    onToggle = {
                        entryExpanded = !entryExpanded
                        if (entryExpanded) exitExpanded = false
                    },
                    placeholder = "Select entry server",
                )
            }

            if (entryExpanded) {
                items(
                    items = servers.filter { it.isOnline },
                    key = { "entry-${it.id}" },
                ) { server ->
                    ServerListItem(
                        server = server,
                        isSelected = selectedEntry?.id == server.id,
                        isDisabled = selectedExit?.id == server.id,
                        onClick = {
                            selectedEntry = server
                            entryExpanded = false
                        },
                    )
                }
            }

            // ── Swap indicator ───────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "Multi-hop route",
                        tint = BirdoWhite40,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // ── Exit Server ──────────────────────────────────────
            item {
                Text(
                    text = "EXIT SERVER",
                    style = MaterialTheme.typography.labelMedium,
                    color = BirdoWhite40,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    letterSpacing = 0.5.sp,
                )
            }

            item {
                ServerSelector(
                    selected = selectedExit,
                    expanded = exitExpanded,
                    onToggle = {
                        exitExpanded = !exitExpanded
                        if (exitExpanded) entryExpanded = false
                    },
                    placeholder = "Select exit server",
                )
            }

            if (exitExpanded) {
                items(
                    items = servers.filter { it.isOnline },
                    key = { "exit-${it.id}" },
                ) { server ->
                    ServerListItem(
                        server = server,
                        isSelected = selectedExit?.id == server.id,
                        isDisabled = selectedEntry?.id == server.id,
                        onClick = {
                            selectedExit = server
                            exitExpanded = false
                        },
                    )
                }
            }

            // ── Error ────────────────────────────────────────────
            item {
                AnimatedVisibility(visible = error != null) {
                    if (error != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = BirdoRed.copy(alpha = 0.1f),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = BirdoRed,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BirdoRed,
                                )
                            }
                        }
                    }
                }
            }

            // ── Same-server warning ──────────────────────────────
            item {
                AnimatedVisibility(
                    visible = selectedEntry != null &&
                        selectedExit != null &&
                        selectedEntry?.id == selectedExit?.id,
                ) {
                    Text(
                        "Entry and exit servers must be different.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirdoRed,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
            }

            // ── Connect Button ───────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (canConnect) {
                            onConnect(selectedEntry!!.id, selectedExit!!.id)
                        }
                    },
                    enabled = canConnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BirdoGreen,
                        contentColor = BirdoBlack,
                        disabledContainerColor = BirdoWhite10,
                        disabledContentColor = BirdoWhite40,
                    ),
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = BirdoBlack,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Connecting…",
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Connect Multi-Hop",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Private Components ───────────────────────────────────────────────────────

@Composable
private fun ServerSelector(
    selected: VpnServer?,
    expanded: Boolean,
    onToggle: () -> Unit,
    placeholder: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        color = BirdoSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                tint = if (selected != null) BirdoGreen else BirdoWhite40,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (selected != null) {
                    Text(
                        selected.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = BirdoWhite80,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${selected.city.ifEmpty { selected.country }} • ${selected.countryCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirdoWhite40,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.titleSmall,
                        color = BirdoWhite40,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = BirdoWhite40,
            )
        }
    }
}

@Composable
private fun ServerListItem(
    server: VpnServer,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
) {
    val contentAlpha = if (isDisabled) 0.4f else 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isDisabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) BirdoWhite10 else Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                tint = (if (isSelected) BirdoGreen else BirdoWhite40).copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    server.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BirdoWhite80.copy(alpha = contentAlpha),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(server.city.ifEmpty { server.country })
                        append(" • ")
                        append(server.countryCode)
                        if (server.load > 0) {
                            append(" • ${server.load}% load")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BirdoWhite40.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = BirdoGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (isDisabled && !isSelected) {
                Text(
                    "in use",
                    style = MaterialTheme.typography.labelSmall,
                    color = BirdoWhite20,
                )
            }
        }
    }
}
