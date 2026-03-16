package app.birdo.vpn.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.birdo.vpn.R
import app.birdo.vpn.data.model.VpnServer
import androidx.compose.ui.semantics.Role
import app.birdo.vpn.ui.theme.*
import app.birdo.vpn.utils.countryCodeToFlag

/**
 * Server list screen — matches the Windows client design:
 * - Search input with glass styling
 * - Filter pills: All | ★ Favorites | 🎬 Streaming | ↓ P2P
 * - Server cards with country flag, name, load bar, favorite star
 * - White-based color scheme (no purple accents)
 * - Selected server: subtle white ring (ring-white/20)
 */
enum class ServerFilter(val label: String, val icon: String) {
    All("All", ""),
    Favorites("Favorites", "★"),
    Streaming("Streaming", "🎬"),
    P2P("P2P", "⬇"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    servers: List<VpnServer>,
    selectedServer: VpnServer?,
    isLoading: Boolean,
    favoriteServers: Set<String>,
    onSelectServer: (VpnServer) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(ServerFilter.All) }

    val filteredServers = remember(servers, searchQuery, activeFilter, favoriteServers) {
        servers
            .filter { server ->
                val matchesSearch = searchQuery.isBlank() ||
                    server.name.contains(searchQuery, ignoreCase = true) ||
                    server.country.contains(searchQuery, ignoreCase = true) ||
                    server.city.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (activeFilter) {
                    ServerFilter.All -> true
                    ServerFilter.Favorites -> favoriteServers.contains(server.id)
                    ServerFilter.Streaming -> server.isStreaming
                    ServerFilter.P2P -> server.isP2p
                }

                matchesSearch && matchesFilter
            }
            .sortedWith(
                compareByDescending<VpnServer> { favoriteServers.contains(it.id) }
                    .thenBy { !it.isOnline }
                    .thenBy { it.load }
                    .thenBy { it.name }
            )
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        // ── Header ──
        Surface(
            color = GlassStrong,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.servers_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        stringResource(R.string.cd_refresh),
                        tint = BirdoWhite40,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // ── Search bar ──
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp),
            color = GlassInput,
            border = BorderStroke(1.dp, BirdoWhite10),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    stringResource(R.string.cd_search),
                    tint = BirdoWhite40,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(stringResource(R.string.servers_search_placeholder), color = BirdoWhite20, fontSize = 14.sp)
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = BirdoWhite80,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                )

                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.cd_clear),
                            tint = BirdoWhite40,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // ── Filter pills (white-based, matching Windows) ──
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ServerFilter.entries) { filter ->
                val isActive = filter == activeFilter
                val favCount = if (filter == ServerFilter.Favorites) favoriteServers.size else null

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Tab) { activeFilter = filter },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) BirdoWhite10 else Color.Transparent,
                    border = if (isActive) BorderStroke(1.dp, BirdoWhite20) else null,
                ) {
                    Text(
                        text = buildString {
                            if (filter.icon.isNotEmpty()) {
                                append(filter.icon)
                                append(" ")
                            }
                            append(filter.label)
                            if (favCount != null && favCount > 0) {
                                append(" ($favCount)")
                            }
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                        color = if (isActive) Color.White else BirdoWhite60,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }

        // ── Server count ──
        Text(
            text = stringResource(R.string.servers_count, filteredServers.size),
            style = MaterialTheme.typography.bodySmall,
            color = BirdoWhite20,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // ── Loading indicator ──
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                trackColor = BirdoWhite10,
            )
        }

        // ── Server list ──
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(
                items = filteredServers,
                key = { it.id },
            ) { server ->
                ServerCard(
                    server = server,
                    isSelected = server.id == selectedServer?.id,
                    isFavorite = favoriteServers.contains(server.id),
                    onSelect = { onSelectServer(server) },
                    onToggleFavorite = { onToggleFavorite(server.id) },
                )
            }

            // ── Empty state ──
            if (filteredServers.isEmpty() && !isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Dns,
                            stringResource(R.string.cd_no_servers),
                            tint = BirdoWhite20,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = when {
                                activeFilter == ServerFilter.Favorites ->
                                    stringResource(R.string.servers_no_favorites)
                                searchQuery.isNotBlank() ->
                                    stringResource(R.string.servers_no_match, searchQuery)
                                else -> stringResource(R.string.no_servers)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = BirdoWhite40,
                        )
                        if (activeFilter == ServerFilter.Favorites) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.servers_favorites_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = BirdoWhite20,
                            )
                        }
                        if (servers.isEmpty() && !isLoading) {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onRefresh,
                                border = BorderStroke(1.dp, BirdoWhite20),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(Icons.Default.Refresh, "Refresh servers", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.retry), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Server Card ─────────────────────────────────────────────────────────────

@Composable
private fun ServerCard(
    server: VpnServer,
    isSelected: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = server.isOnline, role = Role.Button) { onSelect() }
            .then(
                if (isSelected) Modifier.border(1.dp, BirdoWhite20, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = BirdoCard,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .then(if (!server.isOnline) Modifier.alpha(0.5f) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Country flag badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BirdoWhite10),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = countryCodeToFlag(server.countryCode),
                    fontSize = 18.sp,
                )
            }

            Spacer(Modifier.width(12.dp))

            // Server info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = server.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (server.isOnline) Color.White else BirdoWhite20,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (server.isPremium) {
                        Text("⚡", fontSize = 11.sp)
                    }
                    if (server.isStreaming) {
                        Text("🎬", fontSize = 11.sp)
                    }
                    if (server.isP2p) {
                        Text("⬇", fontSize = 11.sp)
                    }
                }
                Text(
                    text = if (server.city.isNotBlank()) "${server.city}, ${server.country}" else server.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = BirdoWhite60,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Load indicator
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${server.load}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = loadColor(server.load),
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BirdoWhite10),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(server.load / 100f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(loadColor(server.load)),
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            // Favorite star
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) stringResource(R.string.cd_remove_favorite) else stringResource(R.string.cd_add_favorite),
                    tint = if (isFavorite) BirdoYellowLight else BirdoWhite20,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── Helpers ──

private fun loadColor(load: Int): Color = when {
    load < 50 -> BirdoGreenLight
    load < 80 -> BirdoYellowLight
    else -> BirdoRed
}
