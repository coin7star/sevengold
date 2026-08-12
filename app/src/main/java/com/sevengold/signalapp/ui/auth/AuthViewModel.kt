package com.sevengold.signalapp.ui.auth

import android.content.Context
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
    val welcomeVoucherPercent: Int = 0,
    // true = Credential Manager gagal (biasanya masalah device/ROM), minta UI buka
    // layar pilih akun Google versi klasik sebagai jalur cadangan.
    val launchLegacyGoogleSignIn: Boolean = false
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

    fun loginWithGoogle(context: Context) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val result = repo.loginWithGoogle(context)
            _state.value = result.fold(
                onSuccess = { AuthUiState(success = true) },
                onFailure = {
                    // Credential Manager gagal (paling sering: masalah kompatibilitas
                    // device/ROM, bukan konfigurasi). Minta UI coba jalur klasik dulu
                    // sebelum menampilkan pesan error ke user.
                    AuthUiState(loading = false, launchLegacyGoogleSignIn = true)
                }
            )
        }
    }

    /** Dipanggil UI untuk mengambil Intent layar pilih akun Google versi klasik. */
    fun buildLegacyGoogleSignInIntent(context: Context) = repo.buildLegacyGoogleSignInIntent(context)

    /** Dipanggil UI setelah user selesai memilih akun di layar Google klasik. */
    fun onLegacyGoogleSignInResult(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            _state.value = AuthUiState(error = "Login dengan Google dibatalkan atau gagal")
            return
        }
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val result = repo.signInWithGoogleIdToken(idToken)
            _state.value = result.fold(
                onSuccess = { AuthUiState(success = true) },
                onFailure = { AuthUiState(error = it.message ?: "Gagal masuk dengan Google") }
            )
        }
    }

    /** UI memanggil ini setelah selesai menindaklanjuti launchLegacyGoogleSignIn, supaya tidak dobel-trigger. */
    fun consumeLegacyGoogleSignInRequest() {
        _state.value = _state.value.copy(launchLegacyGoogleSignIn = false)
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
