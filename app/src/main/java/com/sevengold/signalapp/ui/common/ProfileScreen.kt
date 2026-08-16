@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import com.sevengold.signalapp.data.repository.UserRepository
import kotlinx.coroutines.launch
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
    val authUser = remember { FirebaseAuth.getInstance().currentUser }
    val clipboard = LocalClipboardManager.current
    val displayName = authUser?.displayName?.takeIf { it.isNotBlank() }
        ?: user.email.substringBefore("@").replaceFirstChar { it.uppercase() }
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString()
        ?: user.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val expiry = user.premiumExpiryMillis
    val remainingMillis = expiry?.minus(System.currentTimeMillis()) ?: 0L
    val remainingDays = if (remainingMillis > 0L) ((remainingMillis + 86_399_999L) / 86_400_000L) else 0L
    val provider = authUser?.providerData
        ?.firstOrNull { it.providerId != "firebase" }
        ?.providerId
        ?.let { if (it.contains("google")) "Google" else "Email & Password" }
        ?: "Email & Password"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero profile card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .shadow(16.dp, CircleShape, ambientColor = Color(0xFFD4AF62), spotColor = Color(0xFFD4AF62))
                        .clip(CircleShape)
                        .background(SignalGradients.avatarRing),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, style = MaterialTheme.typography.headlineMedium, color = GoldLight, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(user.email.ifBlank { "(tanpa email)" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                ProfileRoleBadge(user.effectiveRole)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Premium status becomes the visual focus for Premium users.
        if (user.effectiveRole == Role.PREMIUM) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1AD4AF62))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = GoldLight)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("PREMIUM AKTIF", style = MaterialTheme.typography.labelLarge, color = GoldLight, fontWeight = FontWeight.Bold)
                            Text("Sisa sekitar $remainingDays hari", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (remainingDays.coerceAtMost(30L) / 30f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Berlaku sampai ${expiry?.let { df.format(Date(it)) } ?: "-"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Account details
        InfoCard(accentText = true) {
            Text("ACCOUNT CENTER", style = MaterialTheme.typography.labelLarge, color = GoldLight)
            Spacer(Modifier.height(10.dp))
            ProfileInfoRow("Status akun", if (user.effectiveRole == Role.PREMIUM) "Premium aktif" else roleLabel(user.effectiveRole))
            Spacer(Modifier.height(10.dp))
            ProfileInfoRow("Login", provider)
            Spacer(Modifier.height(10.dp))
            ProfileInfoRow("Bergabung sejak", if (user.createdAt > 0) df.format(Date(user.createdAt)) else "-")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("User ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user.uid, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(user.uid)) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Salin User ID")
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        ReferralCard(user)

        Spacer(Modifier.height(14.dp))
        if (user.effectiveRole == Role.PREMIUM) {
            TelegramNotificationCard(user)
            Spacer(Modifier.height(14.dp))
        }

        InfoCard(accentText = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = GoldLight)
                Spacer(Modifier.width(8.dp))
                Text("KEAMANAN AKUN", style = MaterialTheme.typography.labelLarge, color = GoldLight)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Gunakan password yang kuat dan jangan membagikan kode login kepada siapa pun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))
        InfoCard(accentText = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = GoldLight)
                Spacer(Modifier.width(8.dp))
                Text("INFORMASI PERAN", style = MaterialTheme.typography.labelLarge, color = GoldLight)
            }
            Spacer(Modifier.height(8.dp))
            Text(roleDescription(user.effectiveRole), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
        ) {
            Text("Keluar dari Akun", fontWeight = FontWeight.Bold)
        }

        // Tombol khusus ADMIN untuk memaksa crash percobaan, supaya Crashlytics punya
        // data pertama untuk dikonfirmasi di dashboard. TIDAK muncul untuk user biasa.
        if (user.effectiveRole == Role.ADMIN) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { throw RuntimeException("Test Crash dari halaman Profil (ADMIN)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Text("Test Crash (Crashlytics)", fontWeight = FontWeight.Medium)
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
        Text("PROGRAM REFERAL", style = MaterialTheme.typography.labelLarge, color = GoldLight)
        Spacer(Modifier.height(6.dp))
        Text(
            "Ajak teman untuk berlangganan. Setelah teman berhasil berlangganan, Anda akan otomatis menerima bonus 2 hari Premium.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        ReferralCodeRow("Kode referal", referralCode) {
            clipboard.setText(AnnotatedString(referralCode))
        }
        if (voucherCode.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            ReferralCodeRow("Voucher selamat datang", voucherCode) {
                clipboard.setText(AnnotatedString(voucherCode))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Diskon selamat datang: ${user.welcomeVoucherPercent}%. Voucher dapat digunakan saat membeli paket Premium.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                "Belum tersedia voucher selamat datang. Masukkan kode referal teman saat mendaftar untuk mendapatkannya.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Referal berhasil: ${user.referralSuccessfulCount} • Total bonus: ${user.referralRewardDaysEarned} hari Premium",
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
private fun TelegramNotificationCard(user: AppUser) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val repository = remember { UserRepository() }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selectedEvents by remember(user.telegramNotificationEvents) {
        mutableStateOf(
            if (user.telegramNotificationEvents.isEmpty()) {
                TelegramNotificationEvents.ALL.toMutableSet()
            } else {
                user.telegramNotificationEvents.toMutableSet()
            }
        )
    }

    InfoCard(accentText = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Send, contentDescription = null, tint = GoldLight)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("NOTIFIKASI TELEGRAM", style = MaterialTheme.typography.labelLarge, color = GoldLight)
                Text(
                    if (user.telegramConnected)
                        "Terhubung${user.telegramUsername?.takeIf { it.isNotBlank() }?.let { " • @$it" } ?: ""}"
                    else "Belum terhubung",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!user.telegramConnected) {
            Text(
                "Hubungkan Telegram untuk menerima notifikasi sinyal Premium melalui bot. Telegram bersifat tambahan; notifikasi aplikasi tetap berjalan seperti biasa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (user.telegramConnectionCode.isNotBlank() &&
                (user.telegramConnectionExpiresAt ?: 0L) > System.currentTimeMillis()
            ) {
                Text("Kode koneksi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.telegramConnectionCode,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString("/start ${user.telegramConnectionCode}"))
                        message = "Perintah koneksi disalin."
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Salin kode")
                    }
                }
                Text(
                    "Kode ini berlaku 10 menit. Tekan tombol di bawah untuk membuka bot Telegram dan menghubungkan akun secara otomatis.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val code = user.telegramConnectionCode
                        val deepLink = "tg://resolve?domain=signalalertsniper_bot&start=${Uri.encode("SG-$code")}"
                        val webLink = "https://t.me/signalalertsniper_bot?start=${Uri.encode("SG-$code")}"
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)))
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webLink)))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Telegram & Hubungkan")
                }
                Spacer(Modifier.height(10.dp))
            }

            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        repository.createTelegramConnectionCode(user.uid)
                            .onSuccess {
                                message = "Kode koneksi dibuat. Kirim kode tersebut ke bot Telegram."
                            }
                            .onFailure { message = it.message ?: "Gagal membuat kode koneksi." }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Link, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "Menyiapkan..." else "Buat Kode Koneksi")
            }
        } else {
            Text(
                "Telegram terhubung. Pilih jenis notifikasi yang ingin diterima.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            TelegramNotificationEvents.ALL.forEach { event ->
                val checked = event in selectedEvents
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            val next = selectedEvents.toMutableSet().apply {
                                if (it) add(event) else remove(event)
                            }
                            selectedEvents = next
                            scope.launch {
                                repository.updateTelegramNotificationEvents(user.uid, next.toList())
                            }
                        }
                    )
                    Text(TelegramNotificationEvents.label(event), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        repository.disconnectTelegram(user.uid)
                            .onSuccess { message = "Telegram berhasil diputuskan." }
                            .onFailure { message = it.message ?: "Gagal memutuskan Telegram." }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.LinkOff, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Putuskan Telegram")
            }
        }

        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, color = GoldLight)
        }
    }
}

private object TelegramNotificationEvents {
    const val SIGNAL_CREATED = "SIGNAL_CREATED"
    const val SIGNAL_ACTIVE = "SIGNAL_ACTIVE"
    const val TP_HIT = "TP_HIT"
    const val SL_HIT = "SL_HIT"
    const val BE = "BE"
    const val CANCELLED = "CANCELLED"

    val ALL = listOf(SIGNAL_CREATED, TP_HIT, SL_HIT, BE, CANCELLED)

    fun label(event: String): String = when (event) {
        SIGNAL_CREATED -> "Sinyal baru diterbitkan"
        TP_HIT -> "TP tercapai"
        SL_HIT -> "SL tercapai"
        BE -> "Break Even"
        CANCELLED -> "Sinyal dibatalkan"
        SIGNAL_ACTIVE -> "Sinyal aktif"
        else -> event
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
    Role.ADMIN -> "Administrator"
    Role.PREMIUM -> "Premium"
    Role.USER -> "Pengguna"
}

private fun roleDescription(role: Role): String = when (role) {
    Role.ADMIN -> "Anda dapat menerbitkan dan mengelola sinyal (TP/SL/BE/Batalkan), membuat kode langganan, serta mengubah peran pengguna lain melalui menu Pengguna."
    Role.PREMIUM -> "Anda dapat melihat seluruh sinyal secara lengkap selama masa Premium masih aktif."
    Role.USER -> "Sinyal masih terkunci. Gunakan kode langganan dari administrator untuk membuka seluruh sinyal."
}
