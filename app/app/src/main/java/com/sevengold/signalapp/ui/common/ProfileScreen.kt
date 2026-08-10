@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import java.text.SimpleDateFormat
import java.util.*

/**
 * Halaman Profil yang sama untuk ketiga role (ADMIN, PREMIUM, USER).
 * Tujuannya: siapa pun yang login selalu langsung tahu dengan jelas —
 * ini akun siapa, role apa yang aktif sekarang, dan sampai kapan (kalau PREMIUM) —
 * plus tombol "Keluar" yang jelas, biar tidak bingung/pusing pindah-pindah menu.
 */
@Composable
fun ProfileScreen(
    user: AppUser,
    onLogout: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = user.email.ifBlank { "(tanpa email)" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        ProfileRoleBadge(user.effectiveRole)

        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                ProfileInfoRow("Role aktif", roleLabel(user.effectiveRole))

                if (user.effectiveRole == Role.PREMIUM) {
                    Spacer(Modifier.height(10.dp))
                    val expiry = user.premiumExpiryMillis?.let { df.format(Date(it)) } ?: "-"
                    ProfileInfoRow("Premium sampai", expiry)
                }

                if (user.role == Role.PREMIUM && user.effectiveRole == Role.USER) {
                    Spacer(Modifier.height(10.dp))
                    ProfileInfoRow("Status", "Premium sudah habis")
                }

                Spacer(Modifier.height(10.dp))
                ProfileInfoRow(
                    "Bergabung sejak",
                    if (user.createdAt > 0) df.format(Date(user.createdAt)) else "-"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Penjelasan singkat per-role, supaya user awam tidak bingung fitur apa yang dia punya.
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Tentang role kamu", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(roleDescription(user.effectiveRole), style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(28.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Keluar dari Akun")
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProfileRoleBadge(role: Role) {
    val (label, color) = when (role) {
        Role.ADMIN -> "ADMIN" to MaterialTheme.colorScheme.error
        Role.PREMIUM -> "PREMIUM" to MaterialTheme.colorScheme.primary
        Role.USER -> "USER" to MaterialTheme.colorScheme.outline
    }
    AssistChip(onClick = {}, label = { Text(label) }, colors = AssistChipDefaults.assistChipColors(labelColor = color))
}

private fun roleLabel(role: Role): String = when (role) {
    Role.ADMIN -> "Admin"
    Role.PREMIUM -> "Premium"
    Role.USER -> "User"
}

private fun roleDescription(role: Role): String = when (role) {
    Role.ADMIN -> "Kamu bisa publish & kelola sinyal (TP/SL/BE/Cancel), generate kode langganan, dan naik/turunin role user lain lewat tab Users."
    Role.PREMIUM -> "Kamu bisa lihat semua sinyal secara penuh selama status premium masih aktif."
    Role.USER -> "Sinyal masih tampil terkunci/blur. Redeem kode langganan dari admin untuk membuka semua sinyal secara penuh."
}
