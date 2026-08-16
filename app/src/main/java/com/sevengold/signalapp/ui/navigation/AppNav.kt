package com.sevengold.signalapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sevengold.signalapp.data.model.Role
import com.sevengold.signalapp.ui.admin.AdminPanelScreen
import com.sevengold.signalapp.ui.common.AdaptiveAppFrame
import com.sevengold.signalapp.ui.auth.LoginScreen
import com.sevengold.signalapp.ui.auth.RegisterScreen
import com.sevengold.signalapp.ui.premium.PremiumSignalScreen
import com.sevengold.signalapp.ui.user.UserSignalScreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
}

@Composable
fun AppNav(sessionViewModel: SessionViewModel = viewModel()) {
    val navController = rememberNavController()
    val user by sessionViewModel.user.collectAsState()

    // Kalau sudah pernah login sebelumnya (Firebase Auth persist otomatis), langsung mulai listen.
    LaunchedEffect(Unit) {
        sessionViewModel.currentUid()?.let { sessionViewModel.startListening(it) }
    }

    val startDestination = if (sessionViewModel.currentUid() != null) Routes.HOME else Routes.LOGIN

    AdaptiveAppFrame {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    sessionViewModel.currentUid()?.let { sessionViewModel.startListening(it) }
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    sessionViewModel.currentUid()?.let { sessionViewModel.startListening(it) }
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            val currentUser = user
            val uid = sessionViewModel.currentUid()
            val loadFailed by sessionViewModel.loadFailed.collectAsState()

            if (uid == null || currentUser == null) {
                // Kalau listener eksplisit gagal (bukan sekadar masih loading), ATAU sudah
                // nunggu kelamaan tanpa kabar apa-apa (misal koneksi macet), kasih tombol
                // Coba Lagi & Keluar — supaya user tidak PERNAH nyangkut selamanya di
                // spinner tanpa jalan keluar (ini yang sebelumnya bikin app kelihatan
                // "blank"/macet setelah proses ke-kill paksa).
                var showTimeoutEscape by remember { mutableStateOf(false) }
                LaunchedEffect(uid) {
                    showTimeoutEscape = false
                    delay(8_000)
                    showTimeoutEscape = true
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (loadFailed || showTimeoutEscape) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                        ) {
                            Text(
                                "Gagal memuat profil akun. Cek koneksi internet kamu.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    sessionViewModel.currentUid()?.let { sessionViewModel.startListening(it) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Coba Lagi") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    sessionViewModel.logout()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Keluar dari Akun") }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
                return@composable
            }

            val onLogout: () -> Unit = {
                sessionViewModel.logout()
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            }

            when (currentUser.effectiveRole) {
                Role.ADMIN -> AdminPanelScreen(adminUid = uid, user = currentUser, onLogout = onLogout)
                Role.PREMIUM -> {
                    // Subscribe ke topic notif tiap kali layar ini kebuka dgn role PREMIUM aktif.
                    // subscribeToTopic aman dipanggil berkali-kali (idempotent).
                    LaunchedEffect(Unit) {
                        com.sevengold.signalapp.notification.NotificationTopics.subscribeToPremiumSignals()
                    }
                    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
                    val expiryLabel = currentUser.premiumExpiryMillis?.let { df.format(Date(it)) } ?: "-"
                    PremiumSignalScreen(user = currentUser, expiryLabel = expiryLabel, onLogout = onLogout)
                }
                Role.USER -> {
                    // Pastikan user yang bukan/tidak lagi PREMIUM gak ke-subscribe (misal expired).
                    LaunchedEffect(Unit) {
                        com.sevengold.signalapp.notification.NotificationTopics.unsubscribeFromPremiumSignals()
                    }
                    UserSignalScreen(uid = uid, user = currentUser, onLogout = onLogout)
                }
            }
        }
        }
    }
}
