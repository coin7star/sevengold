package com.sevengold.signalapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.model.SubscriptionCode
import com.sevengold.signalapp.data.model.ReferralSettings
import com.sevengold.signalapp.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _codes = MutableStateFlow<List<SubscriptionCode>>(emptyList())
    val codes: StateFlow<List<SubscriptionCode>> = _codes

    private val _referralSettings = MutableStateFlow(ReferralSettings())
    val referralSettings: StateFlow<ReferralSettings> = _referralSettings

    private val _settingsMessage = MutableStateFlow<String?>(null)
    val settingsMessage: StateFlow<String?> = _settingsMessage

    private val _lastGeneratedCode = MutableStateFlow<String?>(null)
    val lastGeneratedCode: StateFlow<String?> = _lastGeneratedCode

    init {
        viewModelScope.launch {
            repo.observeReferralSettings()
                .catch { }
                .collect { _referralSettings.value = it }
        }
        viewModelScope.launch {
            repo.observeCodes()
                // Kalau user logout, Firestore balikin error permission-denied ke listener ini.
                // Ditangkap di sini supaya tidak nge-crash app (layar putih) pas keluar.
                .catch { }
                .collect { _codes.value = it }
        }
    }

    fun generateCode(durationDays: Int, adminUid: String) {
        viewModelScope.launch {
            val result = repo.createCode(durationDays, adminUid)
            _lastGeneratedCode.value = result.getOrNull()
        }
    }

    fun updateReferralSettings(rewardDays: Int, voucherPercent: Int, enabled: Boolean) {
        val safeDays = rewardDays.coerceIn(0, 365)
        val safePercent = voucherPercent.coerceIn(0, 100)
        viewModelScope.launch {
            val result = repo.updateReferralSettings(ReferralSettings(safeDays, safePercent, enabled))
            _settingsMessage.value = result.fold(
                onSuccess = { "Pengaturan referral berhasil disimpan" },
                onFailure = { "Gagal menyimpan: ${it.message ?: "unknown error"}" }
            )
        }
    }

    fun clearSettingsMessage() { _settingsMessage.value = null }
}
