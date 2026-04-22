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
import app.birdo.vpn.data.model.RedeemVoucherResponse
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
    onRedeemVoucher: ((code: String, onResult: (RedeemVoucherResponse?) -> Unit) -> Unit)? = null,
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

            // Voucher redemption (Mullvad-style time-extension codes)
            if (onRedeemVoucher != null) {
                Spacer(Modifier.height(8.dp))
                VoucherRedeemSection(onRedeem = onRedeemVoucher)
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

/**
 * Voucher redemption section — collapsed by default, expands to show a
 * code-entry field. Mirrors the Mullvad-style "Redeem voucher" UX:
 * codes are 30- or 90-day time extensions to the user's current
 * subscription. Backend endpoint POST /vouchers/redeem (NestJS).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoucherRedeemSection(
    onRedeem: (code: String, onResult: (RedeemVoucherResponse?) -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultIsSuccess by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BirdoSurface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = BirdoWhite60, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Redeem voucher",
                        style = MaterialTheme.typography.titleSmall,
                        color = BirdoWhite80,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Enter a 30- or 90-day code to extend your subscription.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BirdoWhite40,
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Open", color = BirdoWhite80)
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { newValue ->
                        // Auto-uppercase and limit length; keep dashes user typed.
                        code = newValue.uppercase().take(24)
                        resultMessage = null
                    },
                    label = { Text("BIRD-XXXX-XXXX-XXXX") },
                    singleLine = true,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        submitting = true
                        resultMessage = null
                        onRedeem(code.trim()) { response ->
                            submitting = false
                            if (response == null) {
                                resultIsSuccess = false
                                resultMessage = "Network error. Try again."
                            } else if (response.ok) {
                                resultIsSuccess = true
                                resultMessage = buildString {
                                    append("Added ")
                                    append(response.durationDays)
                                    append(" days. ")
                                    if (response.extended) append("Subscription extended.")
                                    else append("Plan upgraded to ${response.plan}.")
                                }
                                code = ""
                            } else {
                                resultIsSuccess = false
                                resultMessage = when (response.error) {
                                    "invalid_format" -> "Invalid code format. Should be BIRD-XXXX-XXXX-XXXX."
                                    "not_found" -> "Voucher not recognised."
                                    "already_redeemed" -> "Voucher already used."
                                    "expired" -> "Voucher has expired."
                                    "plan_downgrade" -> "Your plan is already higher than this voucher offers."
                                    else -> "Couldn't redeem voucher. Please try again."
                                }
                            }
                        }
                    },
                    enabled = !submitting && code.length >= 8,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Redeem")
                    }
                }
                resultMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (resultIsSuccess) BirdoGreen else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
