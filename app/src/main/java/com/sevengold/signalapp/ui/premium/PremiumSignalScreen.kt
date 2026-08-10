@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.premium

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.ui.common.PerformanceSummaryCard
import com.sevengold.signalapp.ui.common.ProfileScreen
import com.sevengold.signalapp.ui.common.SignalListViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class PremiumTab { SIGNALS, PROFILE }

@Composable
fun PremiumSignalScreen(
    user: AppUser,
    expiryLabel: String,
    onLogout: () -> Unit,
    vm: SignalListViewModel = viewModel()
) {
    val signals by vm.signals.collectAsState()
    var tab by remember { mutableStateOf(PremiumTab.SIGNALS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == PremiumTab.SIGNALS) "Sinyal XAUUSD" else "Profil") },
                actions = { TextButton(onClick = onLogout) { Text("Keluar") } }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == PremiumTab.SIGNALS,
                    onClick = { tab = PremiumTab.SIGNALS },
                    icon = { Icon(Icons.Filled.ShowChart, contentDescription = null) },
                    label = { Text("Sinyal") }
                )
                NavigationBarItem(
                    selected = tab == PremiumTab.PROFILE,
                    onClick = { tab = PremiumTab.PROFILE },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profil") }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                PremiumTab.SIGNALS -> Column(Modifier.fillMaxSize()) {
                    Text(
                        "Premium aktif sampai: $expiryLabel",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            PerformanceSummaryCard(signals = signals, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                        }
                        items(signals) { signal -> SignalCard(signal) }
                    }
                }
                PremiumTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
            }
        }
    }
}

@Composable
fun SignalCard(signal: Signal) {
    val df = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${signal.type} ${signal.pair}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                StatusBadge(signal.status)
            }
            Spacer(Modifier.height(6.dp))
            Text("Entry: ${signal.entry}")
            Text("TP: ${signal.tp}")
            Text("SL: ${signal.sl}")
            if (signal.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(signal.note, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(df.format(Date(signal.createdAt)), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun StatusBadge(status: SignalStatus) {
    val (label, color) = when (status) {
        SignalStatus.ACTIVE -> "ACTIVE" to MaterialTheme.colorScheme.primary
        SignalStatus.BE -> "BREAK EVEN" to MaterialTheme.colorScheme.tertiary
        SignalStatus.CANCELLED -> "CANCELLED" to MaterialTheme.colorScheme.error
        SignalStatus.TP_HIT -> "TP HIT" to MaterialTheme.colorScheme.primary
        SignalStatus.SL_HIT -> "SL HIT" to MaterialTheme.colorScheme.error
    }
    AssistChip(onClick = {}, label = { Text(label) }, colors = AssistChipDefaults.assistChipColors(labelColor = color))
}
