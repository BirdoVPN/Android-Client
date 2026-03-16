package app.birdo.vpn.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.birdo.vpn.data.model.PortForward
import app.birdo.vpn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortForwardScreen(
    portForwards: List<PortForward>,
    isLoading: Boolean,
    error: String?,
    onCreate: (internalPort: Int, protocol: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onBack: () -> Unit,
) {
    var portText by remember { mutableStateOf("") }
    var selectedProtocol by remember { mutableStateOf("tcp") }

    val portValue = portText.toIntOrNull()
    val isPortValid = portValue != null && portValue in 1..65535

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Port Forwarding",
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
            // ── Info Card ────────────────────────────────────────
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
                        Icon(Icons.Default.Info, null, tint = BirdoWhite40, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Forward external ports on your VPN server to a local port on your device. Useful for hosting services behind the VPN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BirdoWhite60,
                        )
                    }
                }
            }

            // ── Error Display ────────────────────────────────────
            if (error != null) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = BirdoRedBg,
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = BirdoRed,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            // ── New Rule Section ─────────────────────────────────
            item {
                Text(
                    text = "NEW RULE",
                    style = MaterialTheme.typography.labelMedium,
                    color = BirdoWhite40,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                    letterSpacing = 0.5.sp,
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = BirdoSurface,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = portText,
                            onValueChange = { text ->
                                portText = text.filter { it.isDigit() }.take(5)
                            },
                            label = { Text("Internal Port") },
                            placeholder = { Text("e.g. 8080", color = BirdoWhite20) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = portText.isNotEmpty() && !isPortValid,
                            supportingText = if (portText.isNotEmpty() && !isPortValid) {
                                { Text("Port must be 1–65535", color = BirdoRed) }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BirdoWhite,
                                unfocusedTextColor = BirdoWhite80,
                                focusedBorderColor = BirdoWhite60,
                                unfocusedBorderColor = BirdoWhite20,
                                cursorColor = BirdoWhite,
                                focusedLabelColor = BirdoWhite60,
                                unfocusedLabelColor = BirdoWhite40,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(12.dp))

                        // Protocol toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Protocol",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BirdoWhite60,
                            )
                            Spacer(Modifier.width(12.dp))

                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = selectedProtocol == "tcp",
                                    onClick = { selectedProtocol = "tcp" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = BirdoWhite10,
                                        activeContentColor = BirdoWhite,
                                        inactiveContainerColor = Color.Transparent,
                                        inactiveContentColor = BirdoWhite40,
                                        activeBorderColor = BirdoWhite20,
                                        inactiveBorderColor = BirdoWhite20,
                                    ),
                                ) {
                                    Text("TCP")
                                }
                                SegmentedButton(
                                    selected = selectedProtocol == "udp",
                                    onClick = { selectedProtocol = "udp" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = BirdoWhite10,
                                        activeContentColor = BirdoWhite,
                                        inactiveContainerColor = Color.Transparent,
                                        inactiveContentColor = BirdoWhite40,
                                        activeBorderColor = BirdoWhite20,
                                        inactiveBorderColor = BirdoWhite20,
                                    ),
                                ) {
                                    Text("UDP")
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isPortValid) {
                                    onCreate(portValue!!, selectedProtocol)
                                    portText = ""
                                }
                            },
                            enabled = isPortValid && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BirdoGreen,
                                contentColor = BirdoBlack,
                                disabledContainerColor = BirdoWhite10,
                                disabledContentColor = BirdoWhite40,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Rule", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Active Rules Section ─────────────────────────────
            item {
                Text(
                    text = "ACTIVE RULES",
                    style = MaterialTheme.typography.labelMedium,
                    color = BirdoWhite40,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                    letterSpacing = 0.5.sp,
                )
            }

            // Loading
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = BirdoWhite60,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // Empty state
            if (!isLoading && portForwards.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = BirdoSurface,
                    ) {
                        Text(
                            text = "No port forwarding rules yet. Add one above to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BirdoWhite40,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            // Port forward list
            items(portForwards, key = { it.id }) { pf ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = BirdoGreen,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${pf.externalPort}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BirdoWhite80,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = " → ",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BirdoWhite40,
                                )
                                Text(
                                    text = "${pf.internalPort}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BirdoWhite80,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(Modifier.height(2.dp))

                            // Protocol badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BirdoWhite10,
                            ) {
                                Text(
                                    text = pf.protocol.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BirdoWhite60,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        IconButton(onClick = { onDelete(pf.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete rule",
                                tint = BirdoRed,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
