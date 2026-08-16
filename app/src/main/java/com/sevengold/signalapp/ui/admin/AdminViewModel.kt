package com.sevengold.signalapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.model.SubscriptionCode
import com.sevengold.signalapp.data.model.ReferralSettings
import com.sevengold.signalapp.data.model.SubscriptionPackage
import com.sevengold.signalapp.data.repository.AdminRepository
import com.sevengold.signalapp.data.repository.AdminTelegramRepository
import com.sevengold.signalapp.data.repository.AdminTelegramResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repo: AdminRepository = AdminRepository(),
    private val telegramRepo: AdminTelegramRepository = AdminTelegramRepository()
) : ViewModel() {

    private val _codes = MutableStateFlow<List<SubscriptionCode>>(emptyList())
    val codes: StateFlow<List<SubscriptionCode>> = _codes

    private val _referralSettings = MutableStateFlow(ReferralSettings())
    val referralSettings: StateFlow<ReferralSettings> = _referralSettings

    private val _settingsMessage = MutableStateFlow<String?>(null)
    val settingsMessage: StateFlow<String?> = _settingsMessage

    private val _subscriptionPackages = MutableStateFlow(SubscriptionPackage.defaults())
    val subscriptionPackages: StateFlow<List<SubscriptionPackage>> = _subscriptionPackages

    private val _packageMessage = MutableStateFlow<String?>(null)
    val packageMessage: StateFlow<String?> = _packageMessage

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
        viewModelScope.launch {
            repo.observeSubscriptionPackages()
                .catch { _packageMessage.value = "Gagal memuat paket: ${it.message ?: "unknown error"}" }
                .collect { _subscriptionPackages.value = it }
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
    private val _telegramResult = MutableStateFlow(AdminTelegramResult())
    val telegramResult: StateFlow<AdminTelegramResult> = _telegramResult

    private val _telegramMessage = MutableStateFlow<String?>(null)
    val telegramMessage: StateFlow<String?> = _telegramMessage

    private val _telegramLoading = MutableStateFlow(false)
    val telegramLoading: StateFlow<Boolean> = _telegramLoading

    fun telegramAction(action: String) {
        if (_telegramLoading.value) return
        viewModelScope.launch {
            _telegramLoading.value = true
            val result = telegramRepo.action(action)
            _telegramMessage.value = result.fold(
                onSuccess = { r ->
                    _telegramResult.value = r
                    when (action) {
                        "testme" -> "Test notification berhasil dikirim ke Telegram admin."
                        "testsignal" -> "Test broadcast selesai: ${r.sent} terkirim, ${r.failed} gagal."
                        "test_expiry_reminder" -> "Test reminder H-1 selesai: ${r.sentPush} push & ${r.sentTelegram} Telegram terkirim (${r.skipped} dilewati dari ${r.total} user yang expiry-nya H-1)."
                        else -> "Statistik Telegram diperbarui."
                    }
                },
                onFailure = { "Gagal: ${it.message ?: "permintaan tidak dapat diproses"}" }
            )
            _telegramLoading.value = false
        }
    }

    fun clearTelegramMessage() { _telegramMessage.value = null }


    fun saveSubscriptionPackages(packages: List<SubscriptionPackage>) {
        val normalized = packages.mapIndexed { index, pkg ->
            pkg.copy(
                name = pkg.name.trim(),
                price = pkg.price.coerceAtLeast(0L),
                durationDays = pkg.durationDays.coerceAtLeast(0),
                label = pkg.label.trim(),
                sortOrder = index
            )
        }
        if (normalized.any { it.name.isBlank() || it.price <= 0L || it.durationDays <= 0 }) {
            _packageMessage.value = "Nama, harga, dan durasi semua paket harus valid."
            return
        }
        viewModelScope.launch {
            val result = repo.saveSubscriptionPackages(normalized)
            _packageMessage.value = result.fold(
                onSuccess = { "Paket langganan berhasil disimpan" },
                onFailure = { "Gagal menyimpan paket: ${it.message ?: "unknown error"}" }
            )
        }
    }

    fun clearPackageMessage() { _packageMessage.value = null }
}
