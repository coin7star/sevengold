@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import com.sevengold.signalapp.ui.theme.DangerRed
import com.sevengold.signalapp.ui.theme.GoldLight
import com.sevengold.signalapp.ui.theme.SignalGradients
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
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(88.dp)
                .shadow(16.dp, CircleShape, ambientColor = Color(0xFFD4AF62), spotColor = Color(0xFFD4AF62))
                .clip(CircleShape)
                .background(SignalGradients.avatarRing),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GoldLight
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = user.email.ifBlank { "(tanpa email)" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        ProfileRoleBadge(user.effectiveRole)

        Spacer(Modifier.height(28.dp))

        InfoCard {
            ProfileInfoRow("Role aktif", roleLabel(user.effectiveRole))

            if (user.effectiveRole == Role.PREMIUM) {
                Spacer(Modifier.height(12.dp))
                val expiry = user.premiumExpiryMillis?.let { df.format(Date(it)) } ?: "-"
                ProfileInfoRow("Premium sampai", expiry)
            }

            if (user.role == Role.PREMIUM && user.effectiveRole == Role.USER) {
                Spacer(Modifier.height(12.dp))
                ProfileInfoRow("Status", "Premium sudah habis")
            }

            Spacer(Modifier.height(12.dp))
            ProfileInfoRow(
                "Bergabung sejak",
                if (user.createdAt > 0) df.format(Date(user.createdAt)) else "-"
            )
        }

        Spacer(Modifier.height(16.dp))

        ReferralCard(user)

        Spacer(Modifier.height(16.dp))

        // Penjelasan singkat per-role, supaya user awam tidak bingung fitur apa yang dia punya.
        InfoCard(accentText = true) {
            Text("TENTANG ROLE KAMU", style = MaterialTheme.typography.labelLarge, color = GoldLight)
            Spacer(Modifier.height(8.dp))
            Text(
                roleDescription(user.effectiveRole),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1AE5657A))
                .padding(vertical = 2.dp)
        ) {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
            ) {
                Text("Keluar dari Akun", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ReferralCard(user: AppUser) {
    val clipboard = LocalClipboardManager.current
    val referralCode = user.referralCode.ifBlank { "SG${user.uid.take(8).uppercase()}" }
    val voucherCode = user.welcomeVoucherCode

    InfoCard(accentText = true) {
        Text("PROGRAM REFERRAL", style = MaterialTheme.typography.labelLarge, color = GoldLight)
        Spacer(Modifier.height(6.dp))
        Text(
            "Ajak teman berlangganan. Setelah teman berhasil berlangganan, kamu otomatis mendapat bonus 2 hari Premium.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        ReferralCodeRow("Kode referral", referralCode) {
            clipboard.setText(AnnotatedString(referralCode))
        }
        if (voucherCode.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            ReferralCodeRow("Voucher welcome", voucherCode) {
                clipboard.setText(AnnotatedString(voucherCode))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Diskon welcome: ${user.welcomeVoucherPercent}%. Voucher akan dimasukkan setelah kamu klik Beli Paket Premium.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                "Belum ada voucher welcome. Masukkan kode referral teman saat membuat akun untuk mendapatkannya.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Referral berhasil: ${user.referralSuccessfulCount} • Bonus terkumpul: ${user.referralRewardDaysEarned} hari Premium",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReferralCodeRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GoldLight)
        }
        TextButton(onClick = onCopy) { Text("Salin") }
    }
}

@Composable
private fun InfoCard(accentText: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black)
            .clip(RoundedCornerShape(20.dp))
            .background(if (accentText) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        content = content
    )
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
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileRoleBadge(role: Role) {
    val (label, color) = when (role) {
        Role.ADMIN -> "ADMIN" to DangerRed
        Role.PREMIUM -> "PREMIUM" to GoldLight
        Role.USER -> "USER" to MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
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
