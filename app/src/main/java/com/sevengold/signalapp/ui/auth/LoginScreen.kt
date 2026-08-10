package com.sevengold.signalapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("XAUUSD Signal", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        if (state.error != null) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { viewModel.login(email, password) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Masuk")
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onGoToRegister) {
            Text("Belum punya akun? Daftar")
        }
    }
}
