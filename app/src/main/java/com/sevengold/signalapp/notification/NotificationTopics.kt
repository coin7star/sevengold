package com.sevengold.signalapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Semua user PREMIUM subscribe ke topic ini supaya Cloud Function cukup kirim
 * SATU pesan ke topic-nya, gak perlu simpen/loop token device satu-satu.
 * Role USER & yang premium-nya sudah expired otomatis unsubscribe.
 */
const val TOPIC_PREMIUM_SIGNALS = "premium_signals"
const val NOTIFICATION_CHANNEL_ID = "premium_signals"

object NotificationTopics {

    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Update Sinyal Premium",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi sinyal baru, TP, SL, BE, dan Cancel untuk member Premium"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun subscribeToPremiumSignals() {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_PREMIUM_SIGNALS)
    }

    fun unsubscribeFromPremiumSignals() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC_PREMIUM_SIGNALS)
    }
}
