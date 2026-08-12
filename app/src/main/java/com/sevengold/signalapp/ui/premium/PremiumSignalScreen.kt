@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.premium

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.model.SignalType
import com.sevengold.signalapp.ui.common.PerformanceSummaryCard
import com.sevengold.signalapp.ui.common.AdaptiveAppFrame
import com.sevengold.signalapp.ui.common.CompactSignalHistorySection
import com.sevengold.signalapp.ui.common.NotificationCenterScreen
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

private enum class PremiumTab { SIGNALS, NOTIFICATIONS, PROFILE }

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
    var drawerOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }
    var showPackagesSheet by remember { mutableStateOf(false) }
    LaunchedEffect(user.uid) { subscriptionVm.startListeningOrders(user.uid) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { drawerOpen = true }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Buka menu")
                        }
                    },
                    title = {
                        Text(
                            when (tab) { PremiumTab.SIGNALS -> "XAUUSD"; PremiumTab.NOTIFICATIONS -> "Notifikasi"; PremiumTab.PROFILE -> "Profil" },
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
                        selected = tab == PremiumTab.NOTIFICATIONS,
                        onClick = { tab = PremiumTab.NOTIFICATIONS },
                        icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                        label = { Text("Notifikasi") },
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
                AdaptiveAppFrame {
                    when (tab) {
                        PremiumTab.SIGNALS -> {
                            val activeSignals = remember(signals) {
                                signals.filter { it.status == SignalStatus.ACTIVE }
                            }
                            val historySignals = remember(signals) {
                                signals.filter { it.status != SignalStatus.ACTIVE }
                            }

                            BoxWithConstraints(Modifier.fillMaxSize()) {
                                val sidePadding = if (maxWidth < 360.dp) 12.dp else 16.dp
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = sidePadding),
                                    verticalArrangement = Arrangement.spacedBy(if (maxWidth < 360.dp) 10.dp else 12.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
                                ) {
                                item { ActiveSignalsSection(signals = activeSignals) }
                                item { PremiumExpiryBanner(expiryLabel) }
                                item { SubscriptionBanner(user, packages, onBuy = { showPackagesSheet = true }, premium = true) }
                                if (orders.any { it.status.name == "PENDING" }) item { PendingRenewalBanner() }
                                item { PerformanceSummaryCard(signals = signals) }
                                    item { HistorySignalsSection(signals = historySignals) }
                                }
                            }
                        }
                        PremiumTab.NOTIFICATIONS -> NotificationCenterScreen(Modifier.fillMaxSize())
                        PremiumTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
                    }
                }
            }
        }

        if (drawerOpen) {
            // Scrim dibuat full-screen dan berada di atas seluruh konten,
            // sehingga tap di luar drawer maupun tombol X selalu menutup drawer.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable { drawerOpen = false }
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.84f)
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                    .shadow(14.dp, RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxHeight()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("SEVENGOLD", fontWeight = FontWeight.ExtraBold, color = GoldPrimary)
                            Text("Menu", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { drawerOpen = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup menu")
                        }
                    }
                    NavigationDrawerItem(
                        selected = tab == PremiumTab.SIGNALS,
                        onClick = { tab = PremiumTab.SIGNALS; drawerOpen = false },
                        icon = { Icon(Icons.Filled.ShowChart, null) },
                        label = { Text("Sinyal") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        selected = tab == PremiumTab.NOTIFICATIONS,
                        onClick = { tab = PremiumTab.NOTIFICATIONS; drawerOpen = false },
                        icon = { Icon(Icons.Filled.Notifications, null) },
                        label = { Text("Notifikasi") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        selected = tab == PremiumTab.PROFILE,
                        onClick = { tab = PremiumTab.PROFILE; drawerOpen = false },
                        icon = { Icon(Icons.Filled.Person, null) },
                        label = { Text("Profil") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                user.email,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (user.effectiveRole == com.sevengold.signalapp.data.model.Role.PREMIUM) "Premium" else "Pengguna",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Filled.Logout, contentDescription = "Keluar")
                        }
                    }
                }
            }
        }
    }
    if (showPackagesSheet) {
        SubscriptionPurchaseSheet(
            user = user,
            packages = packages,
            orders = orders,
            message = subscriptionMessage,
            onBuy = { pkg, voucher -> subscriptionVm.createOrder(user.uid, user.email, pkg, voucher) },
            onDismiss = { showPackagesSheet = false }
        )
    }
}
@Composable
private fun ActiveSignalsSection(signals: List<Signal>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sinyal aktif",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            if (signals.isNotEmpty()) {
                Text(
                    "${signals.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (signals.isEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    "Belum ada sinyal.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cardWidth = (maxWidth * 0.86f).coerceIn(280.dp, 420.dp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(signals, key = { it.id }) { signal ->
                        Box(Modifier.width(cardWidth)) {
                            SignalCard(signal)
                        }
                    }
                }
            }
            if (signals.size > 1) {
                Text(
                    "Geser untuk sinyal lain →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistorySignalsSection(signals: List<Signal>) {
    CompactSignalHistorySection(signals = signals) { signal -> SignalCard(signal) }
}

@Composable
private fun PendingRenewalBanner() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text("⏳ Perpanjangan diproses", fontWeight = FontWeight.Bold)
            Text("Masa Premium bertambah setelah pembayaran disetujui.", style = MaterialTheme.typography.bodySmall)
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
            Text("Premium aktif", color = Color(0xFF241A02), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text("Aktif sampai $expiryLabel", color = Color(0xFF3A2E10), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SignalCard(signal: Signal) {
    val df = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")) }
    val isBuy = signal.type == SignalType.BUY

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 330.dp
            val padding = if (compact) 12.dp else 16.dp

            Column(Modifier.padding(padding)) {
                // Header dibuat adaptif: pada layar sempit status turun ke baris kedua,
                // sehingga BUY/XAUUSD tidak pernah bertabrakan dengan badge status.
                if (compact) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SignalTypeBadge(isBuy = isBuy, label = signal.type.name)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            signal.pair,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusBadge(status = signal.status, compact = true)
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SignalTypeBadge(isBuy = isBuy, label = signal.type.name)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                signal.pair,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        StatusBadge(status = signal.status, compact = false)
                    }
                }

                Spacer(Modifier.height(if (compact) 12.dp else 14.dp))

                // Tiga kolom selalu mendapat lebar yang sama. Ini mencegah harga panjang
                // mendorong kolom lain keluar layar pada density/DPI yang berbeda.
                Row(Modifier.fillMaxWidth()) {
                    PriceStat("Entry", signal.entry, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    PriceStat("SL", signal.sl, Color(0xFFE5657A), Modifier.weight(1f))
                    PriceStat("TP", signal.tp, EmeraldAccent, Modifier.weight(1f))
                }

                if (signal.note.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        signal.note,
                        maxLines = if (compact) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
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
}

@Composable
private fun SignalTypeBadge(isBuy: Boolean, label: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isBuy) Color(0x263FBF8F) else Color(0x26E5657A))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            color = if (isBuy) EmeraldAccent else Color(0xFFE5657A),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun PriceStat(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            String.format(Locale.US, "%.2f", value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusBadge(status: SignalStatus, compact: Boolean = false) {
    val (label, color) = when (status) {
        SignalStatus.ACTIVE -> "AKTIF" to GoldLight
        SignalStatus.BE -> "IMPAS" to Color(0xFF8FA6D6)
        SignalStatus.CANCELLED -> "DIBATALKAN" to Color(0xFF8B94A8)
        SignalStatus.TP_HIT -> "TP TERCAPAI" to EmeraldAccent
        SignalStatus.SL_HIT -> "SL TERCAPAI" to Color(0xFFE5657A)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
