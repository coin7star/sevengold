@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.model.SignalType
import com.sevengold.signalapp.ui.common.PerformanceSummaryCard
import com.sevengold.signalapp.ui.common.ProfileScreen
import com.sevengold.signalapp.ui.common.SignalListViewModel
import com.sevengold.signalapp.ui.common.SubscriptionBanner
import com.sevengold.signalapp.ui.common.SubscriptionPurchaseSheet
import com.sevengold.signalapp.ui.common.SubscriptionViewModel
import com.sevengold.signalapp.ui.theme.EmeraldAccent
import com.sevengold.signalapp.ui.theme.GoldLight
import com.sevengold.signalapp.ui.theme.GoldPrimary
import com.sevengold.signalapp.ui.theme.SignalGradients
import java.text.SimpleDateFormat
import java.util.*

private enum class PremiumTab { SIGNALS, PROFILE }

@Composable
fun PremiumSignalScreen(
    user: AppUser,
    expiryLabel: String,
    onLogout: () -> Unit,
    vm: SignalListViewModel = viewModel(),
    subscriptionVm: SubscriptionViewModel = viewModel()
) {
    val signals by vm.signals.collectAsState()
    val packages by subscriptionVm.packages.collectAsState()
    val orders by subscriptionVm.orders.collectAsState()
    val subscriptionMessage by subscriptionVm.message.collectAsState()
    var tab by remember { mutableStateOf(PremiumTab.SIGNALS) }
    var showPackagesSheet by remember { mutableStateOf(false) }
    LaunchedEffect(user.uid) { subscriptionVm.startListeningOrders(user.uid) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (tab == PremiumTab.SIGNALS) "Sinyal XAUUSD" else "Profil",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = { TextButton(onClick = onLogout) { Text("Keluar", color = MaterialTheme.colorScheme.error) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == PremiumTab.SIGNALS,
                    onClick = { tab = PremiumTab.SIGNALS },
                    icon = { Icon(Icons.Filled.ShowChart, contentDescription = null) },
                    label = { Text("Sinyal") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldPrimary,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = Color(0x33D4AF62)
                    )
                )
                NavigationBarItem(
                    selected = tab == PremiumTab.PROFILE,
                    onClick = { tab = PremiumTab.PROFILE },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profil") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldPrimary,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = Color(0x33D4AF62)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                PremiumTab.SIGNALS -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
                ) {
                    item { PremiumExpiryBanner(expiryLabel) }
                    item { SubscriptionBanner(user, packages, onBuy = { showPackagesSheet = true }, premium = true) }
                    if (orders.any { it.status.name == "PENDING" }) item { PendingRenewalBanner() }
                    item { PerformanceSummaryCard(signals = signals) }
                    items(signals) { signal -> SignalCard(signal) }
                }
                PremiumTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
            }
        }
    }

    if (showPackagesSheet) {
        SubscriptionPurchaseSheet(
            user = user,
            packages = packages,
            orders = orders,
            message = subscriptionMessage,
            onBuy = { pkg -> subscriptionVm.createOrder(user.uid, user.email, pkg) },
            onDismiss = { showPackagesSheet = false }
        )
    }
}

@Composable
private fun PendingRenewalBanner() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text("⏳ Perpanjangan menunggu approval", fontWeight = FontWeight.Bold)
            Text("Durasi Premium akan ditambahkan setelah admin menyetujui pembayaran.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PremiumExpiryBanner(expiryLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SignalGradients.premiumBadge)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color(0xFF241A02))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Member Premium Aktif", color = Color(0xFF241A02), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text("Berlaku sampai $expiryLabel", color = Color(0xFF3A2E10), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SignalCard(signal: Signal) {
    val df = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")) }
    val isBuy = signal.type == SignalType.BUY

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(if (isBuy) EmeraldAccent else Color(0xFFE5657A))
        )
        Column(Modifier.padding(16.dp).weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBuy) Color(0x263FBF8F) else Color(0x26E5657A))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            signal.type.name,
                            color = if (isBuy) EmeraldAccent else Color(0xFFE5657A),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(signal.pair, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                StatusBadge(signal.status)
            }
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PriceStat("Entry", signal.entry, MaterialTheme.colorScheme.onSurface)
                PriceStat("TP", signal.tp, EmeraldAccent)
                PriceStat("SL", signal.sl, Color(0xFFE5657A))
            }

            if (signal.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    signal.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                df.format(Date(signal.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriceStat(label: String, value: Double, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun StatusBadge(status: SignalStatus) {
    val (label, color) = when (status) {
        SignalStatus.ACTIVE -> "ACTIVE" to GoldLight
        SignalStatus.BE -> "BREAK EVEN" to Color(0xFF8FA6D6)
        SignalStatus.CANCELLED -> "CANCELLED" to Color(0xFF8B94A8)
        SignalStatus.TP_HIT -> "TP HIT" to EmeraldAccent
        SignalStatus.SL_HIT -> "SL HIT" to Color(0xFFE5657A)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
