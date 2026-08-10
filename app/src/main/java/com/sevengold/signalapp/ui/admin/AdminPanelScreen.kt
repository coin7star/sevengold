@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.model.SignalType
import com.sevengold.signalapp.ui.common.SignalListViewModel

private enum class AdminTab { PUBLISH, SIGNALS, CODES }

@Composable
fun AdminPanelScreen(
    adminUid: String,
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
        }

        when (tab) {
            AdminTab.PUBLISH -> PublishSignalTab(adminUid)
            AdminTab.SIGNALS -> ManageSignalsTab()
            AdminTab.CODES -> ManageCodesTab(adminUid)
        }
    }
}

@Composable
private fun PublishSignalTab(adminUid: String, vm: SignalListViewModel = viewModel()) {
    var type by remember { mutableStateOf(SignalType.BUY) }
    var entry by remember { mutableStateOf("") }
    var tp by remember { mutableStateOf("") }
    var sl by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
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
        OutlinedTextField(value = tp, onValueChange = { tp = it }, label = { Text("Take Profit (TP)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = sl, onValueChange = { sl = it }, label = { Text("Stop Loss (SL)") }, modifier = Modifier.fillMaxWidth())
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

@Composable
private fun ManageSignalsTab(vm: SignalListViewModel = viewModel()) {
    val signals by vm.signals.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                        TextButton(onClick = { vm.updateStatus(signal.id, SignalStatus.CLOSED) }) { Text("Closed") }
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
