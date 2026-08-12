package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sevengold.signalapp.notification.AppNotification
import com.sevengold.signalapp.notification.NotificationCenterStore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationCenterScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { NotificationCenterStore(context) }
    var allItems by remember { mutableStateOf(store.readAll()) }
    var selected by remember { mutableStateOf<AppNotification?>(null) }
    var filter by remember { mutableStateOf("Semua") }

    val unread = allItems.count { !it.read }
    val visibleItems = remember(allItems, filter) {
        when (filter) {
            "Belum dibaca" -> allItems.filter { !it.read }
            else -> allItems
        }
    }
    val df = remember { SimpleDateFormat("dd MMM • HH:mm", Locale("id", "ID")) }

    Column(modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (unread > 0) Icons.Filled.Notifications else Icons.Filled.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notifikasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (unread > 0) "$unread notifikasi belum dibaca"
                            else "Semua notifikasi sudah dibaca",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (unread > 0) {
                        IconButton(onClick = {
                            store.markAllRead()
                            allItems = store.readAll()
                        }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Tandai semua dibaca")
                        }
                    }
                    if (allItems.isNotEmpty()) {
                        IconButton(onClick = {
                            store.clear()
                            allItems = emptyList()
                            selected = null
                        }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Hapus semua notifikasi")
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = filter == "Semua",
                        onClick = { filter = "Semua" },
                        label = { Text("Semua (${allItems.size})") }
                    )
                    FilterChip(
                        selected = filter == "Belum dibaca",
                        onClick = { filter = "Belum dibaca" },
                        label = { Text("Belum dibaca ($unread)") }
                    )
                }
            }
        }

        if (visibleItems.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (allItems.isEmpty()) "Belum ada notifikasi" else "Tidak ada notifikasi baru",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (allItems.isEmpty()) "Update sinyal akan muncul di sini."
                        else "Semua notifikasi sudah Anda baca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleItems, key = { it.id }) { item ->
                    NotificationRow(item, df) {
                        if (!item.read) store.markRead(item.id)
                        allItems = store.readAll()
                        selected = item
                    }
                }
            }
        }
    }

    selected?.let { item ->
        NotificationDetailDialog(item, df) { selected = null }
    }
}

@Composable
private fun NotificationRow(
    item: AppNotification,
    df: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.read)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (!item.read) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        fontWeight = if (!item.read) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.read) {
                        Spacer(Modifier.width(8.dp))
                        Badge { Text("BARU") }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    item.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    df.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationDetailDialog(
    item: AppNotification,
    df: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    df.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.pair.isNotBlank()) {
                    Text(item.pair, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (item.type.isNotBlank()) DetailLine("Arah", item.type)
                if (item.entry.isNotBlank()) DetailLine("Entry", item.entry)
                if (item.tp.isNotBlank()) DetailLine("Take Profit", item.tp)
                if (item.sl.isNotBlank()) DetailLine("Stop Loss", item.sl)
                if (item.body.isNotBlank()) {
                    HorizontalDivider()
                    Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
