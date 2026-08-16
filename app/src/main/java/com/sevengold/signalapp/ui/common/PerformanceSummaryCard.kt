package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.ui.theme.EmeraldAccent
import com.sevengold.signalapp.ui.theme.GoldLight
import com.sevengold.signalapp.ui.theme.SignalGradients
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(28.dp))
            .background(SignalGradients.heroCard)
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GoldLight)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "PERFORMA",
                style = MaterialTheme.typography.labelLarge,
                color = GoldLight
            )
        }
        Spacer(Modifier.height(14.dp))

        PeriodSelectorForAdmin(selected = period, onSelect = { period = it })
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem(label = "Winrate", value = "${formatNumber(stats.winRatePercent)}%", accent = GoldLight)
            StatItem(label = "Menang", value = "${stats.wins}", accent = EmeraldAccent)
            StatItem(label = "Kalah", value = "${stats.losses}", accent = Color(0xFFE5657A))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem(label = "Total Sinyal", value = "${stats.totalSignals}")
            StatItem(
                label = "Total Pip",
                value = "${if (stats.totalPips >= 0) "+" else ""}${formatNumber(stats.totalPips)} pip",
                accent = if (stats.totalPips >= 0) EmeraldAccent else Color(0xFFE5657A)
            )
        }

        if (stats.totalSignals == 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Belum ada sinyal closed (TP/SL) di periode ini.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B94A8)
            )
        }
    }
}

@Composable
fun PeriodSelectorForAdmin(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatsPeriod.values().forEach { p ->
            val isSelected = p == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (isSelected) SignalGradients.premiumBadge
                        else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                    )
                    .clickable { onSelect(p) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    p.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF241A02) else Color(0xFFB6BECF)
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, accent: Color = Color.Unspecified) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (accent != Color.Unspecified) accent else MaterialTheme.colorScheme.onSurface
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B94A8))
    }
}

private fun formatNumber(value: Double): String = String.format(Locale("id", "ID"), "%.1f", value)
