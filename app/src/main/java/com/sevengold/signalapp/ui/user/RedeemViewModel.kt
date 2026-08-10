package com.sevengold.signalapp.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RedeemUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class RedeemViewModel(
    private val repo: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(RedeemUiState())
    val state: StateFlow<RedeemUiState> = _state

    fun redeem(uid: String, code: String) {
        if (code.isBlank()) {
            _state.value = RedeemUiState(error = "Masukkan kode terlebih dahulu")
            return
        }
        _state.value = RedeemUiState(loading = true)
        viewModelScope.launch {
            val result = repo.redeemCode(uid, code.trim().uppercase())
            _state.value = result.fold(
                onSuccess = { RedeemUiState(success = true) },
                onFailure = { RedeemUiState(error = it.message ?: "Kode tidak valid") }
            )
        }
    }

    fun reset() {
        _state.value = RedeemUiState()
    }
}
