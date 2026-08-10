@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.SubscriptionOrderStatus
import com.sevengold.signalapp.data.model.SubscriptionPackage
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SubscriptionBanner(
    user: AppUser,
    packages: List<SubscriptionPackage>,
    onBuy: () -> Unit,
    premium: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (premium) "💎 Perpanjang Premium" else "👑 Upgrade ke Premium",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (premium) "Pilih paket untuk menambah durasi. Sisa Premium tidak akan hilang."
                else "Pilih paket Premium. Setelah pembayaran manual dikonfirmasi admin, akun otomatis menjadi PREMIUM.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                packages.take(2).forEach { pkg -> PackageMini(pkg, Modifier.weight(1f)) }
            }
            if (packages.size > 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    packages.drop(2).forEach { pkg -> PackageMini(pkg, Modifier.weight(1f)) }
                }
            }
            Button(onClick = onBuy, modifier = Modifier.fillMaxWidth()) {
                Text("Lihat Paket & Beli")
            }
        }
    }
}

@Composable
private fun PackageMini(pkg: SubscriptionPackage, modifier: Modifier) {
    Column(modifier) {
        Text(pkg.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Text("${rupiah(pkg.price)} • ${pkg.durationDays} hari", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SubscriptionPurchaseSheet(
    user: AppUser,
    packages: List<SubscriptionPackage>,
    orders: List<com.sevengold.signalapp.data.model.SubscriptionOrder>,
    message: String?,
    onBuy: (SubscriptionPackage, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPackage by remember { mutableStateOf<SubscriptionPackage?>(null) }
    var voucherCode by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Paket Premium", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pilih paket dulu. Setelah klik Beli, kamu akan diminta memasukkan voucher jika punya voucher welcome.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (user.welcomeVoucherCode.isNotBlank() && !user.welcomeVoucherUsed) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🎁 Kamu punya Voucher Welcome!", fontWeight = FontWeight.Bold)
                            Text(
                                "Diskon ${user.welcomeVoucherPercent}% akan ditawarkan otomatis saat kamu klik Beli Paket.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (orders.any { it.status == SubscriptionOrderStatus.PENDING }) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("⏳ Pesanan sedang menunggu approval", fontWeight = FontWeight.Bold)
                            Text(
                                "Admin akan memproses pesananmu setelah pembayaran dikonfirmasi.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            items(packages) { pkg ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pkg.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (pkg.label.isNotBlank()) AssistChip(onClick = {}, label = { Text(pkg.label) })
                        }
                        Text(
                            rupiah(pkg.price),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Aktif +${pkg.durationDays} hari Premium", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                selectedPackage = pkg
                                voucherCode = ""
                            },
                            enabled = !orders.any { it.status == SubscriptionOrderStatus.PENDING },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Beli Paket • ${rupiah(pkg.price)}")
                        }
                    }
                }
            }

            if (!message.isNullOrBlank()) {
                item {
                    Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Text(
                    "Voucher tidak wajib. Kalau kamu punya voucher welcome, masukkan setelah klik Beli Paket.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        val pkg = selectedPackage
        if (pkg != null) {
            val normalizedVoucher = voucherCode.trim().uppercase()
            val voucherValid = normalizedVoucher.isNotBlank() &&
                normalizedVoucher == user.welcomeVoucherCode.trim().uppercase() &&
                user.welcomeVoucherPercent > 0 &&
                !user.welcomeVoucherUsed
            val discountPercent = if (voucherValid) user.welcomeVoucherPercent.coerceIn(0, 100) else 0
            val discountAmount = (pkg.price * discountPercent) / 100L
            val finalPrice = (pkg.price - discountAmount).coerceAtLeast(0L)

            AlertDialog(
                onDismissRequest = { selectedPackage = null },
                title = { Text("Beli ${pkg.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${rupiah(pkg.price)} • +${pkg.durationDays} hari Premium")
                        if (user.welcomeVoucherCode.isNotBlank() && !user.welcomeVoucherUsed) {
                            OutlinedTextField(
                                value = voucherCode,
                                onValueChange = { voucherCode = it.uppercase() },
                                label = { Text("Voucher Welcome (opsional)") },
                                placeholder = { Text("Masukkan voucher jika punya") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (voucherCode.isNotBlank()) {
                                Text(
                                    if (voucherValid)
                                        "✓ Voucher valid • Hemat ${rupiah(discountAmount)}"
                                    else
                                        "Voucher tidak valid / sudah digunakan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (voucherValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        HorizontalDivider()
                        Text(
                            if (voucherValid)
                                "Harga ${rupiah(pkg.price)} → ${rupiah(finalPrice)}"
                            else
                                "Total: ${rupiah(finalPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Setelah klik konfirmasi, pesanan masuk ke admin untuk approval manual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedPackage = null }) { Text("Batal") }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onBuy(pkg, if (voucherValid) normalizedVoucher else "")
                            selectedPackage = null
                        }
                    ) {
                        Text("Konfirmasi • ${rupiah(finalPrice)}")
                    }
                }
            )
        }
    }
}

fun rupiah(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        .apply { maximumFractionDigits = 0 }
        .format(value)
