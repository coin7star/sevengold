@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.ui.common.PerformanceSummaryCard
import com.sevengold.signalapp.ui.common.ProfileScreen
import com.sevengold.signalapp.ui.common.SignalListViewModel

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
    redeemVm: RedeemViewModel = viewModel()
) {
    val signals by signalVm.signals.collectAsState()
    val redeemState by redeemVm.state.collectAsState()
    var code by remember { mutableStateOf("") }
    var showRedeemSheet by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(UserTab.SIGNALS) }

    LaunchedEffect(redeemState.success) {
        if (redeemState.success) {
            showRedeemSheet = false
            code = ""
            redeemVm.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == UserTab.SIGNALS) "Sinyal XAUUSD" else "Profil") },
                actions = { TextButton(onClick = onLogout) { Text("Keluar") } }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == UserTab.SIGNALS,
                    onClick = { tab = UserTab.SIGNALS },
                    icon = { Icon(Icons.Filled.ShowChart, contentDescription = null) },
                    label = { Text("Sinyal") }
                )
                NavigationBarItem(
                    selected = tab == UserTab.PROFILE,
                    onClick = { tab = UserTab.PROFILE },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profil") }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                UserTab.SIGNALS -> Column(Modifier.fillMaxSize()) {
                    PerformanceSummaryCard(signals = signals, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Kamu belum berlangganan Premium", fontWeight = FontWeight.Bold)
                            Text("Redeem kode dari admin untuk membuka semua sinyal secara penuh.")
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { showRedeemSheet = true }) { Text("Masukkan Kode Langganan") }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(signals) { signal -> LockedSignalCard(signal) }
                    }
                }
                UserTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
            }
        }
    }

    if (showRedeemSheet) {
        ModalBottomSheet(onDismissRequest = { showRedeemSheet = false }) {
            Column(Modifier.padding(20.dp)) {
                Text("Masukkan Kode Langganan", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (redeemState.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(redeemState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { redeemVm.redeem(uid, code) },
                    enabled = !redeemState.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (redeemState.loading) "Memproses..." else "Aktifkan")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LockedSignalCard(signal: Signal) {
    Box {
        Card(Modifier.fillMaxWidth().alpha(0.5f)) {
            Column(Modifier.padding(14.dp)) {
                Text("${signal.type} ${signal.pair}", fontWeight = FontWeight.Bold)
                Text("Entry: •••••")
                Text("TP: •••••")
                Text("SL: •••••")
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Khusus Premium", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
