package app.birdo.vpn.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.birdo.vpn.ui.theme.BirdoGreen
import app.birdo.vpn.ui.viewmodel.SpeedTestPhase
import app.birdo.vpn.ui.viewmodel.SpeedTestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpeedTestViewModel = hiltViewModel(),
) {
    val phase by viewModel.phase.collectAsState()
    val result by viewModel.result.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speed Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Latency card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Latency",
                    value = "${result.latencyMs}",
                    unit = "ms",
                    active = phase == SpeedTestPhase.Latency,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Jitter",
                    value = "${result.jitterMs}",
                    unit = "ms",
                    active = phase == SpeedTestPhase.Latency,
                )
            }

            // Download card
            MetricCard(
                modifier = Modifier.fillMaxWidth(),
                label = "Download",
                value = "%.1f".format(result.downloadMbps),
                unit = "Mbps",
                active = phase == SpeedTestPhase.Download,
            )

            // Upload card
            MetricCard(
                modifier = Modifier.fillMaxWidth(),
                label = "Upload",
                value = "%.1f".format(result.uploadMbps),
                unit = "Mbps",
                active = phase == SpeedTestPhase.Upload,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Error message
            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            // Action button
            val isRunning = phase != SpeedTestPhase.Idle && phase != SpeedTestPhase.Done && phase != SpeedTestPhase.Error
            Button(
                onClick = { viewModel.runTest() },
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (phase) {
                            SpeedTestPhase.Latency -> "Measuring latency…"
                            SpeedTestPhase.Download -> "Testing download…"
                            SpeedTestPhase.Upload -> "Testing upload…"
                            else -> "Running…"
                        },
                    )
                } else {
                    Icon(Icons.Filled.Speed, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (phase == SpeedTestPhase.Done) "Run Again" else "Start Test")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    active: Boolean = false,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                BirdoGreen.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = value,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (active) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = BirdoGreen,
                )
            }
        }
    }
}
