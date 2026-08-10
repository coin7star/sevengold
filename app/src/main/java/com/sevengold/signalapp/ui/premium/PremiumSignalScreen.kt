@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.premium

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.ui.common.SignalListViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PremiumSignalScreen(
    expiryLabel: String,
    onLogout: () -> Unit,
    vm: SignalListViewModel = viewModel()
) {
    val signals by vm.signals.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Sinyal XAUUSD") },
            actions = { TextButton(onClick = onLogout) { Text("Keluar") } }
        )
        Text(
            "Premium aktif sampai: $expiryLabel",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(signals) { signal -> SignalCard(signal) }
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
        SignalStatus.CLOSED -> "CLOSED" to MaterialTheme.colorScheme.outline
    }
    AssistChip(onClick = {}, label = { Text(label) }, colors = AssistChipDefaults.assistChipColors(labelColor = color))
}
