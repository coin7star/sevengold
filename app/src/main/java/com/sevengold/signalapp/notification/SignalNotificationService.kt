package com.sevengold.signalapp.notification

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sevengold.signalapp.MainActivity
import com.sevengold.signalapp.R

/**
 * Nerima pesan FCM yang dikirim Cloud Function tiap ada sinyal baru / status berubah
 * (TP, SL, BE, Cancel), lalu nampilin sebagai notifikasi system di HP.
 *
 * Kalau app lagi di background/killed, notification payload (bukan data-only) sudah
 * otomatis ditampilkan sistem tanpa lewat sini — method ini kepanggil terutama waktu
 * app lagi dibuka/foreground.
 */
class SignalNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Update Sinyal"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        NotificationTopics.createChannelIfNeeded(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    // Tidak perlu handle onNewToken karena kita pakai topic messaging (subscribeToTopic),
    // bukan kirim ke token device satu-satu.
}
