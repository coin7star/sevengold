@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.ui.common.PerformanceSummaryCard
import com.sevengold.signalapp.ui.common.ProfileScreen
import com.sevengold.signalapp.ui.common.SignalListViewModel
import com.sevengold.signalapp.ui.common.SubscriptionBanner
import com.sevengold.signalapp.ui.common.SubscriptionPurchaseSheet
import com.sevengold.signalapp.ui.common.SubscriptionViewModel
import com.sevengold.signalapp.ui.theme.GoldLight
import com.sevengold.signalapp.ui.theme.GoldPrimary
import com.sevengold.signalapp.ui.theme.SignalGradients

private enum class UserTab { SIGNALS, PROFILE }

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

    LaunchedEffect(uid) { subscriptionVm.startListeningOrders(uid) }

    LaunchedEffect(redeemState.success) {
        if (redeemState.success) {
            showRedeemSheet = false
            code = ""
            redeemVm.reset()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (tab == UserTab.SIGNALS) "Sinyal XAUUSD" else "Profil",
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
            when (tab) {
                UserTab.SIGNALS -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
                ) {
                    item { PerformanceSummaryCard(signals = signals) }
                    item { SubscriptionBanner(user, packages, onBuy = { showPackagesSheet = true }) }
                    item { if (orders.any { it.status.name == "PENDING" }) PendingOrderBanner() }
                    item { UpsellCard(onUpgrade = { showRedeemSheet = true }) }
                    items(signals) { signal -> LockedSignalCard(signal) }
                }
                UserTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
            }
        }
    }

    if (showPackagesSheet) {
        SubscriptionPurchaseSheet(
            user = user,
            packages = packages,
            orders = orders,
            message = subscriptionMessage,
            onBuy = { pkg -> subscriptionVm.createOrder(uid, user.email, pkg) },
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
}

@Composable
private fun PendingOrderBanner() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text("⏳ Pesanan Premium menunggu approval", fontWeight = FontWeight.Bold)
            Text("Setelah pembayaran dikonfirmasi admin, akun akan otomatis menjadi PREMIUM.", style = MaterialTheme.typography.bodySmall)
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
                "Kamu belum berlangganan Premium",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF241A02),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Pilih paket Premium di atas untuk mulai berlangganan. Kode lama dari admin tetap bisa dipakai sebagai opsi cadangan.",
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

@Composable
private fun LockedSignalCard(signal: Signal) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .blur(6.dp)
        ) {
            Text("${signal.type} ${signal.pair}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Entry: 2 3•4.5•", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("TP: 23•6.••", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("SL: 23•2.••", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier.matchParentSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SignalGradients.premiumBadge)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF241A02), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Khusus Premium", style = MaterialTheme.typography.labelMedium, color = Color(0xFF241A02), fontWeight = FontWeight.Bold)
            }
        }
    }
}
