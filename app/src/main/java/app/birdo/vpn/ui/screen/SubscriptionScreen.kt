package app.birdo.vpn.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.birdo.vpn.data.model.SubscriptionStatus
import app.birdo.vpn.ui.theme.*

private data class PlanInfo(
    val id: String,
    val name: String,
    val tagline: String,
    val priceMonthly: String,
    val priceYearly: String,
    val features: List<String>,
    val accent: Color,
    val isPopular: Boolean = false,
)

private val plans = listOf(
    PlanInfo(
        id = "RECON",
        name = "Recon",
        tagline = "Test the waters",
        priceMonthly = "Free",
        priceYearly = "Free",
        features = listOf(
            "1 device connection",
            "2 server locations",
            "10 GB monthly bandwidth",
            "WireGuard\u00ae encryption",
            "Post-quantum encryption",
            "Kill switch",
            "DNS leak protection",
        ),
        accent = BirdoWhite40,
    ),
    PlanInfo(
        id = "OPERATIVE",
        name = "Operative",
        tagline = "Most popular",
        priceMonthly = "$5/mo",
        priceYearly = "$48/yr",
        features = listOf(
            "3 device connections",
            "All server locations",
            "Unlimited bandwidth",
            "WireGuard\u00ae encryption",
            "Post-quantum encryption",
            "Kill switch",
            "Split tunneling",
            "Stealth mode",
            "Speed test",
            "2FA / TOTP",
            "Biometric lock",
            "Priority support",
        ),
        accent = Color(0xFF8B5CF6),
        isPopular = true,
    ),
    PlanInfo(
        id = "SOVEREIGN",
        name = "Sovereign",
        tagline = "Full control",
        priceMonthly = "$12/mo",
        priceYearly = "$99/yr",
        features = listOf(
            "Unlimited devices",
            "All server locations",
            "Unlimited bandwidth",
            "WireGuard\u00ae encryption",
            "Post-quantum encryption",
            "Kill switch",
            "Split tunneling",
            "Stealth mode",
            "Multi-hop routing",
            "Port forwarding",
            "Speed test",
            "2FA / TOTP",
            "Biometric lock",
            "Custom DNS",
            "Priority support",
        ),
        accent = Color(0xFFF59E0B),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    currentSubscription: SubscriptionStatus?,
    onNavigateBack: () -> Unit,
    onSelectPlan: (planId: String) -> Unit,
    onManageOnWeb: () -> Unit,
) {
    var billingPeriod by remember { mutableStateOf("yearly") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Choose Your Plan",
                        fontWeight = FontWeight.Bold,
                        color = BirdoWhite80,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = BirdoWhite60)
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Current plan badge
            if (currentSubscription != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = BirdoSurface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.CreditCard, "Plan", tint = BirdoWhite60, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Current Plan: ${currentSubscription.plan}",
                                style = MaterialTheme.typography.titleSmall,
                                color = BirdoWhite80,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${currentSubscription.activeConnections}/${currentSubscription.maxConnections} devices used",
                                style = MaterialTheme.typography.bodySmall,
                                color = BirdoWhite40,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Billing period toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = BirdoSurface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                ) {
                    listOf("monthly" to "Monthly", "yearly" to "Yearly (Save 20%)").forEach { (key, label) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (billingPeriod == key) Color.White else Color.Transparent,
                            onClick = { billingPeriod = key },
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (billingPeriod == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (billingPeriod == key) Color.Black else BirdoWhite60,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Plan cards
            plans.forEach { plan ->
                val isCurrent = currentSubscription?.plan?.equals(plan.id, ignoreCase = true) == true
                PlanCard(
                    plan = plan,
                    isCurrent = isCurrent,
                    price = if (billingPeriod == "yearly") plan.priceYearly else plan.priceMonthly,
                    onSelect = { onSelectPlan(plan.id) },
                )
                Spacer(Modifier.height(12.dp))
            }

            // Manage on web link
            TextButton(
                onClick = onManageOnWeb,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Manage subscription on birdo.app", color = BirdoWhite40)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlanCard(
    plan: PlanInfo,
    isCurrent: Boolean,
    price: String,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderModifier = if (plan.isPopular) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(plan.accent, plan.accent.copy(alpha = 0.3f))),
            shape = shape,
        )
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = shape,
        color = BirdoSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            plan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BirdoWhite80,
                        )
                        if (plan.isPopular) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = plan.accent.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    "POPULAR",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = plan.accent,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        if (isCurrent) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BirdoGreen.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    "CURRENT",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BirdoGreen,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(plan.tagline, style = MaterialTheme.typography.bodySmall, color = BirdoWhite40)
                }
                Text(
                    price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (plan.id == "RECON") BirdoWhite60 else Color.White,
                )
            }

            Spacer(Modifier.height(16.dp))

            plan.features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = plan.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = BirdoWhite60,
                    )
                }
            }

            if (!isCurrent && plan.id != "RECON") {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (plan.isPopular) Color.White else BirdoWhite10,
                        contentColor = if (plan.isPopular) Color.Black else Color.White,
                    ),
                ) {
                    Text(
                        "Upgrade to ${plan.name}",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
