@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.user

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.ui.common.PerformanceSummaryCard
import com.sevengold.signalapp.ui.common.AdaptiveAppFrame
import com.sevengold.signalapp.ui.common.CompactSignalHistorySection
import com.sevengold.signalapp.ui.common.NotificationCenterScreen
import com.sevengold.signalapp.ui.common.LockedSignalCard
import com.sevengold.signalapp.ui.common.ProfileScreen
import com.sevengold.signalapp.ui.common.SignalListViewModel
import com.sevengold.signalapp.ui.common.SubscriptionBanner
import com.sevengold.signalapp.ui.common.SubscriptionPurchaseSheet
import com.sevengold.signalapp.ui.common.SubscriptionViewModel
import com.sevengold.signalapp.ui.theme.GoldLight
import com.sevengold.signalapp.ui.theme.GoldPrimary
import com.sevengold.signalapp.ui.theme.SignalGradients

private enum class UserTab { SIGNALS, NOTIFICATIONS, PROFILE }

/**
 * Ditampilkan untuk role USER (belum / tidak lagi berlangganan).
 * Angka-angka penting disamarkan jadi "•••" + overlay kunci, supaya
 * tetap terlihat ADA sinyal (memancing untuk upgrade) tanpa membocorkan datanya.
 */
@Composable
fun UserSignalScreen(
    uid: String,
    user: AppUser,
    onLogout: () -> Unit,
    signalVm: SignalListViewModel = viewModel(),
    redeemVm: RedeemViewModel = viewModel(),
    subscriptionVm: SubscriptionViewModel = viewModel()
) {
    val signals by signalVm.signals.collectAsState()
    val redeemState by redeemVm.state.collectAsState()
    val packages by subscriptionVm.packages.collectAsState()
    val orders by subscriptionVm.orders.collectAsState()
    val subscriptionMessage by subscriptionVm.message.collectAsState()
    var code by remember { mutableStateOf("") }
    var showRedeemSheet by remember { mutableStateOf(false) }
    var showPackagesSheet by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(UserTab.SIGNALS) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch { drawerState.close() }
    }

    LaunchedEffect(uid) { subscriptionVm.startListeningOrders(uid) }

    LaunchedEffect(redeemState.success) {
        if (redeemState.success) {
            showRedeemSheet = false
            code = ""
            redeemVm.reset()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SEVENGOLD", fontWeight = FontWeight.ExtraBold, color = GoldPrimary)
                            Text("Menu aplikasi", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { drawerScope.launch { drawerState.close() } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup menu")
                        }
                    }
                    NavigationDrawerItem(
                        selected = tab == UserTab.SIGNALS,
                        onClick = { tab = UserTab.SIGNALS; drawerScope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Filled.ShowChart, null) },
                        label = { Text("Sinyal") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        selected = tab == UserTab.NOTIFICATIONS,
                        onClick = { tab = UserTab.NOTIFICATIONS; drawerScope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Filled.Notifications, null) },
                        label = { Text("Notifikasi") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        selected = tab == UserTab.PROFILE,
                        onClick = { tab = UserTab.PROFILE; drawerScope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Filled.Person, null) },
                        label = { Text("Profil") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(user.email, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(if (user.effectiveRole == com.sevengold.signalapp.data.model.Role.PREMIUM) "Premium" else "Pengguna", color = GoldPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, contentDescription = "Keluar") }
                    }
                }
            }
        },
        content = {
        Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Buka menu")
                    }
                },
                title = {
                    Text(
                        when (tab) { UserTab.SIGNALS -> "Sinyal XAUUSD"; UserTab.NOTIFICATIONS -> "Notifikasi"; UserTab.PROFILE -> "Profil" },
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
                    selected = tab == UserTab.SIGNALS,
                    onClick = { tab = UserTab.SIGNALS },
                    icon = { Icon(Icons.Filled.ShowChart, contentDescription = null) },
                    label = { Text("Sinyal") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldPrimary,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = Color(0x33D4AF62)
                    )
                )
                NavigationBarItem(
                    selected = tab == UserTab.NOTIFICATIONS,
                    onClick = { tab = UserTab.NOTIFICATIONS },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    label = { Text("Notifikasi") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldPrimary,
                        selectedTextColor = GoldPrimary,
                        indicatorColor = Color(0x33D4AF62)
                    )
                )
                NavigationBarItem(
                    selected = tab == UserTab.PROFILE,
                    onClick = { tab = UserTab.PROFILE },
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
                UserTab.SIGNALS -> {
                    val activeSignals = remember(signals) {
                        signals.filter { it.status == com.sevengold.signalapp.data.model.SignalStatus.ACTIVE }
                    }
                    val historySignals = remember(signals) {
                        signals.filter { it.status != com.sevengold.signalapp.data.model.SignalStatus.ACTIVE }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
                    ) {
                        item {
                            ActiveSignalsSection(
                                signals = activeSignals,
                                locked = true
                            )
                        }
                        item { PerformanceSummaryCard(signals = signals) }
                        item { SubscriptionBanner(user, packages, onBuy = { showPackagesSheet = true }) }
                        item { if (orders.any { it.status.name == "PENDING" }) PendingOrderBanner() }
                        item { UpsellCard(onUpgrade = { showRedeemSheet = true }) }
                        item { HistorySignalsSection(signals = historySignals, locked = true) }
                    }
                }
                    UserTab.NOTIFICATIONS -> NotificationCenterScreen(Modifier.fillMaxSize())
                UserTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
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
            onBuy = { pkg, voucher -> subscriptionVm.createOrder(uid, user.email, pkg, voucher) },
            onDismiss = { showPackagesSheet = false }
        )
    }

    if (showRedeemSheet) {
        ModalBottomSheet(onDismissRequest = { showRedeemSheet = false }) {
            Column(Modifier.padding(20.dp)) {
                Text("Masukkan Kode Langganan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kode") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (redeemState.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(redeemState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                com.sevengold.signalapp.ui.auth.GoldButton(
                    text = if (redeemState.loading) "Memproses..." else "Aktifkan",
                    loading = redeemState.loading,
                    onClick = { redeemVm.redeem(uid, code) }
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    })
}
@Composable
private fun ActiveSignalsSection(
    signals: List<Signal>,
    locked: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sinyal Aktif",
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
                    "Belum ada sinyal aktif.",
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
                            if (locked) LockedSignalCard(signal)
                        }
                    }
                }
            }
            if (signals.size > 1) {
                Text(
                    "Geser untuk melihat sinyal aktif lainnya →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistorySignalsSection(
    signals: List<Signal>,
    locked: Boolean
) {
    CompactSignalHistorySection(signals = signals, locked = locked) { signal -> LockedSignalCard(signal) }
}

@Composable
private fun PendingOrderBanner() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text("⏳ Pesanan Premium Menunggu Persetujuan", fontWeight = FontWeight.Bold)
            Text("Setelah pembayaran dikonfirmasi administrator, akun akan otomatis diaktifkan sebagai Premium.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun UpsellCard(onUpgrade: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFFD4AF62))
            .clip(RoundedCornerShape(20.dp))
            .background(SignalGradients.goldButton)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color(0xFF241A02))
            Spacer(Modifier.width(8.dp))
            Text(
                "Anda Belum Berlangganan Premium",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF241A02),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Pilih paket Premium untuk mulai berlangganan. Kode langganan dari administrator tetap dapat digunakan sebagai alternatif.",
            color = Color(0xFF3A2E10),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF241A02))
        ) {
            TextButton(onClick = onUpgrade) {
                Text("Punya kode lama? Masukkan kode", color = GoldLight, fontWeight = FontWeight.Bold)
            }
        }
    }
}

