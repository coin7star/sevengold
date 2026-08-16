@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.model.SignalType
import com.sevengold.signalapp.data.model.SubscriptionPackage
import com.sevengold.signalapp.ui.auth.GoldButton
import com.sevengold.signalapp.ui.common.ProfileScreen
import com.sevengold.signalapp.ui.common.AdaptiveAppFrame
import com.sevengold.signalapp.ui.common.SignalListViewModel
import com.sevengold.signalapp.ui.common.rupiah
import com.sevengold.signalapp.ui.common.StatsPeriod
import com.sevengold.signalapp.ui.common.toPerformanceStats
import com.sevengold.signalapp.ui.theme.DangerRed
import com.sevengold.signalapp.ui.theme.GoldPrimary
import java.util.Locale

private enum class AdminTab(val label: String) {
    PUBLISH("Terbitkan"), SIGNALS("Sinyal"), ANALYTICS("Analytics"), USER_ANALYTICS("User Analytics"), TELEGRAM("Telegram"), CODES("Kode"), PACKAGES("Paket"), SUBSCRIPTIONS("Pesanan"), USERS("Pengguna"), REFERRAL("Referal"), PROFILE("Profil")
}

@Composable
fun AdminPanelScreen(
    adminUid: String,
    user: AppUser,
    onLogout: () -> Unit
) {
    var tab by remember { mutableStateOf(AdminTab.PUBLISH) }
    val subscriptionVm: SubscriptionAdminViewModel = viewModel()
    val pendingOrders by subscriptionVm.orders.collectAsState()
    var drawerOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = GoldPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (tab) {
                                AdminTab.PUBLISH -> "Terbitkan Sinyal"
                                AdminTab.SIGNALS -> "Kelola Sinyal"
                                AdminTab.ANALYTICS -> "Signal Analytics"
                                AdminTab.USER_ANALYTICS -> "User Analytics"
                                AdminTab.TELEGRAM -> "Telegram Control"
                                AdminTab.CODES -> "Kode / Voucher"
                                AdminTab.PACKAGES -> "Paket Langganan"
                                AdminTab.SUBSCRIPTIONS -> "Pesanan"
                                AdminTab.USERS -> "Pengguna"
                                AdminTab.REFERRAL -> "Referral"
                                AdminTab.PROFILE -> "Profil Admin"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { drawerOpen = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Buka menu")
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = "Keluar",
                            tint = DangerRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            AdaptiveAppFrame {
                when (tab) {
                    AdminTab.PUBLISH -> PublishSignalTab(adminUid)
                    AdminTab.SIGNALS -> ManageSignalsTab()
                    AdminTab.ANALYTICS -> AdminSignalAnalyticsTab()
                    AdminTab.USER_ANALYTICS -> AdminUserAnalyticsTab()
                    AdminTab.TELEGRAM -> AdminTelegramTab()
                    AdminTab.CODES -> ManageCodesTab(adminUid)
                    AdminTab.PACKAGES -> SubscriptionPackagesTab()
                    AdminTab.SUBSCRIPTIONS -> ManageSubscriptionOrdersTab(subscriptionVm)
                    AdminTab.USERS -> ManageUsersTab()
                    AdminTab.REFERRAL -> ReferralSettingsTab(adminUid)
                    AdminTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
                }
            }
        }

        if (drawerOpen) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var dragDistance = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                dragDistance += dragAmount
                            },
                            onDragEnd = {
                                if (dragDistance > 80f) {
                                    drawerOpen = false
                                }
                                dragDistance = 0f
                            }
                        )
                    },
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "SEVENGOLD",
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldPrimary
                            )
                            Text(
                                "Panel Administrator",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        IconButton(onClick = { drawerOpen = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup menu")
                        }
                    }

                    AdminDrawerItem(
                        selected = tab == AdminTab.PUBLISH,
                        label = "Terbitkan Sinyal",
                        icon = Icons.Filled.ShowChart,
                        onClick = {
                            tab = AdminTab.PUBLISH
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.SIGNALS,
                        label = "Kelola Sinyal",
                        icon = Icons.Filled.ShowChart,
                        onClick = {
                            tab = AdminTab.SIGNALS
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.ANALYTICS,
                        label = "Signal Analytics",
                        icon = Icons.Filled.ShowChart,
                        onClick = {
                            tab = AdminTab.ANALYTICS
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.USER_ANALYTICS,
                        label = "User Analytics",
                        icon = Icons.Filled.Person,
                        onClick = {
                            tab = AdminTab.USER_ANALYTICS
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.TELEGRAM,
                        label = "Telegram Control",
                        icon = Icons.Filled.Notifications,
                        onClick = {
                            tab = AdminTab.TELEGRAM
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.CODES,
                        label = "Kode / Voucher",
                        icon = Icons.Filled.AdminPanelSettings,
                        onClick = {
                            tab = AdminTab.CODES
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.PACKAGES,
                        label = "Paket Langganan",
                        icon = Icons.Filled.WorkspacePremium,
                        onClick = {
                            tab = AdminTab.PACKAGES
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.SUBSCRIPTIONS,
                        label = if (pendingOrders.isNotEmpty()) {
                            "Pesanan (${pendingOrders.size})"
                        } else {
                            "Pesanan"
                        },
                        icon = Icons.Filled.WorkspacePremium,
                        onClick = {
                            tab = AdminTab.SUBSCRIPTIONS
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.USERS,
                        label = "Pengguna",
                        icon = Icons.Filled.Person,
                        onClick = {
                            tab = AdminTab.USERS
                            drawerOpen = false
                        }
                    )
                    AdminDrawerItem(
                        selected = tab == AdminTab.REFERRAL,
                        label = "Referral",
                        icon = Icons.Filled.AdminPanelSettings,
                        onClick = {
                            tab = AdminTab.REFERRAL
                            drawerOpen = false
                        }
                    )

                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                    NavigationDrawerItem(
                        selected = tab == AdminTab.PROFILE,
                        onClick = {
                            tab = AdminTab.PROFILE
                            drawerOpen = false
                        },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Profil Admin") },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                "Administrator",
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
}

@Composable
private fun AdminDrawerItem(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.AdminPanelSettings,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun PublishSignalTab(adminUid: String, vm: SignalListViewModel = viewModel()) {
    var type by remember { mutableStateOf(SignalType.BUY) }
    var entry by remember { mutableStateOf("") }
    var tp by remember { mutableStateOf("") }
    var sl by remember { mutableStateOf("") }
    var quickSlMode by remember { mutableStateOf(false) }
    var slPips by remember { mutableStateOf("") }
    var rrTarget by remember { mutableStateOf("2") } // target RR default 1:2
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    // Mode cepat: XAUUSD memakai 1$ = 10 pips, jadi 50 pips = jarak harga $5.
    val effectiveSl = if (quickSlMode) {
        calculateSlFromPips(type, entry.toDoubleOrNull(), slPips.toDoubleOrNull())
    } else {
        sl.toDoubleOrNull()
    }

    // RR aktual dihitung live dari Entry/TP/SL yang lagi diisi (buat sanity-check sebelum publish)
    val liveRR = remember(entry, tp, effectiveSl) { calculateRR(entry.toDoubleOrNull(), tp.toDoubleOrNull(), effectiveSl) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Terbitkan Sinyal XAUUSD", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Row {
            FilterChip(selected = type == SignalType.BUY, onClick = { type = SignalType.BUY }, label = { Text("BUY") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = type == SignalType.SELL, onClick = { type = SignalType.SELL }, label = { Text("SELL") })
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = entry,
            onValueChange = { entry = normalizeDecimalInput(it) },
            label = { Text("Entry") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Mode Cepat SL", style = MaterialTheme.typography.labelLarge)
                Text(
                    "1$ = 10 pips • isi pips, harga SL otomatis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = quickSlMode, onCheckedChange = { quickSlMode = it })
        }
        Spacer(Modifier.height(6.dp))

        if (quickSlMode) {
            OutlinedTextField(
                value = slPips,
                onValueChange = { slPips = normalizeDecimalInput(it) },
                label = { Text("Stop Loss (Pips)") },
                suffix = { Text("pips") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            if (effectiveSl != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Harga SL otomatis: ${formatPriceWithDot(effectiveSl)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            OutlinedTextField(
                value = sl,
                onValueChange = { sl = normalizeDecimalInput(it) },
                label = { Text("Stop Loss (SL)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        Spacer(Modifier.height(8.dp))

        // --- Kalkulator RR: isi Entry + SL + target RR, TP otomatis kehitung ---
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Text("Kalkulator RR", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Target RR  1 :", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = rrTarget,
                        onValueChange = { rrTarget = normalizeDecimalInput(it) },
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val entryD = entry.toDoubleOrNull()
                        val slD = effectiveSl
                        val rrD = rrTarget.toDoubleOrNull()
                        val computedTp = calculateTpFromRR(type, entryD, slD, rrD)
                        if (computedTp != null) {
                            tp = formatPriceWithDot(computedTp)
                        }
                    }) {
                        Text("Isi TP")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Isi Entry & SL dulu, lalu tekan \"Isi TP\" — TP otomatis dihitung sesuai target RR di atas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = tp,
            onValueChange = { tp = normalizeDecimalInput(it) },
            label = { Text("Take Profit (TP)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (liveRR != null) "RR saat ini: 1 : ${formatPriceWithDot(liveRR)}" else "RR saat ini: —",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan (opsional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        if (message != null) {
            Text(message ?: "")
            Spacer(Modifier.height(8.dp))
        }

        GoldButton(
            text = "Terbitkan Sinyal",
            loading = false,
            onClick = {
                val signal = Signal(
                    type = type,
                    entry = entry.toDoubleOrNull() ?: 0.0,
                    tp = tp.toDoubleOrNull() ?: 0.0,
                    sl = effectiveSl ?: 0.0,
                    status = SignalStatus.ACTIVE,
                    note = note,
                    createdBy = adminUid
                )
                vm.publish(signal) { ok, err ->
                    message = if (ok) {
                        entry = ""; tp = ""; sl = ""; slPips = ""; note = ""
                        "Sinyal berhasil dipublish"
                    } else {
                        "Gagal: $err"
                    }
                }
            }
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** Normalisasi input angka agar selalu memakai titik sebagai desimal, termasuk pada keyboard locale Indonesia. */
private fun normalizeDecimalInput(value: String): String = value.replace(',', '.')

/** Format harga selalu dengan titik, bukan koma. Contoh 4010.00. */
private fun formatPriceWithDot(value: Double): String = String.format(Locale.US, "%.2f", value)

/** Mode cepat XAUUSD: 1$ = 10 pips, sehingga 1 pip = $0.10. */
private fun calculateSlFromPips(type: SignalType, entry: Double?, pips: Double?): Double? {
    if (entry == null || pips == null || pips <= 0.0) return null
    val priceDistance = pips / 10.0
    return when (type) {
        SignalType.BUY -> entry - priceDistance
        SignalType.SELL -> entry + priceDistance
    }
}

/** Hitung RR aktual (reward/risk) dari Entry/TP/SL yang sudah diisi. Null kalau datanya belum lengkap/valid. */
private fun calculateRR(entry: Double?, tp: Double?, sl: Double?): Double? {
    if (entry == null || tp == null || sl == null) return null
    val risk = kotlin.math.abs(entry - sl)
    if (risk <= 0.0) return null
    val reward = kotlin.math.abs(tp - entry)
    return reward / risk
}

/** Hitung TP otomatis dari Entry + SL + target RR, sesuai arah BUY/SELL. */
private fun calculateTpFromRR(type: SignalType, entry: Double?, sl: Double?, rr: Double?): Double? {
    if (entry == null || sl == null || rr == null || rr <= 0.0) return null
    val risk = kotlin.math.abs(entry - sl)
    if (risk <= 0.0) return null
    val rewardDistance = risk * rr
    return when (type) {
        SignalType.BUY -> entry + rewardDistance
        SignalType.SELL -> entry - rewardDistance
    }
}


@Composable
private fun AdminTelegramTab(vm: AdminViewModel = viewModel()) {
    val result by vm.telegramResult.collectAsState()
    val message by vm.telegramMessage.collectAsState()
    val loading by vm.telegramLoading.collectAsState()
    var showBroadcastConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.telegramAction("stats") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Telegram Control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Kelola dan uji notifikasi Telegram Premium tanpa harus mengirim sinyal sungguhan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Status Telegram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelegramStatCard("Premium", result.premium, Modifier.weight(1f))
                    TelegramStatCard("Terhubung", result.connected, Modifier.weight(1f))
                    TelegramStatCard("Belum terhubung", result.disconnected, Modifier.weight(1f))
                }
                OutlinedButton(
                    onClick = { vm.telegramAction("stats") },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Perbarui Statistik") }
            }
        }

        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pengujian Notifikasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Gunakan Test My Telegram untuk memastikan bot dan webhook bekerja. Test Premium Broadcast hanya dikirim ke akun Premium yang sudah terhubung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { vm.telegramAction("testme") },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🧪 Test My Telegram") }
                Button(
                    onClick = { showBroadcastConfirm = true },
                    enabled = !loading && result.connected > 0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🚨 Test Premium Broadcast") }
            }
        }

        if (message != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(message.orEmpty(), Modifier.padding(14.dp))
            }
        }

        if (result.sent > 0 || result.failed > 0) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Hasil Pengujian Terakhir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Terkirim: ${result.sent}")
                    Text("Gagal: ${result.failed}")
                }
            }
        }
    }

    if (showBroadcastConfirm) {
        AlertDialog(
            onDismissRequest = { showBroadcastConfirm = false },
            title = { Text("Kirim Test Broadcast?") },
            text = { Text("Test signal akan dikirim ke ${result.connected} akun Premium yang sudah terhubung ke Telegram. Ini hanya notifikasi pengujian, bukan sinyal trading nyata.") },
            confirmButton = {
                TextButton(onClick = { showBroadcastConfirm = false; vm.telegramAction("testsignal") }) { Text("Kirim") }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun TelegramStatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GoldPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AdminUserAnalyticsTab(
    userVm: UserManagementViewModel = viewModel()
) {
    val users by userVm.users.collectAsState()
    val stats = remember(users) { calculateUserRegistrationStats(users) }
    var growthDays by remember { mutableStateOf(7) }

    val premiumCount = remember(users) {
        users.count { it.role == Role.PREMIUM }
    }
    val freeCount = (users.size - premiumCount).coerceAtLeast(0)
    val conversion = if (users.isEmpty()) 0.0 else (premiumCount.toDouble() / users.size.toDouble()) * 100.0

    val growthData = remember(users, growthDays) {
        calculateDailyUserGrowth(users, growthDays)
    }
    val maxGrowth = maxOf(1, growthData.maxOfOrNull { it.second } ?: 0)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("User Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Pantau pertumbuhan user, distribusi Premium, dan conversion rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsMetricCard("Total User", users.size.toString(), Modifier.weight(1f))
                AnalyticsMetricCard("Premium", premiumCount.toString(), Modifier.weight(1f))
                AnalyticsMetricCard("Free", freeCount.toString(), Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Premium Conversion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${formatAnalytics(conversion)}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrimary
                    )
                    LinearProgressIndicator(
                        progress = { (conversion / 100.0).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(9.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "$premiumCount Premium dari ${users.size} total user",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("User Growth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Pendaftaran per hari berdasarkan createdAt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = growthDays == 7,
                                onClick = { growthDays = 7 },
                                label = { Text("7D") }
                            )
                            FilterChip(
                                selected = growthDays == 30,
                                onClick = { growthDays = 30 },
                                label = { Text("30D") }
                            )
                        }
                    }

                    if (growthData.all { it.second == 0 }) {
                        Text(
                            "Belum ada pendaftaran pada periode ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        growthData.forEach { (label, count) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, modifier = Modifier.width(if (growthDays == 7) 42.dp else 48.dp), style = MaterialTheme.typography.labelSmall)
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / maxGrowth.toFloat() },
                                    modifier = Modifier.weight(1f).height(10.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    count.toString(),
                                    modifier = Modifier.width(32.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Premium vs Free", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AnalyticsRow("💎 Premium", premiumCount.toString())
                    AnalyticsRow("🆓 Free", freeCount.toString())
                    AnalyticsRow("Conversion", "${formatAnalytics(conversion)}%")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Registration Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AnalyticsRow("Hari ini", stats.today.toString())
                    AnalyticsRow("7 hari terakhir", stats.last7Days.toString())
                    AnalyticsRow("30 hari terakhir", stats.last30Days.toString())
                }
            }
        }

        item {
            Text(
                "Perhitungan mengikuti waktu lokal perangkat admin. Premium dihitung dari role = PREMIUM.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calculateDailyUserGrowth(
    users: List<com.sevengold.signalapp.data.model.AppUser>,
    days: Int
): List<Pair<String, Int>> {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)

    val start = calendar.clone() as java.util.Calendar
    start.add(java.util.Calendar.DAY_OF_YEAR, -(days - 1))

    return (0 until days).map { offset ->
        val day = start.clone() as java.util.Calendar
        day.add(java.util.Calendar.DAY_OF_YEAR, offset)

        val nextDay = day.clone() as java.util.Calendar
        nextDay.add(java.util.Calendar.DAY_OF_YEAR, 1)

        val count = users.count {
            it.createdAt >= day.timeInMillis && it.createdAt < nextDay.timeInMillis
        }

        val label = if (days == 7) {
            java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(day.time)
        } else {
            java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(day.time)
        }

        label to count
    }
}

@Composable
private fun AdminSignalAnalyticsTab(
    vm: SignalListViewModel = viewModel(),
    userVm: UserManagementViewModel = viewModel()
) {
    val signals by vm.signals.collectAsState()
    val users by userVm.users.collectAsState()
    var period by remember { mutableStateOf(StatsPeriod.DAILY) }

    val userRegistrationStats = remember(users) {
        calculateUserRegistrationStats(users)
    }
    val stats = remember(signals, period) {
        signals.toPerformanceStats(period)
    }

    val active = remember(signals) { signals.count { it.status == SignalStatus.ACTIVE } }
    val tp = remember(signals) { signals.count { it.status == SignalStatus.TP_HIT } }
    val sl = remember(signals) { signals.count { it.status == SignalStatus.SL_HIT } }
    val be = remember(signals) { signals.count { it.status == SignalStatus.BE } }
    val cancelled = remember(signals) { signals.count { it.status == SignalStatus.CANCELLED } }
    val buy = remember(signals) { signals.count { it.type == SignalType.BUY } }
    val sell = remember(signals) { signals.count { it.type == SignalType.SELL } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Signal Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pantau performa sinyal dan hasil trading dari panel admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    com.sevengold.signalapp.ui.common.PeriodSelectorForAdmin(
                        selected = period,
                        onSelect = { period = it }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "User Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Jumlah pendaftaran berdasarkan waktu lokal perangkat admin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            users.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyticsMetricCard(
                            "Hari Ini",
                            userRegistrationStats.today.toString(),
                            Modifier.weight(1f)
                        )
                        AnalyticsMetricCard(
                            "7 Hari",
                            userRegistrationStats.last7Days.toString(),
                            Modifier.weight(1f)
                        )
                        AnalyticsMetricCard(
                            "30 Hari",
                            userRegistrationStats.last30Days.toString(),
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsMetricCard("Win Rate", "${formatAnalytics(stats.winRatePercent)}%", Modifier.weight(1f))
                AnalyticsMetricCard("Total Pip", "${if (stats.totalPips >= 0) "+" else ""}${formatAnalytics(stats.totalPips)}", Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsMetricCard("Menang", tp.toString(), Modifier.weight(1f))
                AnalyticsMetricCard("Kalah", sl.toString(), Modifier.weight(1f))
                AnalyticsMetricCard("BE", be.toString(), Modifier.weight(1f))
            }
        }

        item {
            Text("Status Sinyal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsMetricCard("🟢 Aktif", active.toString(), Modifier.weight(1f))
                AnalyticsMetricCard("❌ Cancel", cancelled.toString(), Modifier.weight(1f))
            }
        }

        item {
            Text("Arah Sinyal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("BUY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(buy.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SELL", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(sell.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ringkasan Periode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    AnalyticsRow("Closed", stats.totalSignals.toString())
                    AnalyticsRow("TP Hit", stats.wins.toString())
                    AnalyticsRow("SL Hit", stats.losses.toString())
                    AnalyticsRow("Win Rate", "${formatAnalytics(stats.winRatePercent)}%")
                    AnalyticsRow("Net Pip", "${if (stats.totalPips >= 0) "+" else ""}${formatAnalytics(stats.totalPips)} pip")
                }
            }
        }

        item {
            Text(
                "Catatan: statistik ini menggunakan sinyal yang sedang tersedia di aplikasi. Sinyal yang sudah dihapus dari Firebase tidak ikut dihitung.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class UserRegistrationStats(
    val today: Int,
    val last7Days: Int,
    val last30Days: Int
)

private fun calculateUserRegistrationStats(
    users: List<com.sevengold.signalapp.data.model.AppUser>
): UserRegistrationStats {
    val now = System.currentTimeMillis()
    val dayMillis = 24L * 60L * 60L * 1000L

    // Calendar-day boundaries follow the admin device's local timezone.
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = now
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)

    val startOfToday = calendar.timeInMillis
    val startOf7Days = now - (7L * dayMillis)
    val startOf30Days = now - (30L * dayMillis)

    var today = 0
    var last7Days = 0
    var last30Days = 0

    users.forEach { user ->
        val createdAt = user.createdAt
        if (createdAt >= startOfToday) today++
        if (createdAt >= startOf7Days) last7Days++
        if (createdAt >= startOf30Days) last30Days++
    }

    return UserRegistrationStats(
        today = today,
        last7Days = last7Days,
        last30Days = last30Days
    )
}

@Composable
private fun AnalyticsMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = GoldPrimary)
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatAnalytics(value: Double): String = String.format(Locale("id", "ID"), "%.1f", value)

@Composable
private fun ManageSignalsTab(vm: SignalListViewModel = viewModel()) {
    val signals by vm.signals.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<SignalType?>(null) }
    var statusFilter by remember { mutableStateOf<SignalStatus?>(null) }
    var showAll by remember { mutableStateOf(false) }
    var signalToDelete by remember { mutableStateOf<Signal?>(null) }
    var showClearCancelledDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var selectedSignalIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    val cancelledCount = remember(signals) { signals.count { it.status == SignalStatus.CANCELLED } }

    val filteredSignals = remember(signals, searchQuery, typeFilter, statusFilter) {
        val query = searchQuery.trim().lowercase()
        signals
            .filter { signal ->
                val matchesQuery = query.isBlank() || listOf(
                    signal.pair,
                    signal.type.name,
                    signal.status.name,
                    signal.note
                ).any { it.lowercase().contains(query) }
                val matchesType = typeFilter == null || signal.type == typeFilter
                val matchesStatus = statusFilter == null || signal.status == statusFilter
                matchesQuery && matchesType && matchesStatus
            }
            .sortedByDescending { it.createdAt }
    }

    val visibleSignals = if (showAll) filteredSignals else filteredSignals.take(8)
    val deletableVisibleSignals = visibleSignals.filter {
        it.status == SignalStatus.TP_HIT ||
            it.status == SignalStatus.SL_HIT ||
            it.status == SignalStatus.BE
    }
    val selectedVisibleSignals = deletableVisibleSignals.filter { it.id in selectedSignalIds }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Responsive header: action buttons move below the title on narrow screens/DPI.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactHeader = maxWidth < 600.dp

            if (compactHeader) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Kelola Sinyal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Filter dan kelola sinyal tanpa membuat halaman terlalu panjang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        if (selectedVisibleSignals.isNotEmpty()) {
                            item {
                                TextButton(
                                    onClick = { showDeleteSelectedDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Hapus ${selectedVisibleSignals.size}")
                                }
                            }
                        }
                        if (deletableVisibleSignals.isNotEmpty()) {
                            item {
                                TextButton(
                                    onClick = {
                                        val allSelected = deletableVisibleSignals.all { it.id in selectedSignalIds }
                                        selectedSignalIds = if (allSelected) {
                                            selectedSignalIds - deletableVisibleSignals.map { it.id }.toSet()
                                        } else {
                                            selectedSignalIds + deletableVisibleSignals.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Text(if (deletableVisibleSignals.all { it.id in selectedSignalIds }) "Batal ceklis" else "Ceklis semua")
                                }
                            }
                        }
                        if (cancelledCount > 0) {
                            item {
                                TextButton(
                                    onClick = { showClearCancelledDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Bersihkan Batal")
                                }
                            }
                        }
                        if (filteredSignals.size > 8) {
                            item {
                                TextButton(onClick = { showAll = !showAll }) {
                                    Text(if (showAll) "Ringkas" else "Lihat semua")
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Kelola Sinyal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Filter dan kelola sinyal tanpa membuat halaman terlalu panjang.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (selectedVisibleSignals.isNotEmpty()) {
                            TextButton(
                                onClick = { showDeleteSelectedDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Hapus ${selectedVisibleSignals.size}")
                            }
                        }
                        if (deletableVisibleSignals.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val allSelected = deletableVisibleSignals.all { it.id in selectedSignalIds }
                                    selectedSignalIds = if (allSelected) {
                                        selectedSignalIds - deletableVisibleSignals.map { it.id }.toSet()
                                    } else {
                                        selectedSignalIds + deletableVisibleSignals.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(if (deletableVisibleSignals.all { it.id in selectedSignalIds }) "Batal ceklis" else "Ceklis semua")
                            }
                        }
                        if (cancelledCount > 0) {
                            TextButton(
                                onClick = { showClearCancelledDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Bersihkan Batal")
                            }
                        }
                        if (filteredSignals.size > 8) {
                            TextButton(onClick = { showAll = !showAll }) {
                                Text(if (showAll) "Ringkas" else "Lihat semua")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari sinyal") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian")
                    }
                }
            },
            placeholder = { Text("Cari pair, arah, status, atau catatan") },
            label = { Text("Pencarian") }
        )

        Spacer(Modifier.height(6.dp))

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            item {
                FilterChip(
                    selected = typeFilter == null,
                    onClick = { typeFilter = null },
                    label = { Text("Semua arah") }
                )
            }
            item {
                FilterChip(
                    selected = typeFilter == SignalType.BUY,
                    onClick = { typeFilter = if (typeFilter == SignalType.BUY) null else SignalType.BUY },
                    label = { Text("BUY") }
                )
            }
            item {
                FilterChip(
                    selected = typeFilter == SignalType.SELL,
                    onClick = { typeFilter = if (typeFilter == SignalType.SELL) null else SignalType.SELL },
                    label = { Text("SELL") }
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Semua status") }
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == SignalStatus.ACTIVE,
                    onClick = { statusFilter = if (statusFilter == SignalStatus.ACTIVE) null else SignalStatus.ACTIVE },
                    label = { Text("Aktif") }
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == SignalStatus.TP_HIT,
                    onClick = { statusFilter = if (statusFilter == SignalStatus.TP_HIT) null else SignalStatus.TP_HIT },
                    label = { Text("TP Hit") }
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == SignalStatus.SL_HIT,
                    onClick = { statusFilter = if (statusFilter == SignalStatus.SL_HIT) null else SignalStatus.SL_HIT },
                    label = { Text("SL Hit") }
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == SignalStatus.BE,
                    onClick = { statusFilter = if (statusFilter == SignalStatus.BE) null else SignalStatus.BE },
                    label = { Text("BE") }
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == SignalStatus.CANCELLED,
                    onClick = { statusFilter = if (statusFilter == SignalStatus.CANCELLED) null else SignalStatus.CANCELLED },
                    label = { Text("Batal") }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            "Menampilkan ${visibleSignals.size} dari ${filteredSignals.size} sinyal",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        actionMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(message, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                com.sevengold.signalapp.ui.common.PerformanceSummaryCard(
                    signals = filteredSignals,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (visibleSignals.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Tidak ada sinyal yang sesuai", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Coba ubah kata kunci atau filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(visibleSignals, key = { it.id }) { signal ->
                val canDeleteBySelection = signal.status == SignalStatus.TP_HIT ||
                    signal.status == SignalStatus.SL_HIT ||
                    signal.status == SignalStatus.BE
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canDeleteBySelection) {
                                    Checkbox(
                                        checked = signal.id in selectedSignalIds,
                                        onCheckedChange = { checked ->
                                            selectedSignalIds = if (checked) selectedSignalIds + signal.id else selectedSignalIds - signal.id
                                        }
                                    )
                                }
                                Text(
                                    "${signal.type} ${signal.pair}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(adminSignalStatusLabel(signal.status)) }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Entry: ${signal.entry}  •  TP: ${signal.tp}  •  SL: ${signal.sl}")
                        if (signal.note.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                signal.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(onClick = { vm.updateStatus(signal, SignalStatus.BE) }) { Text("BE") }
                            TextButton(onClick = { vm.updateStatus(signal, SignalStatus.CANCELLED) }) { Text("Batal") }
                            TextButton(onClick = { vm.updateStatus(signal, SignalStatus.TP_HIT) }) { Text("TP Hit") }
                            TextButton(onClick = { vm.updateStatus(signal, SignalStatus.SL_HIT) }) { Text("SL Hit") }
                            if (signal.status == SignalStatus.CANCELLED) {
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { signalToDelete = signal }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus sinyal", tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    signalToDelete?.let { signal ->
        AlertDialog(
            onDismissRequest = { signalToDelete = null },
            title = { Text("Hapus sinyal?") },
            text = { Text("Sinyal ${signal.type} ${signal.pair} yang berstatus Batal akan dihapus permanen dari database Firebase. Tindakan ini tidak bisa dibatalkan.") },
            dismissButton = { TextButton(onClick = { signalToDelete = null }) { Text("Batal") } },
            confirmButton = {
                Button(
                    onClick = {
                        val target = signalToDelete
                        signalToDelete = null
                        if (target != null) {
                            vm.deleteSignal(target) { success, error ->
                                actionMessage = if (success) "Sinyal ${target.type} ${target.pair} berhasil dihapus dari Firebase." else "Gagal menghapus sinyal: ${error ?: "tidak diketahui"}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Hapus permanen") }
            }
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("Hapus sinyal terpilih?") },
            text = { Text("Sebanyak ${selectedVisibleSignals.size} sinyal TP/SL/BE yang diceklis akan dihapus permanen dari Firebase. Sinyal Batal tetap aman dan tidak ikut terhapus.") },
            dismissButton = { TextButton(onClick = { showDeleteSelectedDialog = false }) { Text("Batal") } },
            confirmButton = {
                Button(
                    onClick = {
                        val targets = selectedVisibleSignals.toList()
                        showDeleteSelectedDialog = false
                        if (targets.isNotEmpty()) {
                            vm.deleteSignals(targets) { success, deleted, error ->
                                if (success) {
                                    selectedSignalIds = selectedSignalIds - targets.map { it.id }.toSet()
                                    actionMessage = "Berhasil menghapus $deleted sinyal TP/SL/BE dari Firebase."
                                } else {
                                    actionMessage = "Gagal menghapus sinyal terpilih: ${error ?: "tidak diketahui"}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Hapus permanen") }
            }
        )
    }

    if (showClearCancelledDialog) {
        AlertDialog(
            onDismissRequest = { showClearCancelledDialog = false },
            title = { Text("Bersihkan semua sinyal Batal?") },
            text = { Text("Sebanyak $cancelledCount sinyal Batal akan dihapus permanen dari Firebase. Sinyal Aktif, TP, SL, dan BE tidak akan ikut terhapus.") },
            dismissButton = { TextButton(onClick = { showClearCancelledDialog = false }) { Text("Jangan") } },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCancelledDialog = false
                        vm.deleteCancelledSignals { success, deleted, error ->
                            actionMessage = if (success) {
                                if (deleted > 0) "Berhasil menghapus $deleted sinyal Batal dari Firebase." else "Tidak ada sinyal Batal yang perlu dihapus."
                            } else "Gagal membersihkan sinyal Batal: ${error ?: "tidak diketahui"}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Bersihkan") }
            }
        )
    }
}

private fun adminSignalStatusLabel(status: SignalStatus): String = when (status) {
    SignalStatus.ACTIVE -> "Aktif"
    SignalStatus.BE -> "BE"
    SignalStatus.CANCELLED -> "Dibatalkan"
    SignalStatus.TP_HIT -> "TP Hit"
    SignalStatus.SL_HIT -> "SL Hit"
}

@Composable
private fun ManageCodesTab(adminUid: String, vm: AdminViewModel = viewModel()) {
    val codes by vm.codes.collectAsState()
    val lastCode by vm.lastGeneratedCode.collectAsState()
    var duration by remember { mutableStateOf("30") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Generate Kode Langganan", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Durasi (hari)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.generateCode(duration.toIntOrNull() ?: 30, adminUid) }) {
                Text("Buat")
            }
        }

        if (lastCode != null) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp)
            ) {
                Text("KODE BARU", style = MaterialTheme.typography.labelMedium, color = GoldPrimary)
                Spacer(Modifier.height(2.dp))
                Text("$lastCode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GoldPrimary)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Riwayat Kode", style = MaterialTheme.typography.titleSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(codes) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${c.code} — ${c.durationDays} hari")
                        Text(if (c.isUsed) "Sudah dipakai" else "Belum dipakai")
                    }
                }
            }
        }
    }
}

/**
 * Panel User Management: admin bisa lihat semua akun (email, role, expiry premium)
 * dan langsung naik/turunin role USER <-> PREMIUM tanpa perlu generate kode dulu.
 */

@Composable
private fun SubscriptionPackagesTab(vm: AdminViewModel = viewModel()) {
    val packages by vm.subscriptionPackages.collectAsState()
    val message by vm.packageMessage.collectAsState()
    var editing by remember { mutableStateOf<SubscriptionPackage?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Paket Langganan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Ubah harga, durasi, nama, label, dan status paket langsung dari panel administrator. Perubahan berlaku untuk pembelian baru; pesanan lama tetap menggunakan harga dan durasi yang tersimpan saat pembayaran.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { creating = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("➕ Tambah Paket")
            }
        }

        if (message != null) {
            Text(
                message ?: "",
                color = if ((message ?: "").startsWith("Gagal")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (packages.isEmpty()) {
            Text("Belum ada paket.")
        }

        packages.sortedBy { it.sortOrder }.forEach { pkg ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(pkg.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${rupiah(pkg.price)} • ${pkg.durationDays} hari${if (pkg.label.isNotBlank()) " • ${pkg.label}" else ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        AssistChip(
                            onClick = {
                                val updated = packages.map {
                                    if (it.id == pkg.id) it.copy(enabled = !it.enabled) else it
                                }
                                vm.saveSubscriptionPackages(updated)
                            },
                            label = { Text(if (pkg.enabled) "AKTIF" else "NONAKTIF") }
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editing = pkg }) {
                            Text("Ubah")
                        }
                        TextButton(
                            onClick = {
                                if (packages.size <= 1) {
                                    // saveSubscriptionPackages akan menolak keadaan kosong; minimal
                                    // satu paket dipertahankan agar halaman pembelian tidak kosong.
                                    vm.saveSubscriptionPackages(packages)
                                } else {
                                    vm.saveSubscriptionPackages(
                                        packages.filterNot { it.id == pkg.id }
                                    )
                                }
                            }
                        ) {
                            Text("Hapus", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (editing != null) {
        PackageEditorDialog(
            initial = editing!!,
            title = "Ubah Paket",
            onDismiss = { editing = null },
            onSave = { updated ->
                vm.saveSubscriptionPackages(
                    packages.map { if (it.id == updated.id) updated else it }
                )
                editing = null
            }
        )
    }

    if (creating) {
        val newPackage = remember {
            SubscriptionPackage(
                id = "pkg_${System.currentTimeMillis()}",
                name = "",
                price = 0L,
                durationDays = 0,
                label = "",
                enabled = true,
                sortOrder = packages.size
            )
        }
        PackageEditorDialog(
            initial = newPackage,
            title = "Tambah Paket",
            onDismiss = { creating = false },
            onSave = { created ->
                vm.saveSubscriptionPackages(packages + created)
                creating = false
            }
        )
    }
}

@Composable
private fun PackageEditorDialog(
    initial: SubscriptionPackage,
    title: String,
    onDismiss: () -> Unit,
    onSave: (SubscriptionPackage) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var price by remember(initial.id) { mutableStateOf(if (initial.price > 0) initial.price.toString() else "") }
    var days by remember(initial.id) { mutableStateOf(if (initial.durationDays > 0) initial.durationDays.toString() else "") }
    var label by remember(initial.id) { mutableStateOf(initial.label) }
    var enabled by remember(initial.id) { mutableStateOf(initial.enabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama paket") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter(Char::isDigit) },
                    label = { Text("Harga (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it.filter(Char::isDigit) },
                    label = { Text("Durasi (hari)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (opsional)") },
                    placeholder = { Text("BEST VALUE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Paket aktif")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            price = price.toLongOrNull() ?: 0L,
                            durationDays = days.toIntOrNull() ?: 0,
                            label = label.trim(),
                            enabled = enabled
                        )
                    )
                }
            ) {
                Text("Simpan")
            }
        }
    )
}

@Composable
private fun ManageSubscriptionOrdersTab(vm: SubscriptionAdminViewModel) {
    val orders by vm.orders.collectAsState()
    val message by vm.message.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 420.dp

        Column(Modifier.fillMaxSize()) {
            Text(
                "Persetujuan Langganan Manual",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Pengguna dan member Premium membuat pesanan dari paket. Setelah pembayaran diverifikasi, setujui pesanan di sini. Setelah disetujui, akun otomatis diaktifkan sebagai Premium dan durasinya ditambahkan.",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!message.isNullOrBlank()) {
                Text(
                    message ?: "",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada pesanan yang menunggu persetujuan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                Modifier.padding(if (compact) 14.dp else 18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (compact) {
                                    Text(
                                        order.packageName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        rupiah(order.price),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                } else {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            order.packageName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            rupiah(order.price),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary
                                        )
                                    }
                                }

                                Text(
                                    "${order.email.ifBlank { order.uid }} • +${order.durationDays} hari",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (order.discountPercent > 0) {
                                    Text(
                                        "Harga ${rupiah(order.originalPrice)} → diskon ${order.discountPercent}% → bayar ${rupiah(order.price)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        "Total bayar: ${rupiah(order.price)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    "Order: ${order.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Status: MENUNGGU PEMBAYARAN / PERSETUJUAN",
                                    style = MaterialTheme.typography.labelSmall
                                )

                                if (compact) {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { vm.approve(order.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Setujui") }
                                        OutlinedButton(
                                            onClick = { vm.reject(order.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Tolak") }
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(onClick = { vm.approve(order.id) }) { Text("Setujui") }
                                        OutlinedButton(onClick = { vm.reject(order.id) }) { Text("Tolak") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferralSettingsTab(adminUid: String, vm: AdminViewModel = viewModel()) {
    val settings by vm.referralSettings.collectAsState()
    val message by vm.settingsMessage.collectAsState()
    var rewardDays by remember(settings.rewardPremiumDays) { mutableStateOf(settings.rewardPremiumDays.toString()) }
    var voucherPercent by remember(settings.welcomeVoucherPercent) { mutableStateOf(settings.welcomeVoucherPercent.toString()) }
    var enabled by remember(settings.enabled) { mutableStateOf(settings.enabled) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pengaturan Referal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Atur bonus referal langsung dari panel administrator. Perubahan berlaku untuk referal baru dan bonus berikutnya.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Program referal", fontWeight = FontWeight.SemiBold)
                        Text("Aktif/nonaktif referal", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                OutlinedTextField(
                    value = rewardDays,
                    onValueChange = { rewardDays = it.filter(Char::isDigit) },
                    label = { Text("Bonus Premium untuk pemberi referal (hari)") },
                    supportingText = { Text("Contoh: 2 = +2 hari Premium") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = voucherPercent,
                    onValueChange = { voucherPercent = it.filter(Char::isDigit) },
                    label = { Text("Voucher selamat datang (%)") },
                    supportingText = { Text("Contoh: 10 = diskon 10% untuk teman baru") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val currentMessage = message
                if (currentMessage != null) {
                    Text(
                        currentMessage,
                        color = if (currentMessage.startsWith("Gagal")) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }

                GoldButton(
                    text = "Simpan Pengaturan Referal",
                    loading = false,
                    onClick = {
                        vm.updateReferralSettings(
                            rewardDays = rewardDays.toIntOrNull() ?: settings.rewardPremiumDays,
                            voucherPercent = voucherPercent.toIntOrNull() ?: settings.welcomeVoucherPercent,
                            enabled = enabled
                        )
                    }
                )
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Preview", fontWeight = FontWeight.SemiBold)
                Text("Teman mendaftar dengan referal → voucher ${settings.welcomeVoucherPercent}%")
                Text("Teman berhasil berlangganan → pemberi referal +${settings.rewardPremiumDays} hari Premium")
                Text("Status: ${if (settings.enabled) "Aktif" else "Nonaktif"}")
            }
        }
    }
}

@Composable
private fun ManageUsersTab(vm: UserManagementViewModel = viewModel()) {
    val users by vm.users.collectAsState()
    val actionMessage by vm.actionMessage.collectAsState()

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            kotlinx.coroutines.delay(2000)
            vm.clearMessage()
        }
    }

    var selectedRoleFilter by remember { mutableStateOf<com.sevengold.signalapp.data.model.Role?>(null) }
    var telegramFilter by remember { mutableStateOf<Boolean?>(null) }
    var userSearchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, selectedRoleFilter, telegramFilter, userSearchQuery) {
        val query = userSearchQuery.trim()
        users
            .let { list ->
                selectedRoleFilter?.let { role -> list.filter { it.effectiveRole == role } } ?: list
            }
            .let { list ->
                telegramFilter?.let { connected -> list.filter { it.telegramConnected == connected } } ?: list
            }
            .filter { u ->
                query.isBlank() ||
                    u.email.contains(query, ignoreCase = true) ||
                    u.uid.contains(query, ignoreCase = true)
            }
    }

    Column(Modifier.fillMaxSize()) {
        if (actionMessage != null) {
            Text(
                actionMessage ?: "",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Filter role + pencarian email/UID agar Admin lebih mudah menemukan user tertentu.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Cari Pengguna",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = userSearchQuery,
                onValueChange = { userSearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Cari berdasarkan email atau UID...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Cari pengguna"
                    )
                },
                trailingIcon = {
                    if (userSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { userSearchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Hapus pencarian"
                            )
                        }
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Filter Telegram",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = telegramFilter == null,
                        onClick = { telegramFilter = null },
                        label = { Text("Semua Telegram") }
                    )
                }
                item {
                    FilterChip(
                        selected = telegramFilter == true,
                        onClick = { telegramFilter = true },
                        label = { Text("🟢 Terhubung (${users.count { it.telegramConnected }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = telegramFilter == false,
                        onClick = { telegramFilter = false },
                        label = { Text("⚪ Belum Terhubung (${users.count { !it.telegramConnected }})") }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Filter Peran",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedRoleFilter == null,
                        onClick = { selectedRoleFilter = null },
                        label = { Text("Semua (${users.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedRoleFilter == com.sevengold.signalapp.data.model.Role.USER,
                        onClick = { selectedRoleFilter = com.sevengold.signalapp.data.model.Role.USER },
                        label = { Text("PENGGUNA (${users.count { it.effectiveRole == com.sevengold.signalapp.data.model.Role.USER }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedRoleFilter == com.sevengold.signalapp.data.model.Role.PREMIUM,
                        onClick = { selectedRoleFilter = com.sevengold.signalapp.data.model.Role.PREMIUM },
                        label = { Text("PREMIUM (${users.count { it.effectiveRole == com.sevengold.signalapp.data.model.Role.PREMIUM }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedRoleFilter == com.sevengold.signalapp.data.model.Role.ADMIN,
                        onClick = { selectedRoleFilter = com.sevengold.signalapp.data.model.Role.ADMIN },
                        label = { Text("ADMINISTRATOR (${users.count { it.effectiveRole == com.sevengold.signalapp.data.model.Role.ADMIN }})") }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminUserStatCard("Total", users.size.toString(), Modifier.weight(1f))
            AdminUserStatCard("Premium", users.count { it.effectiveRole == com.sevengold.signalapp.data.model.Role.PREMIUM }.toString(), Modifier.weight(1f))
            AdminUserStatCard("Telegram", users.count { it.telegramConnected }.toString(), Modifier.weight(1f))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    if (userSearchQuery.isBlank()) {
                        "${filteredUsers.size} pengguna ditampilkan"
                    } else {
                        "${filteredUsers.size} pengguna ditemukan dari ${users.size} pengguna"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (filteredUsers.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Pengguna tidak ditemukan",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Periksa kembali email atau UID yang Anda masukkan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredUsers,
                    key = { it.uid }
                ) { u ->
                    UserRow(u, vm)
                }
            }
        }
    }
}

@Composable
private fun AdminUserStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UserRow(user: com.sevengold.signalapp.data.model.AppUser, vm: UserManagementViewModel) {
    var showPremiumInput by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf("30") }
    val df = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        user.email.ifBlank { "Email tidak tersedia" },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "UID: ${user.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                RoleBadge(user.effectiveRole)
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (user.telegramConnected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (user.telegramConnected) "🟢 Telegram Terhubung" else "⚪ Telegram Belum Terhubung",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (user.telegramConnected && !user.telegramUsername.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "@${user.telegramUsername!!.removePrefix("@")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (user.telegramConnectedAt != null) {
                Text(
                    "Terhubung: ${df.format(java.util.Date(user.telegramConnectedAt!!))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (user.role == com.sevengold.signalapp.data.model.Role.PREMIUM) {
                val expiryText = user.premiumExpiryMillis?.let { df.format(java.util.Date(it)) } ?: "-"
                Text(
                    if (user.isPremiumActive) "Premium berlaku sampai: $expiryText" else "Premium telah berakhir: $expiryText",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))

            if (user.effectiveRole != com.sevengold.signalapp.data.model.Role.ADMIN) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (user.effectiveRole == com.sevengold.signalapp.data.model.Role.PREMIUM) {
                        TextButton(onClick = { vm.setUser(user.uid) }) { Text("Ubah ke Pengguna") }
                    } else {
                        TextButton(onClick = { showPremiumInput = !showPremiumInput }) { Text("Aktifkan Premium") }
                    }
                }

                if (showPremiumInput) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = days,
                            onValueChange = { days = it },
                            label = { Text("Durasi (hari)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            vm.setPremium(user.uid, days.toIntOrNull() ?: 30)
                            showPremiumInput = false
                        }) {
                            Text("Konfirmasi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: com.sevengold.signalapp.data.model.Role) {
    val (label, color) = when (role) {
        com.sevengold.signalapp.data.model.Role.ADMIN -> "ADMIN" to MaterialTheme.colorScheme.error
        com.sevengold.signalapp.data.model.Role.PREMIUM -> "PREMIUM" to MaterialTheme.colorScheme.primary
        com.sevengold.signalapp.data.model.Role.USER -> "USER" to MaterialTheme.colorScheme.outline
    }
    AssistChip(onClick = {}, label = { Text(label) }, colors = AssistChipDefaults.assistChipColors(labelColor = color))
}
