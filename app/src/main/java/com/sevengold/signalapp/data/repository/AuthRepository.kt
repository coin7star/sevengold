package com.sevengold.signalapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun register(email: String, password: String, referralCodeInput: String = ""): Result<String> = runCatching {
        val normalizedReferral = referralCodeInput.trim().uppercase()

        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("Gagal membuat akun")

        try {
            val referrerUid = if (normalizedReferral.isNotBlank()) {
                val referralSnap = db.collection("referralCodes")
                    .document(normalizedReferral)
                    .get()
                    .await()
                referralSnap.getString("uid")?.takeIf { it != uid }
                    ?: error("Kode referral tidak ditemukan")
            } else null

            // Kode referral stabil berdasarkan UID, jadi tidak perlu generate random.
            val myReferralCode = "SG${uid.take(8).uppercase()}"
            val referralSettingsSnap = db.collection("appSettings").document("referral").get().await()
            val referralSettings = com.sevengold.signalapp.data.model.ReferralSettings.fromMap(referralSettingsSnap.data)
            val welcomePercent = if (referrerUid != null && referralSettings.enabled) referralSettings.welcomeVoucherPercent else 0
            val welcomeVoucherCode = if (referrerUid != null && welcomePercent > 0) {
                "WELCOME${welcomePercent}-${uid.take(6).uppercase()}"
            } else ""
            val newUser = AppUser(
                uid = uid,
                email = email,
                role = Role.USER,
                referralCode = myReferralCode,
                referredByUid = referrerUid,
                welcomeVoucherCode = welcomeVoucherCode,
                welcomeVoucherPercent = welcomePercent
            )

            val batch = db.batch()
            batch.set(db.collection("users").document(uid), newUser.toMap())
            batch.set(
                db.collection("referralCodes").document(myReferralCode),
                mapOf("uid" to uid, "createdAt" to System.currentTimeMillis())
            )
            batch.commit().await()
        } catch (e: Exception) {
            // Jangan tinggalkan akun Auth setengah jadi kalau profil/referral gagal dibuat.
            runCatching { result.user?.delete()?.await() }
            throw e
        }

        uid
    }

    suspend fun login(email: String, password: String): Result<String> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("Gagal login")
        ensureReferralData(uid, result.user?.email ?: email)
        uid
    }

    private suspend fun ensureReferralData(uid: String, email: String) {
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()
        if (!snap.exists()) return

        val referralCode = snap.getString("referralCode")
            ?.takeIf { it.isNotBlank() }
            ?: "SG${uid.take(8).uppercase()}"
        val isReferred = !snap.getString("referredByUid").isNullOrBlank()
        val referralSettingsSnap = db.collection("appSettings").document("referral").get().await()
        val referralSettings = com.sevengold.signalapp.data.model.ReferralSettings.fromMap(referralSettingsSnap.data)
        val fallbackPercent = if (isReferred && referralSettings.enabled) referralSettings.welcomeVoucherPercent else 0
        val existingVoucherPercent = snap.getLong("welcomeVoucherPercent")?.toInt()
        val voucherPercent = existingVoucherPercent ?: fallbackPercent
        val voucherCode = snap.getString("welcomeVoucherCode")
            ?.takeIf { it.isNotBlank() }
            ?: if (isReferred && voucherPercent > 0) "WELCOME${voucherPercent}-${uid.take(6).uppercase()}" else ""

        val updates = mutableMapOf<String, Any>(
            "referralCode" to referralCode,
            "welcomeVoucherCode" to voucherCode,
            "welcomeVoucherPercent" to voucherPercent,
            "welcomeVoucherUsed" to (snap.getBoolean("welcomeVoucherUsed") ?: false)
        )
        if (snap.getString("email").isNullOrBlank() && email.isNotBlank()) {
            updates["email"] = email
        }
        ref.update(updates).await()

        // Jangan menimpa dokumen referral code yang sudah ada.
        // Firestore rules hanya mengizinkan user membuat kode miliknya sendiri,
        // sehingga login ulang tidak boleh berubah menjadi operasi UPDATE.
        val referralRef = db.collection("referralCodes").document(referralCode)
        val referralSnap = referralRef.get().await()
        if (!referralSnap.exists()) {
            referralRef.set(
                mapOf(
                    "uid" to uid,
                    "createdAt" to (snap.getLong("createdAt") ?: System.currentTimeMillis())
                )
            ).await()
        } else if (referralSnap.getString("uid") != uid) {
            error("Kode referral user tidak valid")
        }
    }

    fun logout() = auth.signOut()
}
