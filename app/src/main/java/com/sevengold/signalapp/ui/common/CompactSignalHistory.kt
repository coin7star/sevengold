package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.model.SignalType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compact history list used on USER and PREMIUM home screens.
 * Only a few recent items are shown inline; the complete history opens in a dialog.
 * This avoids a very tall home screen when many signals have accumulated.
 */
@Composable
fun CompactSignalHistorySection(
    signals: List<Signal>,
    locked: Boolean = false,
    maxPreview: Int = 5,
    detailContent: @Composable (Signal) -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    val preview = remember(signals, maxPreview) { signals.take(maxPreview) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Riwayat Sinyal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (signals.isNotEmpty()) {
                Text(
                    "${signals.size} sinyal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (signals.isEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    "Belum ada riwayat sinyal.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    preview.forEachIndexed { index, signal ->
                        CompactSignalRow(signal = signal, locked = locked)
                        if (index < preview.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }

            if (signals.size > maxPreview) {
                OutlinedButton(
                    onClick = { showAll = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Lihat semua riwayat (${signals.size})")
                }
            }
        }
    }

    if (showAll) {
        SignalHistoryDialog(
            signals = signals,
            detailContent = detailContent,
            onDismiss = { showAll = false }
        )
    }
}

@Composable
private fun CompactSignalRow(signal: Signal, locked: Boolean) {
    val df = remember { SimpleDateFormat("dd MMM • HH:mm", Locale("id", "ID")) }
    val isBuy = signal.type == SignalType.BUY
    val accent = if (isBuy) Color(0xFF3FBF8F) else Color(0xFFE5657A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 38.dp)
                .clip(RoundedCornerShape(50))
                .background(accent)
        )
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${signal.type.name} ${signal.pair}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(8.dp))
                CompactStatusBadge(signal.status)
            }
            Text(
                if (locked) "Detail sinyal tersedia untuk Premium" else "Entry ${formatPrice(signal.entry)} • TP ${formatPrice(signal.tp)} • SL ${formatPrice(signal.sl)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(Modifier.width(8.dp))
        Text(
            df.format(Date(signal.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactStatusBadge(status: SignalStatus) {
    val (label, color) = when (status) {
        SignalStatus.ACTIVE -> "Aktif" to Color(0xFFD4AF62)
        SignalStatus.BE -> "BE" to Color(0xFF8FA6D6)
        SignalStatus.CANCELLED -> "Batal" to Color(0xFF8B94A8)
        SignalStatus.TP_HIT -> "TP" to Color(0xFF3FBF8F)
        SignalStatus.SL_HIT -> "SL" to Color(0xFFE5657A)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SignalHistoryDialog(
    signals: List<Signal>,
    detailContent: @Composable (Signal) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Riwayat Sinyal", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(signals, key = { it.id }) { signal ->
                    detailContent(signal)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun LockedSignalCard(signal: Signal) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            Modifier
                .padding(14.dp)
                .blur(5.dp)
        ) {
            Text("${signal.type} ${signal.pair}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Entry •••••", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("TP •••••", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("SL •••••", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            Text(
                "🔒 Khusus Member Premium",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF241A02),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFD4AF62))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

private fun formatPrice(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
