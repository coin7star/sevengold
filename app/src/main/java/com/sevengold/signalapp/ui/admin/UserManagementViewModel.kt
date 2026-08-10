package com.sevengold.signalapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import com.sevengold.signalapp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserManagementViewModel(
    private val repo: UserRepository = UserRepository()
) : ViewModel() {

    private val _users = MutableStateFlow<List<AppUser>>(emptyList())
    val users: StateFlow<List<AppUser>> = _users

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    init {
        viewModelScope.launch {
            repo.observeAllUsers().collect { _users.value = it }
        }
    }

    /** ADMIN naikin user jadi PREMIUM dengan durasi tertentu (hari), langsung dari panel. */
    fun setPremium(uid: String, durationDays: Int) {
        viewModelScope.launch {
            val result = repo.adminSetRole(uid, Role.PREMIUM, durationDays)
            _actionMessage.value = if (result.isSuccess) "Berhasil dijadikan PREMIUM" else "Gagal: ${result.exceptionOrNull()?.message}"
        }
    }

    /** ADMIN turunin balik ke USER biasa. */
    fun setUser(uid: String) {
        viewModelScope.launch {
            val result = repo.adminSetRole(uid, Role.USER)
            _actionMessage.value = if (result.isSuccess) "Berhasil dijadikan USER" else "Gagal: ${result.exceptionOrNull()?.message}"
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}
