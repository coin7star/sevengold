package com.sevengold.signalapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sevengold.signalapp.data.model.SubscriptionCode
import com.sevengold.signalapp.data.model.ReferralSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class AdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection("subscriptionCodes")
    private val referralSettingsRef = db.collection("appSettings").document("referral")

    fun observeCodes(): Flow<List<SubscriptionCode>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { SubscriptionCode.fromMap(it.data) } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeReferralSettings(): Flow<ReferralSettings> = callbackFlow {
        val registration = referralSettingsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(ReferralSettings.fromMap(snapshot?.data))
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateReferralSettings(settings: ReferralSettings): Result<Unit> = runCatching {
        referralSettingsRef.set(settings.toMap()).await()
    }

    /** Generate kode acak 8 karakter, misal "K7X9QF2A", lalu simpan ke Firestore. */
    suspend fun createCode(durationDays: Int, createdByUid: String): Result<String> = runCatching {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")

        val subCode = SubscriptionCode(
            code = code,
            durationDays = durationDays,
            createdBy = createdByUid
        )
        collection.document(code).set(subCode.toMap()).await()
        code
    }
}
