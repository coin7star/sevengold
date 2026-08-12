package com.sevengold.signalapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sevengold.signalapp.data.model.AppUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Real-time listener untuk profil user yang sedang login (role, expiry, dll). */
    fun observeUser(uid: String): Flow<AppUser> = callbackFlow {
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(AppUser.fromMap(uid, snapshot?.data))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Redeem kode langganan.
     * - Kode harus ada & belum dipakai.
     * - Menambah durasi ke expiry yang lama kalau masih aktif (bukan menimpa),
     *   atau mulai dari sekarang kalau sudah expired/baru pertama kali.
     */
    /**
     * Real-time daftar SEMUA user, dipakai di panel User Management admin.
     * Firestore rules mengizinkan ADMIN membaca semua dokumen di /users.
     */
    fun observeAllUsers(): Flow<List<AppUser>> = callbackFlow {
        val registration = db.collection("users")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.map { AppUser.fromMap(it.id, it.data) } ?: emptyList()
                trySend(users)
            }
        awaitClose { registration.remove() }
    }

    /**
     * ADMIN naikin/turunin role user langsung dari panel (tanpa lewat redeem kode).
     * - Ke PREMIUM: set premiumExpiryMillis = sekarang + durationDays.
     * - Ke USER: role diturunin, premiumExpiryMillis dibiarkan apa adanya (riwayat),
     *   tapi karena role sudah USER, effectiveRole otomatis USER juga.
     */
    suspend fun adminSetRole(uid: String, role: com.sevengold.signalapp.data.model.Role, durationDays: Int? = null): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any?>("role" to role.name)
        if (role == com.sevengold.signalapp.data.model.Role.PREMIUM) {
            val days = durationDays ?: 30
            updates["premiumExpiryMillis"] = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        }
        db.collection("users").document(uid).update(updates).await()
        Unit
    }

    suspend fun redeemCode(uid: String, code: String): Result<Long> = runCatching {
        db.runTransaction { tx ->
            val codeRef = db.collection("subscriptionCodes").document(code)
            val codeSnap = tx.get(codeRef)
            if (!codeSnap.exists()) error("Kode tidak ditemukan")
            if (codeSnap.getBoolean("isUsed") == true) error("Kode sudah pernah dipakai")

            val durationDays = (codeSnap.getLong("durationDays") ?: 30L)
            val durationMillis = TimeUnit.DAYS.toMillis(durationDays)

            val userRef = db.collection("users").document(uid)
            val userSnap = tx.get(userRef)
            val currentExpiry = userSnap.getLong("premiumExpiryMillis") ?: 0L
            val now = System.currentTimeMillis()
            val base = if (currentExpiry > now) currentExpiry else now
            val newExpiry = base + durationMillis

            tx.update(
                userRef,
                mapOf(
                    "role" to "PREMIUM",
                    "premiumExpiryMillis" to newExpiry,
                    "lastSubscriptionActivatedAt" to now
                )
            )
            tx.update(
                codeRef,
                mapOf(
                    "isUsed" to true,
                    "usedByUid" to uid,
                    "usedAt" to now
                )
            )
            newExpiry
        }.await()
    }
    fun observeMySubscriptionOrders(uid: String): Flow<List<com.sevengold.signalapp.data.model.SubscriptionOrder>> = callbackFlow {
        val registration = db.collection("subscriptionOrders")
            .whereEqualTo("uid", uid)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val orders = snapshot?.documents?.map { com.sevengold.signalapp.data.model.SubscriptionOrder.fromMap(it.id, it.data) }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(orders)
            }
        awaitClose { registration.remove() }
    }

    suspend fun createSubscriptionOrder(
        uid: String,
        email: String,
        pkg: com.sevengold.signalapp.data.model.SubscriptionPackage,
        voucherCode: String = ""
    ): Result<String> = runCatching {
        val userSnap = db.collection("users").document(uid).get().await()
        val userVoucher = userSnap.getString("welcomeVoucherCode")?.trim()?.uppercase().orEmpty()
        val storedPercent = (userSnap.getLong("welcomeVoucherPercent") ?: 0L).toInt().coerceIn(0, 100)
        val voucherUsed = userSnap.getBoolean("welcomeVoucherUsed") == true
        val normalizedVoucher = voucherCode.trim().uppercase()
        val validVoucher = normalizedVoucher.isNotBlank() &&
            normalizedVoucher == userVoucher && storedPercent > 0 && !voucherUsed
        val discountPercent = if (validVoucher) storedPercent else 0
        val discountAmount = (pkg.price * discountPercent) / 100L
        val finalPrice = (pkg.price - discountAmount).coerceAtLeast(0L)

        val ref = db.collection("subscriptionOrders").document()
        val now = System.currentTimeMillis()
        ref.set(mapOf(
            "uid" to uid,
            "email" to email,
            "packageId" to pkg.id,
            "packageName" to pkg.name,
            "price" to finalPrice,
            "originalPrice" to pkg.price,
            "discountPercent" to discountPercent,
            "discountAmount" to discountAmount,
            "voucherCode" to if (validVoucher) normalizedVoucher else "",
            "durationDays" to pkg.durationDays,
            "status" to "PENDING",
            "createdAt" to now
        )).await()
        ref.id
    }

    suspend fun createTelegramConnectionCode(uid: String): Result<String> = runCatching {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = java.security.SecureRandom()
        val code = buildString {
            repeat(6) { append(chars[random.nextInt(chars.length)]) }
        }
        val expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)
        db.collection("users").document(uid).update(
            mapOf(
                "telegramConnectionCode" to code,
                "telegramConnectionExpiresAt" to expiresAt
            )
        ).await()
        code
    }

    suspend fun updateTelegramNotificationEvents(uid: String, events: List<String>): Result<Unit> = runCatching {
        db.collection("users").document(uid).update(
            "telegramNotificationEvents", events.distinct()
        ).await()
    }

    suspend fun disconnectTelegram(uid: String): Result<Unit> = runCatching {
        db.collection("users").document(uid).update(
            mapOf(
                "telegramChatId" to com.google.firebase.firestore.FieldValue.delete(),
                "telegramUsername" to com.google.firebase.firestore.FieldValue.delete(),
                "telegramConnectedAt" to com.google.firebase.firestore.FieldValue.delete(),
                "telegramConnectionCode" to com.google.firebase.firestore.FieldValue.delete(),
                "telegramConnectionExpiresAt" to com.google.firebase.firestore.FieldValue.delete(),
                "telegramNotificationEvents" to com.google.firebase.firestore.FieldValue.delete()
            )
        ).await()
    }

}
