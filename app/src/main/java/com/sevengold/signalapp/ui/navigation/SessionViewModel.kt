package com.sevengold.signalapp.ui.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    // true kalau listener gagal konek (bukan sekadar "masih loading"). UI pakai ini untuk
    // menampilkan tombol Coba Lagi / Keluar, alih-alih spinner tanpa akhir yang bikin user
    // nyangkut tanpa jalan keluar (misal setelah proses ke-kill paksa / koneksi Firestore lokal
    // sempat error).
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed

    // Job listener Firestore yang sedang aktif, supaya bisa dimatikan manual saat logout.
    private var listenJob: Job? = null
    private var notificationExpiryJob: Job? = null

    fun startListening(uid: String) {
        // Kalau sebelumnya sudah ada listener nyala (misal dari akun lain), matikan dulu.
        listenJob?.cancel()
        _loadFailed.value = false
        listenJob = viewModelScope.launch {
            userRepository.observeUser(uid)
                .catch { e ->
                    // Sebelumnya error di sini "ditelan diam-diam" -> _user.value tetap null
                    // selamanya -> UI nyangkut di spinner tanpa akhir tanpa cara keluar.
                    // Sekarang: dicatat (Logcat + Crashlytics) dan diberi tahu ke UI supaya
                    // bisa nawarin tombol Coba Lagi / Keluar.
                    Log.e("SessionViewModel", "Gagal listen profil user (uid=$uid)", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    _loadFailed.value = true
                }
                .collect { appUser ->
                    _loadFailed.value = false
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
