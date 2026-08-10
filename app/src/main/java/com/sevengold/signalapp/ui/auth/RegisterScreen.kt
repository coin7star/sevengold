@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sevengold.signalapp.ui.theme.SignalGradients

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    var showWelcomeVoucher by remember { mutableStateOf(false) }

    LaunchedEffect(state.success, state.welcomeVoucherCode) {
        if (state.success && !state.welcomeVoucherCode.isNullOrBlank()) {
            showWelcomeVoucher = true
        } else if (state.success) {
            onRegisterSuccess()
        }
    }

    if (showWelcomeVoucher) {
        AlertDialog(
            onDismissRequest = {
                showWelcomeVoucher = false
                onRegisterSuccess()
            },
            title = { Text("🎁 Voucher Welcome Kamu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Akun berhasil dibuat!")
                    Text(
                        "Kamu mendapat voucher welcome ${state.welcomeVoucherPercent}% karena mendaftar lewat referral teman.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            state.welcomeVoucherCode.orEmpty(),
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Voucher akan digunakan nanti saat kamu klik Beli Paket Premium.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showWelcomeVoucher = false
                    onRegisterSuccess()
                }) { Text("Oke, Lihat Paket") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SignalGradients.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark()

            Spacer(Modifier.height(36.dp))

            AuthCard {
                Text(
                    "Buat Akun Baru",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Gabung dan pantau sinyal XAUUSD real-time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))

                PremiumTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = Icons.Filled.Mail,
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(14.dp))

                PremiumTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password (min. 6 karakter)",
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true
                )
                Spacer(Modifier.height(14.dp))

                PremiumTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = "Konfirmasi Password",
                    leadingIcon = Icons.Filled.LockReset,
                    isPassword = true
                )
                Spacer(Modifier.height(14.dp))

                PremiumTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it.uppercase() },
                    label = "Kode Referral (opsional)",
                    leadingIcon = Icons.Filled.Person
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pakai kode teman untuk mendapatkan voucher welcome berlangganan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.error != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(24.dp))
                GoldButton(
                    text = "Daftar",
                    loading = state.loading,
                    onClick = { viewModel.register(email, password, confirm, referralCode) }
                )

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Sudah punya akun? Masuk", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
