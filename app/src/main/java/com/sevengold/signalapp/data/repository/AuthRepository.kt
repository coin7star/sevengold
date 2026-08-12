package com.sevengold.signalapp.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import kotlinx.coroutines.tasks.await

private const val GOOGLE_ID_TOKEN_CREDENTIAL_TYPE = "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL"

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
     * Login/daftar menggunakan akun Google melalui Credential Manager (metode modern).
     * Di sebagian HP/ROM (terutama beberapa unit Xiaomi/MIUI), Credential Manager API
     * ini bisa gagal total walau akun Google ada & konfigurasi Firebase benar. Kalau ini
     * terjadi, gunakan buildLegacyGoogleSignInIntent() + signInWithGoogleIdToken() sebagai
     * jalur cadangan (lihat AuthViewModel & LoginScreen).
     */
    suspend fun loginWithGoogle(context: Context): Result<String> = runCatching {
        val credentialManager = CredentialManager.create(context)
        val clientId = resolveWebClientId(context)

        val googleCredential = getGoogleCredential(
            credentialManager = credentialManager,
            context = context,
            serverClientId = clientId,
            filterAuthorizedAccounts = true
        ) ?: getGoogleCredential(
            credentialManager = credentialManager,
            context = context,
            serverClientId = clientId,
            filterAuthorizedAccounts = false
        ) ?: error("Tidak ada akun Google yang dapat digunakan lewat Credential Manager")

        val googleIdTokenCredential = try {
            GoogleIdTokenCredential.createFrom(googleCredential.data)
        } catch (e: GoogleIdTokenParsingException) {
            throw IllegalStateException("Token Google tidak valid", e)
        }

        completeFirebaseSignIn(googleIdTokenCredential.idToken)
    }

    /**
     * Jalur cadangan (metode Google Sign-In "klasik" / play-services-auth) untuk device
     * yang tidak kompatibel dengan Credential Manager. UI memanggil buildLegacyGoogleSignInIntent()
     * untuk membuka layar pilih akun Google bawaan Android, lalu meneruskan idToken hasilnya ke
     * fungsi ini.
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

    private suspend fun getGoogleCredential(
        credentialManager: CredentialManager,
        context: Context,
        serverClientId: String,
        filterAuthorizedAccounts: Boolean
    ): CustomCredential? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GOOGLE_ID_TOKEN_CREDENTIAL_TYPE
            ) {
                credential
            } else {
                null
            }
        } catch (e: NoCredentialException) {
            // Benar-benar tidak ada akun Google yang cocok untuk opsi ini (misal filter
            // "hanya akun yang sudah pernah dipakai" tidak menemukan apa-apa). Ini normal
            // saat percobaan pertama, makanya di loginWithGoogle() ada percobaan kedua.
            Log.w(
                "AuthRepository",
                "Google Sign-In: tidak ada credential (filterAuthorizedAccounts=$filterAuthorizedAccounts)"
            )
            null
        } catch (e: GetCredentialException) {
            // Ini BUKAN kasus "tidak ada akun" — ini error konfigurasi/jaringan/Play Services.
            // Sebelumnya error ini ditelan (catch Exception -> null) sehingga selalu muncul
            // pesan generik "Tidak ada akun Google yang dapat digunakan" walau akar masalahnya
            // beda. Sekarang kita lempar pesan aslinya supaya kelihatan penyebab sebenarnya.
            Log.e(
                "AuthRepository",
                "Google Sign-In gagal: ${e.javaClass.simpleName} - ${e.message}",
                e
            )
            throw IllegalStateException(
                "Google Sign-In gagal (${e.javaClass.simpleName}): ${e.message ?: "tidak ada detail"}. " +
                    "Cek SHA-1 fingerprint & Web Client ID di Firebase Console, dan pastikan Google Play Services aktif.",
                e
            )
        }
    }

    private suspend fun ensureUserProfile(uid: String, email: String, displayName: String) {
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()

        if (!snap.exists()) {
            val referralCode = "SG${uid.take(8).uppercase()}"
            val createdAt = System.currentTimeMillis()
            val newUser = AppUser(
                uid = uid,
                email = email,
                role = Role.USER,
                createdAt = createdAt,
                referralCode = referralCode
            )
            val batch = db.batch()
            batch.set(ref, newUser.toMap())
            batch.set(
                db.collection("referralCodes").document(referralCode),
                mapOf("uid" to uid, "createdAt" to createdAt)
            )
            batch.commit().await()
            return
        }

        ensureReferralData(uid, email)
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
