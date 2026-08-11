package com.sevengold.signalapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Live-observe dokumen users/{uid} milik user yang sedang login.
 * Begitu ADMIN mengubah role seseorang di Firestore, UI otomatis pindah tanpa perlu logout.
 */
class SessionViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _user = MutableStateFlow<AppUser?>(null)
    val user: StateFlow<AppUser?> = _user

    // Job listener Firestore yang sedang aktif, supaya bisa dimatikan manual saat logout.
    private var listenJob: Job? = null
    private var notificationExpiryJob: Job? = null

    fun startListening(uid: String) {
        // Kalau sebelumnya sudah ada listener nyala (misal dari akun lain), matikan dulu.
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            userRepository.observeUser(uid)
                // Setelah signOut(), Firestore bakal balikin error permission-denied ke listener lama.
                // Ditangkap di sini supaya tidak nge-crash app / ganggu proses balik ke halaman Login.
                .catch { /* listener ditutup karena sesi berakhir (logout) — aman diabaikan */ }
                .collect { appUser ->
                    _user.value = appUser
                    syncPremiumNotificationSubscription(appUser)
                }
        }
    }

    fun currentUid(): String? = auth.currentUser?.uid


    /**
     * Sinkronisasi subscription FCM dengan role Premium yang sedang aktif.
     *
     * - PREMIUM aktif: subscribe ke topic premium_signals.
     * - USER/expired/ADMIN: unsubscribe.
     * - Jika Premium akan expired saat app tetap terbuka, jadwalkan unsubscribe
     *   agar perangkat tidak terus menerima sinyal setelah masa Premium berakhir.
     */
    private fun syncPremiumNotificationSubscription(appUser: AppUser) {
        notificationExpiryJob?.cancel()

        val isActivePremium = appUser.isPremiumActive
        if (isActivePremium) {
            com.sevengold.signalapp.notification.NotificationTopics.subscribeToPremiumSignals()

            val expiry = appUser.premiumExpiryMillis ?: return
            val remaining = expiry - System.currentTimeMillis()
            if (remaining > 0L) {
                notificationExpiryJob = viewModelScope.launch {
                    delay(remaining)
                    com.sevengold.signalapp.notification.NotificationTopics.unsubscribeFromPremiumSignals()
                }
            }
        } else {
            com.sevengold.signalapp.notification.NotificationTopics.unsubscribeFromPremiumSignals()
        }
    }

    /**
     * Keluar dari akun. Urutan penting: matikan listener Firestore dulu SEBELUM signOut(),
     * supaya tidak ada race condition yang bikin UI nyangkut sebelum pindah ke menu Login.
     */
    fun logout() {
        listenJob?.cancel()
        listenJob = null
        notificationExpiryJob?.cancel()
        notificationExpiryJob = null
        com.sevengold.signalapp.notification.NotificationTopics.unsubscribeFromPremiumSignals()
        auth.signOut()
        _user.value = null
    }
}
