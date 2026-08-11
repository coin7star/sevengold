package com.sevengold.signalapp.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.sevengold.signalapp.BuildConfig
import com.sevengold.signalapp.data.model.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mengirim event sinyal dari Admin Panel ke webhook push eksternal.
 *
 * Credential FCM tidak pernah masuk ke APK. APK hanya membawa URL webhook publik
 * dan Firebase ID token admin. Worker memvalidasi token + UID admin sebelum mengirim FCM.
 */
object PremiumPushGateway {
    private const val TAG = "PremiumPush"
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 8_000

    suspend fun notifySignal(event: String, signal: Signal): Result<Unit> = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.PUSH_WEBHOOK_URL.trim()
        if (endpoint.isBlank()) {
            return@withContext Result.failure(IllegalStateException("URL webhook push belum dikonfigurasi"))
        }

        val user = FirebaseAuth.getInstance().currentUser
            ?: return@withContext Result.failure(IllegalStateException("Sesi administrator tidak ditemukan"))

        try {
            val token = user.getIdToken(false).await().token
                ?: return@withContext Result.failure(IllegalStateException("Firebase ID token tidak tersedia"))

            val payload = JSONObject().apply {
                put("event", event)
                put("signalId", signal.id)
                put("pair", signal.pair)
                put("type", signal.type.name)
                put("entry", signal.entry)
                put("tp", signal.tp)
                put("sl", signal.sl)
                put("status", signal.status.name)
                put("note", signal.note)
                put("createdAt", signal.createdAt)
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $token")
            }

            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val responseText = runCatching {
                (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            connection.disconnect()

            if (code !in 200..299) {
                Log.e(TAG, "Webhook push gagal HTTP $code: $responseText")
                return@withContext Result.failure(IllegalStateException("Webhook push gagal (HTTP $code)"))
            }

            Log.d(TAG, "Webhook push berhasil: event=$event response=$responseText")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Webhook push exception", t)
            Result.failure(t)
        }
    }
}
