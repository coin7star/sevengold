package com.sevengold.signalapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sevengold.signalapp.data.model.SubscriptionOrder
import com.sevengold.signalapp.data.model.SubscriptionOrderStatus
import com.sevengold.signalapp.data.model.SubscriptionPackage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SubscriptionRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val packagesRef = db.collection("appSettings").document("subscriptionPackages")
    private val orders = db.collection("subscriptionOrders")

    fun observePackages(): Flow<List<SubscriptionPackage>> = callbackFlow {
        val registration = packagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val raw = snapshot?.get("packages") as? List<*>
            val parsed = raw?.mapNotNull { (it as? Map<*, *>)?.entries?.associate { e -> e.key.toString() to e.value } }
                ?.map { SubscriptionPackage.fromMap(it) }
                ?.filter { it.enabled && it.durationDays > 0 && it.price > 0 }
                ?.sortedBy { it.sortOrder }
            trySend(parsed ?: SubscriptionPackage.defaults())
        }
        awaitClose { registration.remove() }
    }

    suspend fun ensureDefaultPackages(): Result<Unit> = runCatching {
        val snap = packagesRef.get().await()
        if (!snap.exists()) {
            packagesRef.set(mapOf("packages" to SubscriptionPackage.defaults().map { it.toMap() })).await()
        }
    }

    suspend fun createOrder(uid: String, email: String, pkg: SubscriptionPackage): Result<String> =
        UserRepository(db).createSubscriptionOrder(uid, email, pkg)

    fun observePendingOrders(): Flow<List<SubscriptionOrder>> = callbackFlow {
        val registration = orders.whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { SubscriptionOrder.fromMap(it.id, it.data) }?.sortedBy { it.createdAt } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun setOrderStatus(orderId: String, status: SubscriptionOrderStatus, note: String = ""): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val data = mutableMapOf<String, Any>("status" to status.name, "adminNote" to note)
        if (status == SubscriptionOrderStatus.APPROVED) data["approvedAt"] = now
        if (status == SubscriptionOrderStatus.REJECTED) data["rejectedAt"] = now
        orders.document(orderId).update(data).await()
    }
}
