package com.sevengold.signalapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val welcomeVoucherCode: String? = null,
    val welcomeVoucherPercent: Int = 0
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Email dan kata sandi wajib diisi")
            return
        }
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val result = repo.login(email.trim(), password)
            _state.value = result.fold(
                onSuccess = { AuthUiState(success = true) },
                onFailure = { AuthUiState(error = it.message ?: "Gagal masuk") }
            )
        }
    }

    fun register(email: String, password: String, confirmPassword: String, referralCode: String = "") {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Email dan kata sandi wajib diisi")
            return
        }
        if (password.length < 6) {
            _state.value = AuthUiState(error = "Kata sandi minimal 6 karakter")
            return
        }
        if (password != confirmPassword) {
            _state.value = AuthUiState(error = "Konfirmasi kata sandi tidak cocok")
            return
        }
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val result = repo.register(email.trim(), password, referralCode)
            _state.value = result.fold(
                onSuccess = { registration ->
                    AuthUiState(
                        success = true,
                        welcomeVoucherCode = registration.voucherCode.ifBlank { null },
                        welcomeVoucherPercent = registration.voucherPercent
                    )
                },
                onFailure = { AuthUiState(error = it.message ?: "Registrasi gagal") }
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
