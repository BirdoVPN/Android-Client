package app.birdo.vpn.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import app.birdo.vpn.R
import app.birdo.vpn.ui.theme.*
import app.birdo.vpn.ui.viewmodel.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    onToggleApp: (String) -> Unit,
    onBack: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val excludedCount = apps.count { it.isExcluded }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.split_tunnel_title),
                        fontWeight = FontWeight.Bold,
                        color = BirdoWhite80,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = BirdoWhite60)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Info banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFA855F7).copy(alpha = 0.1f),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Info,
                        stringResource(R.string.cd_info),
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.split_tunnel_info, excludedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA855F7).copy(alpha = 0.8f),
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.split_tunnel_search_placeholder), color = BirdoWhite20) },
                leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.cd_search), tint = BirdoWhite40) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BirdoWhite40,
                    unfocusedBorderColor = BirdoBorder,
                    focusedTextColor = BirdoWhite,
                    unfocusedTextColor = BirdoWhite80,
                    cursorColor = BirdoWhite,
                    focusedContainerColor = BirdoSurface,
                    unfocusedContainerColor = BirdoSurface,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BirdoWhite40, strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppItem(
                            app = app,
                            onToggle = { onToggleApp(app.packageName) },
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    app: AppInfo,
    onToggle: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (app.isExcluded) Color(0xFFA855F7).copy(alpha = 0.08f) else BirdoSurface,
        label = "appBg",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = app.isExcluded, role = Role.Checkbox, onValueChange = { onToggle() }),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App icon
            if (app.icon != null) {
                Image(
                    bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BirdoWhite10),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Android, stringResource(R.string.cd_app), tint = BirdoWhite40, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = BirdoWhite80,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = BirdoWhite20,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Bypass badge
            if (app.isExcluded) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFA855F7).copy(alpha = 0.2f),
                ) {
                    Text(
                        stringResource(R.string.split_tunnel_bypass_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA855F7),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Checkbox(
                checked = app.isExcluded,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFA855F7),
                    uncheckedColor = BirdoWhite20,
                    checkmarkColor = Color.White,
                ),
            )
        }
    }
}
