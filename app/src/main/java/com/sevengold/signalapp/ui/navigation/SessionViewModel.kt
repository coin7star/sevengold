package com.sevengold.signalapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun startListening(uid: String) {
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { appUser ->
                _user.value = appUser
            }
        }
    }

    fun currentUid(): String? = auth.currentUser?.uid

    fun logout() {
        com.sevengold.signalapp.notification.NotificationTopics.unsubscribeFromPremiumSignals()
        auth.signOut()
        _user.value = null
    }
}
