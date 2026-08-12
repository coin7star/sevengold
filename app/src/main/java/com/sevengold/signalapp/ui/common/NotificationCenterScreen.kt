package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengold.signalapp.notification.AppNotification
import com.sevengold.signalapp.notification.NotificationCenterStore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationCenterScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { NotificationCenterStore(context) }
    var items by remember { mutableStateOf(store.readAll()) }
    var selected by remember { mutableStateOf<AppNotification?>(null) }
    val unread = items.count { !it.read }
    val df = remember { SimpleDateFormat("dd MMM • HH:mm", Locale("id", "ID")) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Notifikasi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (unread > 0) "$unread belum dibaca" else "Semua notifikasi sudah dibaca", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (unread > 0) IconButton(onClick = { store.markAllRead(); items = store.readAll() }) {
                Icon(Icons.Filled.DoneAll, contentDescription = "Tandai semua dibaca")
            }
            if (items.isNotEmpty()) IconButton(onClick = { store.clear(); items = emptyList() }) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "Hapus notifikasi")
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Notifications, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Text("Belum ada notifikasi", fontWeight = FontWeight.SemiBold)
                    Text("Update sinyal akan muncul di sini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    NotificationRow(item, df) {
                        if (!item.read) store.markRead(item.id)
                        items = store.readAll()
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
private fun NotificationRow(item: AppNotification, df: SimpleDateFormat, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (!item.read) Badge { Text("Baru") }
                }
                Text(item.body, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp))
                Text(df.format(Date(item.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NotificationDetailDialog(item: AppNotification, df: SimpleDateFormat, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(df.format(Date(item.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.pair.isNotBlank()) Text(item.pair, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (item.type.isNotBlank()) Text("Arah: ${item.type}")
                if (item.entry.isNotBlank()) Text("Entry: ${item.entry}")
                if (item.tp.isNotBlank()) Text("TP: ${item.tp}")
                if (item.sl.isNotBlank()) Text("SL: ${item.sl}")
                if (item.body.isNotBlank()) Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
