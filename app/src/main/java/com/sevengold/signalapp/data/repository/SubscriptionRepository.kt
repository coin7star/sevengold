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

    suspend fun createOrder(uid: String, email: String, pkg: SubscriptionPackage, voucherCode: String = ""): Result<String> =
        UserRepository(db).createSubscriptionOrder(uid, email, pkg, voucherCode)

    fun observePendingOrders(): Flow<List<SubscriptionOrder>> = callbackFlow {
        val registration = orders.whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.map { SubscriptionOrder.fromMap(it.id, it.data) }?.sortedBy { it.createdAt } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /**
     * Admin approval is intentionally handled atomically in the Android admin client for the
     * early/manual phase. This means the app does NOT depend on a Cloud Function trigger just
     * to activate Premium. Cloud Function remains as a server-side backup for environments that
     * deploy it, but approvalProcessedAt makes the operation idempotent so it cannot double-credit.
     */
    suspend fun setOrderStatus(orderId: String, status: SubscriptionOrderStatus, note: String = ""): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val orderRef = orders.document(orderId)

        db.runTransaction { tx ->
            val orderSnap = tx.get(orderRef)
            if (!orderSnap.exists()) error("Pesanan tidak ditemukan")
            val order = orderSnap.data ?: error("Data pesanan kosong")
            val currentStatus = order["status"] as? String ?: "PENDING"

            if (status == SubscriptionOrderStatus.APPROVED) {
                if (currentStatus == "APPROVED" || order["approvalProcessedAt"] != null) {
                    error("Pesanan ini sudah diproses")
                }
                if (currentStatus != "PENDING") error("Pesanan sudah tidak menunggu approval")

                val uid = order["uid"] as? String ?: error("UID pesanan tidak valid")
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) error("User pesanan tidak ditemukan")
                val user = userSnap.data ?: emptyMap()

                val referrerUid = user["referredByUid"] as? String
                val referrerRef = referrerUid?.takeIf { it.isNotBlank() && it != uid }
                    ?.let { db.collection("users").document(it) }
                val referrerSnap = referrerRef?.let { tx.get(it) }
                val settingsRef = db.collection("appSettings").document("referral")
                val settingsSnap = tx.get(settingsRef)
                val packageRef = db.collection("appSettings").document("subscriptionPackages")
                val packageSnap = tx.get(packageRef)

                val durationDays = ((order["durationDays"] as? Number)?.toLong() ?: 0L)
                    .coerceIn(1L, 3650L)
                val durationMillis = durationDays * 24L * 60L * 60L * 1000L

                // Approval manual: identitas paket + durasi harus cocok dengan katalog aktif.
                // Harga TIDAK dibandingkan lagi dengan order lama karena admin bisa mengubah
                // harga paket setelah order dibuat. Untuk pembayaran manual, admin tetap
                // memverifikasi nominal transfer sebelum menekan APPROVE.
                val packageId = order["packageId"] as? String ?: error("Paket pesanan tidak valid")
                val configuredPackages = packageSnap.get("packages") as? List<*>
                val configuredPackage = configuredPackages
                    ?.mapNotNull { it as? Map<*, *> }
                    ?.firstOrNull { pkg ->
                        pkg["id"] == packageId &&
                            pkg["enabled"] != false &&
                            (pkg["durationDays"] as? Number)?.toLong() == durationDays
                    }
                if (configuredPackage == null) error("Paket tidak ditemukan / sudah dinonaktifkan")

                val currentExpiry = (user["premiumExpiryMillis"] as? Number)?.toLong() ?: 0L
                val base = if (currentExpiry > now) currentExpiry else now
                val newExpiry = base + durationMillis
                val userRole = user["role"] as? String ?: "USER"

                val voucherCode = (order["voucherCode"] as? String)?.trim()?.uppercase().orEmpty()
                val userVoucher = (user["welcomeVoucherCode"] as? String)?.trim()?.uppercase().orEmpty()
                val voucherValid = voucherCode.isNotBlank() && voucherCode == userVoucher && user["welcomeVoucherUsed"] != true

                tx.update(userRef, mapOf(
                    "role" to if (userRole == "ADMIN") "ADMIN" else "PREMIUM",
                    "premiumExpiryMillis" to if (userRole == "ADMIN") currentExpiry else newExpiry,
                    "lastSubscriptionActivatedAt" to now,
                    "lastSubscriptionOrderId" to orderId,
                    "welcomeVoucherUsed" to if (voucherValid) true else (user["welcomeVoucherUsed"] ?: false)
                ))

                // Referral reward juga diproses di transaction ini supaya fase manual tidak
                // bergantung pada Cloud Function. Kalau function aktif, flag ini membuatnya idempotent.
                if (referrerRef != null && referrerSnap?.exists() == true && user["referralRewardGranted"] != true) {
                    val settings = settingsSnap?.data ?: emptyMap()
                    val enabled = settings["enabled"] != false
                    val rewardDays = ((settings["rewardPremiumDays"] as? Number)?.toLong() ?: 2L).coerceIn(0L, 365L)
                    val referrer = referrerSnap.data ?: emptyMap()
                    if (enabled && rewardDays > 0L) {
                        val bonusMillis = rewardDays * 24L * 60L * 60L * 1000L
                        val refExpiry = (referrer["premiumExpiryMillis"] as? Number)?.toLong() ?: 0L
                        val refBase = if (refExpiry > now) refExpiry else now
                        val refRole = referrer["role"] as? String ?: "USER"
                        tx.update(referrerRef, mapOf(
                            "role" to if (refRole == "ADMIN") "ADMIN" else "PREMIUM",
                            "premiumExpiryMillis" to if (refRole == "ADMIN") refExpiry else refBase + bonusMillis,
                            "referralSuccessfulCount" to ((referrer["referralSuccessfulCount"] as? Number)?.toLong() ?: 0L) + 1L,
                            "referralRewardDaysEarned" to ((referrer["referralRewardDaysEarned"] as? Number)?.toLong() ?: 0L) + rewardDays,
                            "lastReferralRewardAt" to now
                        ))
                        tx.update(userRef, mapOf(
                            "referralRewardGranted" to true,
                            "referralRewardGrantedAt" to now
                        ))
                    }
                }

                tx.update(orderRef, mapOf(
                    "status" to "APPROVED",
                    "adminNote" to note,
                    "approvedAt" to now,
                    "approvalProcessedAt" to now,
                    "processedExpiryMillis" to newExpiry
                ))
            } else if (status == SubscriptionOrderStatus.REJECTED) {
                if (currentStatus != "PENDING") error("Pesanan sudah diproses")
                tx.update(orderRef, mapOf(
                    "status" to "REJECTED",
                    "adminNote" to note,
                    "rejectedAt" to now
                ))
            } else {
                tx.update(orderRef, mapOf("status" to status.name, "adminNote" to note))
            }
            null
        }.await()
    }
}
