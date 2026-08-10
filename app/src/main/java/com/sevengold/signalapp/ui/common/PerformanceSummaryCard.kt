package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengold.signalapp.data.model.Signal
import java.util.Locale

/**
 * Ditaruh di beranda (USER & PREMIUM) setelah login. Nunjukin winrate, jumlah sinyal
 * menang/kalah, dan total pip untuk periode harian/mingguan/bulanan — dihitung
 * cuma dari sinyal berstatus TP_HIT/SL_HIT (CANCELLED & BE tidak dihitung).
 */
@Composable
fun PerformanceSummaryCard(signals: List<Signal>, modifier: Modifier = Modifier) {
    var period by remember { mutableStateOf(StatsPeriod.DAILY) }
    val stats = remember(signals, period) { signals.toPerformanceStats(period) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Performa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            TabRow(selectedTabIndex = period.ordinal) {
                StatsPeriod.values().forEach { p ->
                    Tab(selected = period == p, onClick = { period = p }, text = { Text(p.label) })
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(label = "Winrate", value = "${formatNumber(stats.winRatePercent)}%")
                StatItem(label = "Menang", value = "${stats.wins}")
                StatItem(label = "Kalah", value = "${stats.losses}")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(label = "Total Sinyal", value = "${stats.totalSignals}")
                StatItem(
                    label = "Total Pip",
                    value = "${if (stats.totalPips >= 0) "+" else ""}${formatNumber(stats.totalPips)} pip"
                )
            }

            if (stats.totalSignals == 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Belum ada sinyal closed (TP/SL) di periode ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatNumber(value: Double): String = String.format(Locale("id", "ID"), "%.1f", value)
