package com.sevengold.signalapp.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.repository.SignalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignalListViewModel(
    private val repo: SignalRepository = SignalRepository()
) : ViewModel() {

    private val _signals = MutableStateFlow<List<Signal>>(emptyList())
    val signals: StateFlow<List<Signal>> = _signals

    init {
        viewModelScope.launch {
            repo.observeSignals().collect { _signals.value = it }
        }
    }

    fun publish(signal: Signal, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.publish(signal)
            onDone(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun updateStatus(signalId: String, status: SignalStatus) {
        viewModelScope.launch { repo.updateStatus(signalId, status) }
    }
}
