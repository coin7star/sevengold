package com.sevengold.signalapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.model.SubscriptionOrder
import com.sevengold.signalapp.data.model.SubscriptionOrderStatus
import com.sevengold.signalapp.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SubscriptionAdminViewModel(private val repo: SubscriptionRepository = SubscriptionRepository()) : ViewModel() {
    private val _orders = MutableStateFlow<List<SubscriptionOrder>>(emptyList())
    val orders: StateFlow<List<SubscriptionOrder>> = _orders
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    init { viewModelScope.launch { repo.observePendingOrders().catch { }.collect { _orders.value = it } } }
    fun approve(orderId: String) { set(orderId, SubscriptionOrderStatus.APPROVED) }
    fun reject(orderId: String) { set(orderId, SubscriptionOrderStatus.REJECTED) }
    private fun set(id: String, status: SubscriptionOrderStatus) = viewModelScope.launch {
        val r = repo.setOrderStatus(id, status)
        _message.value = r.fold({ "Pesanan ${status.name.lowercase()}" }, { "Gagal: ${it.message ?: "unknown error"}" })
    }
}
