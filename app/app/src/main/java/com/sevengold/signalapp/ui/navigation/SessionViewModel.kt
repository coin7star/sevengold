package com.sevengold.signalapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.repository.UserRepository
import kotlinx.coroutines.Job
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
                }
        }
    }

    fun currentUid(): String? = auth.currentUser?.uid

    /**
     * Keluar dari akun. Urutan penting: matikan listener Firestore dulu SEBELUM signOut(),
     * supaya tidak ada race condition yang bikin UI nyangkut sebelum pindah ke menu Login.
     */
    fun logout() {
        listenJob?.cancel()
        listenJob = null
        com.sevengold.signalapp.notification.NotificationTopics.unsubscribeFromPremiumSignals()
        auth.signOut()
        _user.value = null
    }
}
