package com.sevengold.signalapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.sevengold.signalapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AdminTelegramRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun action(action: String): Result<AdminTelegramResult> = runCatching {
        val user = auth.currentUser ?: error("Sesi admin tidak ditemukan")
        val token = user.getIdToken(false).await().token ?: error("Token admin tidak tersedia")
        withContext(Dispatchers.IO) {
            val url = URL(BuildConfig.PUSH_WEBHOOK_URL.trimEnd('/') + "/admin/telegram")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { it.write(JSONObject().put("action", action).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            val json = runCatching { JSONObject(body) }.getOrElse { JSONObject() }
            if (status !in 200..299 || json.optBoolean("ok", false).not()) {
                error(json.optString("error").ifBlank { "Permintaan Telegram gagal (HTTP $status)" })
            }
            AdminTelegramResult(
                premium = json.optInt("premium", 0),
                connected = json.optInt("connected", 0),
                disconnected = json.optInt("disconnected", 0),
                sent = json.optInt("sent", 0),
                failed = json.optInt("failed", 0),
                sentPush = json.optInt("sentPush", 0),
                sentTelegram = json.optInt("sentTelegram", 0),
                skipped = json.optInt("skipped", 0),
                total = json.optInt("total", 0)
            )
        }
    }
}

data class AdminTelegramResult(
    val premium: Int = 0,
    val connected: Int = 0,
    val disconnected: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
    // Khusus hasil "test_expiry_reminder": beda skema dari testme/testsignal
    // karena reminder H-1 punya dua channel (push + Telegram) sekaligus.
    val sentPush: Int = 0,
    val sentTelegram: Int = 0,
    val skipped: Int = 0,
    val total: Int = 0
)
