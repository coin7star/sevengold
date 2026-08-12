package com.sevengold.signalapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.sevengold.signalapp.notification.NotificationTopics
import com.sevengold.signalapp.ui.navigation.AppNav
import com.sevengold.signalapp.ui.theme.SignalAppTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Kalau ditolak, notif cuma gak akan muncul — sisa fitur app tetap jalan normal. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = com.sevengold.signalapp.ui.theme.NavyBackground.toArgb()
        window.navigationBarColor = com.sevengold.signalapp.ui.theme.NavyBackground.toArgb()

        NotificationTopics.createChannelIfNeeded(this)
        askNotificationPermissionIfNeeded()

        setContent {
            SignalAppTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        ),
                    color = Color.Transparent
                ) {
                    AppNav()
                }
            }
        }
    }

    /** Android 13+ (API 33) wajib minta izin runtime dulu sebelum notifikasi bisa muncul. */
    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
