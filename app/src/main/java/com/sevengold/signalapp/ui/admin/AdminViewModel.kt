package com.sevengold.signalapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.model.SubscriptionCode
import com.sevengold.signalapp.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _codes = MutableStateFlow<List<SubscriptionCode>>(emptyList())
    val codes: StateFlow<List<SubscriptionCode>> = _codes

    private val _lastGeneratedCode = MutableStateFlow<String?>(null)
    val lastGeneratedCode: StateFlow<String?> = _lastGeneratedCode

    init {
        viewModelScope.launch {
            repo.observeCodes().collect { _codes.value = it }
        }
    }

    fun generateCode(durationDays: Int, adminUid: String) {
        viewModelScope.launch {
            val result = repo.createCode(durationDays, adminUid)
            _lastGeneratedCode.value = result.getOrNull()
        }
    }
}
