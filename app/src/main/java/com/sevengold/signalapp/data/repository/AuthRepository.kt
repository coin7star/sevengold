package com.sevengold.signalapp.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import kotlinx.coroutines.tasks.await


data class RegistrationResult(
    val uid: String,
    val voucherCode: String = "",
    val voucherPercent: Int = 0
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        val normalizedEmail = email.trim()
        require(normalizedEmail.isNotBlank()) { "Email wajib diisi" }
        auth.sendPasswordResetEmail(normalizedEmail).await()
    }

    suspend fun register(email: String, password: String, referralCodeInput: String = ""): Result<RegistrationResult> = runCatching {
        val normalizedReferral = referralCodeInput.trim().uppercase()

        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("Gagal membuat akun")
        var welcomePercent = 0
        var welcomeVoucherCode = ""

        try {
            val referrerUid = if (normalizedReferral.isNotBlank()) {
                val referralSnap = db.collection("referralCodes")
                    .document(normalizedReferral)
                    .get()
                    .await()
                referralSnap.getString("uid")?.takeIf { it != uid }
                    ?: error("Kode referral tidak ditemukan")
            } else null

            val myReferralCode = "SG${uid.take(8).uppercase()}"
            val referralSettingsSnap = db.collection("appSettings").document("referral").get().await()
            val referralSettings = com.sevengold.signalapp.data.model.ReferralSettings.fromMap(referralSettingsSnap.data)
            welcomePercent = if (referrerUid != null && referralSettings.enabled) referralSettings.welcomeVoucherPercent else 0
            welcomeVoucherCode = if (referrerUid != null && welcomePercent > 0) {
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
            runCatching { result.user?.delete()?.await() }
            throw e
        }

        RegistrationResult(uid = uid, voucherCode = welcomeVoucherCode, voucherPercent = welcomePercent)
    }

    suspend fun login(email: String, password: String): Result<String> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("Gagal login")
        ensureReferralData(uid, result.user?.email ?: email)
        uid
    }

    /**
     * Login/daftar menggunakan Google Sign-In Play Services.
     * Jalur ini sengaja dipakai sebagai satu-satunya UI Google Login agar user
     * selalu mendapatkan layar pemilih akun Google yang familiar dan dapat
     * memilih akun secara eksplisit.
     * Jalur Google Sign-In Play Services membuka pemilih akun Google bawaan Android,
     * lalu meneruskan ID token hasilnya ke Firebase Authentication.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = runCatching {
        completeFirebaseSignIn(idToken)
    }

    /** Membuat Intent untuk membuka layar pilih akun Google versi klasik (non-Credential Manager). */
    fun buildLegacyGoogleSignInIntent(context: Context): Intent {
        val webClientId = resolveWebClientId(context)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        // Selalu minta pilih akun dari awal (bukan auto pakai akun terakhir), supaya user
        // punya kesempatan ganti akun kalau diperlukan.
        GoogleSignIn.getClient(context, gso).signOut()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    private fun resolveWebClientId(context: Context): String {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        val clientId = resourceId.takeIf { it != 0 }
            ?.let { context.getString(it).trim() }
            .orEmpty()

        if (clientId.isBlank() || clientId.contains("YOUR_WEB_CLIENT_ID")) {
            error(
                "Google Login belum dikonfigurasi: Web OAuth Client ID tidak ditemukan. " +
                    "Pastikan google-services.json terbaru memiliki oauth_client client_type=3."
            )
        }
        return clientId
    }

    private suspend fun completeFirebaseSignIn(idToken: String): String {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = try {
            auth.signInWithCredential(firebaseCredential).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            error("Email ini sudah terdaftar dengan metode login lain. Gunakan login email/password terlebih dahulu.")
        }

        val firebaseUser = authResult.user ?: error("Gagal login dengan Google")
        ensureUserProfile(firebaseUser.uid, firebaseUser.email.orEmpty(), firebaseUser.displayName.orEmpty())
        return firebaseUser.uid
    }


    /**
     * Ensures an authenticated user's Firestore profile contains the referral
     * fields expected by the current app version. Existing role/premium data
     * is preserved.
     */
    private suspend fun ensureReferralData(uid: String, email: String) {
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()

        if (!snap.exists()) {
            val referralCode = "SG${uid.take(8).uppercase()}"
            val user = AppUser(
                uid = uid,
                email = email,
                role = Role.USER,
                referralCode = referralCode
            )
            db.runBatch { batch ->
                batch.set(ref, user.toMap())
                batch.set(
                    db.collection("referralCodes").document(referralCode),
                    mapOf(
                        "uid" to uid,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
            }.await()
            return
        }

        val data = snap.data ?: emptyMap()
        val updates = mutableMapOf<String, Any>()
        if (data["email"] !is String || (data["email"] as String).isBlank()) {
            updates["email"] = email
        }

        val existingReferralCode = data["referralCode"] as? String ?: ""
        if (existingReferralCode.isBlank()) {
            val referralCode = "SG${uid.take(8).uppercase()}"
            updates["referralCode"] = referralCode
            db.collection("referralCodes")
                .document(referralCode)
                .set(
                    mapOf(
                        "uid" to uid,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
                .await()
        }

        if (updates.isNotEmpty()) {
            ref.update(updates).await()
        }
    }

    /**
     * Ensures a Google-authenticated user has a Firestore profile without
     * overwriting an existing role, premium expiry, referral or voucher data.
     */
    private suspend fun ensureUserProfile(uid: String, email: String, displayName: String) {
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()

        if (!snap.exists()) {
            val referralCode = "SG${uid.take(8).uppercase()}"
            val user = AppUser(
                uid = uid,
                email = email,
                role = Role.USER,
                referralCode = referralCode
            )
            db.runBatch { batch ->
                batch.set(ref, user.toMap())
                batch.set(
                    db.collection("referralCodes").document(referralCode),
                    mapOf(
                        "uid" to uid,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
            }.await()
            return
        }

        val data = snap.data ?: emptyMap()
        val updates = mutableMapOf<String, Any>()

        if ((data["email"] as? String).isNullOrBlank() && email.isNotBlank()) {
            updates["email"] = email
        }

        // Keep this field only when the project already uses it; it is useful
        // for displaying a friendly profile name but never changes role/entitlement.
        if (displayName.isNotBlank() && (data["displayName"] as? String).isNullOrBlank()) {
            updates["displayName"] = displayName
        }

        if ((data["referralCode"] as? String).isNullOrBlank()) {
            val referralCode = "SG${uid.take(8).uppercase()}"
            updates["referralCode"] = referralCode
            db.collection("referralCodes")
                .document(referralCode)
                .set(
                    mapOf(
                        "uid" to uid,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
                .await()
        }

        if (updates.isNotEmpty()) {
            ref.update(updates).await()
        }
    }



}
