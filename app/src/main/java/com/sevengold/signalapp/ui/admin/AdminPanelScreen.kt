@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.model.SignalType
import com.sevengold.signalapp.data.model.SubscriptionPackage
import com.sevengold.signalapp.ui.auth.GoldButton
import com.sevengold.signalapp.ui.common.ProfileScreen
import com.sevengold.signalapp.ui.common.AdaptiveAppFrame
import com.sevengold.signalapp.ui.common.SignalListViewModel
import com.sevengold.signalapp.ui.common.rupiah
import com.sevengold.signalapp.ui.theme.DangerRed
import com.sevengold.signalapp.ui.theme.GoldPrimary

private enum class AdminTab(val label: String) {
    PUBLISH("Terbitkan"), SIGNALS("Sinyal"), CODES("Kode"), PACKAGES("Paket"), SUBSCRIPTIONS("Pesanan"), USERS("Pengguna"), REFERRAL("Referal"), PROFILE("Profil")
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
    var rrTarget by remember { mutableStateOf("2") } // target RR default 1:2
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    // RR aktual dihitung live dari Entry/TP/SL yang lagi diisi (buat sanity-check sebelum publish)
    val liveRR = remember(entry, tp, sl) { calculateRR(entry.toDoubleOrNull(), tp.toDoubleOrNull(), sl.toDoubleOrNull()) }

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

        OutlinedTextField(value = entry, onValueChange = { entry = it }, label = { Text("Entry") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = sl, onValueChange = { sl = it }, label = { Text("Stop Loss (SL)") }, modifier = Modifier.fillMaxWidth())
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
                        onValueChange = { rrTarget = it },
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val entryD = entry.toDoubleOrNull()
                        val slD = sl.toDoubleOrNull()
                        val rrD = rrTarget.toDoubleOrNull()
                        val computedTp = calculateTpFromRR(type, entryD, slD, rrD)
                        if (computedTp != null) {
                            tp = "%.2f".format(computedTp)
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

        OutlinedTextField(value = tp, onValueChange = { tp = it }, label = { Text("Take Profit (TP)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (liveRR != null) "RR saat ini: 1 : ${"%.2f".format(liveRR)}" else "RR saat ini: —",
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
                    sl = sl.toDoubleOrNull() ?: 0.0,
                    status = SignalStatus.ACTIVE,
                    note = note,
                    createdBy = adminUid
                )
                vm.publish(signal) { ok, err ->
                    message = if (ok) {
                        entry = ""; tp = ""; sl = ""; note = ""
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
private fun ManageSignalsTab(vm: SignalListViewModel = viewModel()) {
    val signals by vm.signals.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<SignalType?>(null) }
    var statusFilter by remember { mutableStateOf<SignalStatus?>(null) }
    var showAll by remember { mutableStateOf(false) }

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

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Kelola Sinyal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Filter dan kelola sinyal tanpa membuat halaman terlalu panjang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (filteredSignals.size > 8) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Ringkas" else "Lihat semua")
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
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${signal.type} ${signal.pair}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
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
                        }
                    }
                }
            }
        }
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
    var userSearchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, selectedRoleFilter, userSearchQuery) {
        val query = userSearchQuery.trim()
        users
            .let { list ->
                selectedRoleFilter?.let { role -> list.filter { it.effectiveRole == role } } ?: list
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
