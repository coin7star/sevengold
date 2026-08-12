package com.sevengold.signalapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

            if (uid == null || currentUser == null) {
                // Masih loading profil dari Firestore
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
