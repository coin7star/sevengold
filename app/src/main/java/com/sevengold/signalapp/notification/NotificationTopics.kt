package com.sevengold.signalapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

const val TOPIC_PREMIUM_SIGNALS = "premium_signals"
const val NOTIFICATION_CHANNEL_ID = "premium_signals"

object NotificationTopics {
    private const val TAG = "PremiumPush"

    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Update Sinyal Premium",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi sinyal baru, TP, SL, BE, dan pembatalan untuk member Premium"
                setShowBadge(true)
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun subscribeToPremiumSignals() {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_PREMIUM_SIGNALS)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "Berhasil subscribe topic $TOPIC_PREMIUM_SIGNALS")
                else Log.e(TAG, "Gagal subscribe topic $TOPIC_PREMIUM_SIGNALS", task.exception)
            }
    }

    fun unsubscribeFromPremiumSignals() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC_PREMIUM_SIGNALS)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "Berhasil unsubscribe topic $TOPIC_PREMIUM_SIGNALS")
                else Log.e(TAG, "Gagal unsubscribe topic $TOPIC_PREMIUM_SIGNALS", task.exception)
            }
    }
}
