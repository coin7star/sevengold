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
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) onRegisterSuccess()
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
                    onClick = { viewModel.register(email, password, confirm) }
                )

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Sudah punya akun? Masuk", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
