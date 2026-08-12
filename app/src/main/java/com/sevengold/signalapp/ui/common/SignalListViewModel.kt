package com.sevengold.signalapp.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.repository.SignalRepository
import com.sevengold.signalapp.notification.PremiumPushGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SignalListViewModel(
    private val repo: SignalRepository = SignalRepository()
) : ViewModel() {

    private val _signals = MutableStateFlow<List<Signal>>(emptyList())
    val signals: StateFlow<List<Signal>> = _signals

    init {
        viewModelScope.launch {
            repo.observeSignals()
                // Kalau user logout, Firestore balikin error permission-denied ke listener ini.
                // Ditangkap di sini supaya tidak nge-crash app (layar putih) pas keluar.
                .catch { }
                .collect { _signals.value = it }
        }
    }

    fun publish(signal: Signal, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.publish(signal)
            if (result.isFailure) {
                onDone(false, result.exceptionOrNull()?.message)
                return@launch
            }

            // Firestore tetap menjadi sumber data utama. Push dikirim setelah write sukses.
            val pushResult = PremiumPushGateway.notifySignal("SIGNAL_CREATED", signal)
            onDone(
                true,
                if (pushResult.isSuccess) null
                else "Sinyal tersimpan, tetapi notifikasi belum terkirim: ${pushResult.exceptionOrNull()?.message ?: "webhook tidak merespons"}"
            )
        }
    }


    fun deleteSignal(signal: Signal, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.deleteSignal(signal.id)
            onDone(
                result.isSuccess,
                result.exceptionOrNull()?.message
            )
        }
    }

    fun deleteCancelledSignals(onDone: (Boolean, Int, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.deleteCancelledSignals()
            if (result.isSuccess) {
                onDone(true, result.getOrDefault(0), null)
            } else {
                onDone(false, 0, result.exceptionOrNull()?.message)
            }
        }
    }

    fun updateStatus(signal: Signal, status: SignalStatus, onDone: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repo.updateStatus(signal.id, status)
            if (result.isFailure) {
                onDone?.invoke(false, result.exceptionOrNull()?.message)
                return@launch
            }

            val updated = signal.copy(status = status)
            val event = when (status) {
                SignalStatus.TP_HIT -> "TP_HIT"
                SignalStatus.SL_HIT -> "SL_HIT"
                SignalStatus.BE -> "BE"
                SignalStatus.CANCELLED -> "CANCELLED"
                SignalStatus.ACTIVE -> "SIGNAL_ACTIVE"
            }
            val pushResult = PremiumPushGateway.notifySignal(event, updated)
            onDone?.invoke(
                true,
                if (pushResult.isSuccess) null
                else "Status tersimpan, tetapi notifikasi belum terkirim: ${pushResult.exceptionOrNull()?.message ?: "webhook tidak merespons"}"
            )
        }
    }
}
