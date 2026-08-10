package com.sevengold.signalapp.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.model.SubscriptionOrder
import com.sevengold.signalapp.data.model.SubscriptionPackage
import com.sevengold.signalapp.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SubscriptionViewModel(private val repo: SubscriptionRepository = SubscriptionRepository()) : ViewModel() {
    private val _packages = MutableStateFlow<List<SubscriptionPackage>>(SubscriptionPackage.defaults())
    val packages: StateFlow<List<SubscriptionPackage>> = _packages
    private val _orders = MutableStateFlow<List<SubscriptionOrder>>(emptyList())
    val orders: StateFlow<List<SubscriptionOrder>> = _orders
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch { repo.ensureDefaultPackages() }
        viewModelScope.launch { repo.observePackages().catch { }.collect { _packages.value = it } }
    }

    fun startListeningOrders(uid: String) {
        viewModelScope.launch {
            com.sevengold.signalapp.data.repository.UserRepository().observeMySubscriptionOrders(uid)
                .catch { }
                .collect { _orders.value = it }
        }
    }

    fun createOrder(uid: String, email: String, pkg: SubscriptionPackage, voucherCode: String = "") {
        if (_orders.value.any { it.status.name == "PENDING" }) {
            _message.value = "Masih ada pesanan yang menunggu approval admin."
            return
        }
        viewModelScope.launch {
            val result = repo.createOrder(uid, email, pkg, voucherCode)
            _message.value = result.fold(
                onSuccess = { "Pesanan ${pkg.name} dibuat. Silakan ikuti instruksi pembayaran lalu tunggu approval admin." },
                onFailure = { "Gagal membuat pesanan: ${it.message ?: "unknown error"}" }
            )
        }
    }

    fun clearMessage() { _message.value = null }
}
