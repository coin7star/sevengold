@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.model.SignalType
import com.sevengold.signalapp.ui.common.ProfileScreen
import com.sevengold.signalapp.ui.common.SignalListViewModel

private enum class AdminTab { PUBLISH, SIGNALS, CODES, USERS, PROFILE }

@Composable
fun AdminPanelScreen(
    adminUid: String,
    user: AppUser,
    onLogout: () -> Unit
) {
    var tab by remember { mutableStateOf(AdminTab.PUBLISH) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Admin Panel") },
            actions = {
                TextButton(onClick = onLogout) { Text("Keluar") }
            }
        )

        TabRow(selectedTabIndex = tab.ordinal) {
            Tab(selected = tab == AdminTab.PUBLISH, onClick = { tab = AdminTab.PUBLISH }, text = { Text("Publish") })
            Tab(selected = tab == AdminTab.SIGNALS, onClick = { tab = AdminTab.SIGNALS }, text = { Text("Sinyal") })
            Tab(selected = tab == AdminTab.CODES, onClick = { tab = AdminTab.CODES }, text = { Text("Kode") })
            Tab(selected = tab == AdminTab.USERS, onClick = { tab = AdminTab.USERS }, text = { Text("Users") })
            Tab(selected = tab == AdminTab.PROFILE, onClick = { tab = AdminTab.PROFILE }, text = { Text("Profil") })
        }

        when (tab) {
            AdminTab.PUBLISH -> PublishSignalTab(adminUid)
            AdminTab.SIGNALS -> ManageSignalsTab()
            AdminTab.CODES -> ManageCodesTab(adminUid)
            AdminTab.USERS -> ManageUsersTab()
            AdminTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
        }
    }
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
        Text("Publish Sinyal XAUUSD", style = MaterialTheme.typography.titleMedium)
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

        Button(
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
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Publish Sinyal")
        }
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            com.sevengold.signalapp.ui.common.PerformanceSummaryCard(
                signals = signals,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(signals) { signal ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${signal.type} ${signal.pair} — ${signal.status}", style = MaterialTheme.typography.titleSmall)
                    Text("Entry: ${signal.entry}  TP: ${signal.tp}  SL: ${signal.sl}")
                    if (signal.note.isNotBlank()) Text(signal.note)

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { vm.updateStatus(signal.id, SignalStatus.BE) }) { Text("Set BE") }
                        TextButton(onClick = { vm.updateStatus(signal.id, SignalStatus.CANCELLED) }) { Text("Cancel") }
                        TextButton(onClick = { vm.updateStatus(signal.id, SignalStatus.TP_HIT) }) { Text("TP Hit") }
                        TextButton(onClick = { vm.updateStatus(signal.id, SignalStatus.SL_HIT) }) { Text("SL Hit") }
                    }
                }
            }
        }
    }
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
            Text("Kode baru: $lastCode", style = MaterialTheme.typography.titleLarge)
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
private fun ManageUsersTab(vm: UserManagementViewModel = viewModel()) {
    val users by vm.users.collectAsState()
    val actionMessage by vm.actionMessage.collectAsState()

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            kotlinx.coroutines.delay(2000)
            vm.clearMessage()
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { u -> UserRow(u, vm) }
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
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(user.email.ifBlank { user.uid }, style = MaterialTheme.typography.titleSmall)
                RoleBadge(user.effectiveRole)
            }
            if (user.role == com.sevengold.signalapp.data.model.Role.PREMIUM) {
                val expiryText = user.premiumExpiryMillis?.let { df.format(java.util.Date(it)) } ?: "-"
                Text(
                    if (user.isPremiumActive) "Premium sampai: $expiryText" else "Premium expired: $expiryText",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))

            if (user.effectiveRole != com.sevengold.signalapp.data.model.Role.ADMIN) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (user.effectiveRole == com.sevengold.signalapp.data.model.Role.PREMIUM) {
                        TextButton(onClick = { vm.setUser(user.uid) }) { Text("Turunkan ke USER") }
                    } else {
                        TextButton(onClick = { showPremiumInput = !showPremiumInput }) { Text("Jadikan PREMIUM") }
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
