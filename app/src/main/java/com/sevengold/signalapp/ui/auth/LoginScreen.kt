@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sevengold.signalapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.sevengold.signalapp.ui.theme.SignalGradients

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.success) {
        if (state.success) onLoginSuccess()
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
                    "Selamat Datang",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Masuk untuk lihat sinyal XAUUSD terbaru",
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
                    label = "Password",
                    leadingIcon = Icons.Filled.Lock,
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
                    text = "Masuk",
                    loading = state.loading,
                    onClick = { viewModel.login(email, password) }
                )

                Spacer(Modifier.height(12.dp))
                GoogleButton(
                    loading = state.loading,
                    onClick = { viewModel.loginWithGoogle(context) }
                )

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onGoToRegister, modifier = Modifier.fillMaxWidth()) {
                    Text("Belum punya akun? Daftar", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Logo bulat bergradasi emas dengan ikon chart, plus nama brand. */
@Composable
internal fun BrandMark() {
    Box(
        modifier = Modifier
            .size(84.dp)
            .shadow(18.dp, CircleShape, ambientColor = Color(0xFFD4AF62), spotColor = Color(0xFFD4AF62))
            .clip(CircleShape)
            .background(SignalGradients.avatarRing),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.ShowChart,
            contentDescription = null,
            tint = Color(0xFF0B1220),
            modifier = Modifier.size(38.dp)
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "SEVENGOLD",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Text(
        "XAUUSD Signal — Sinyal Trading Premium",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )
}

/** Card kaca gelap dengan border tipis emas, dipakai di Login & Register. */
@Composable
internal fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(28.dp))
            .background(SignalGradients.heroCard)
            .background(Color(0x1AFFFFFF))
            .padding(24.dp),
        content = content
    )
}

@Composable
internal fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = Color(0x14FFFFFF),
            unfocusedContainerColor = Color(0x0DFFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GoogleButton(
    loading: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0x12FFFFFF),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "G",
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Masuk dengan Google",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Tombol utama emas bergradasi, dipakai untuk aksi primer (Masuk / Daftar / Aktifkan). */
@Composable
internal fun GoldButton(
    text: String,
    loading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFD4AF62))
            .clip(RoundedCornerShape(16.dp))
            .background(SignalGradients.goldButton)
            .then(
                if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color(0xFF241A02), strokeWidth = 2.5.dp)
        } else {
            Text(text, color = Color(0xFF241A02), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}
