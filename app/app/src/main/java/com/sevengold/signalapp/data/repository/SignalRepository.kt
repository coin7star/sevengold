package com.sevengold.signalapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SignalRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection("signals")

    /** Real-time list sinyal terbaru dulu. Dipakai baik oleh PREMIUM maupun USER (blur di sisi UI). */
    fun observeSignals(): Flow<List<Signal>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val signals = snapshot?.documents?.map { Signal.fromMap(it.id, it.data) } ?: emptyList()
                trySend(signals)
            }
        awaitClose { registration.remove() }
    }

    suspend fun publish(signal: Signal): Result<Unit> = runCatching {
        collection.document().set(signal.toMap()).await()
        Unit
    }

    suspend fun updateStatus(signalId: String, status: SignalStatus): Result<Unit> = runCatching {
        collection.document(signalId).update("status", status.name).await()
        Unit
    }
}
