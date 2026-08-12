package com.sevengold.signalapp.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local notification inbox. Keeps the latest 100 signal events on-device. */
data class AppNotification(
    val id: Long,
    val event: String,
    val title: String,
    val body: String,
    val pair: String = "",
    val type: String = "",
    val entry: String = "",
    val tp: String = "",
    val sl: String = "",
    val signalId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

class NotificationCenterStore(context: Context) {
    private val prefs = context.getSharedPreferences("notification_center", Context.MODE_PRIVATE)

    fun add(item: AppNotification) {
        val current = readAll().toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        val trimmed = current.take(100)
        prefs.edit().putString(KEY_ITEMS, JSONArray().apply {
            trimmed.forEach { put(toJson(it)) }
        }.toString()).apply()
    }

    fun readAll(): List<AppNotification> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i)))
            }
        }.getOrDefault(emptyList())
    }

    fun markRead(id: Long) = update { it.copy(read = if (it.id == id) true else it.read) }
    fun markAllRead() = update { it.copy(read = true) }
    fun clear() { prefs.edit().remove(KEY_ITEMS).apply() }

    private fun update(transform: (AppNotification) -> AppNotification) {
        val updated = readAll().map(transform)
        prefs.edit().putString(KEY_ITEMS, JSONArray().apply { updated.forEach { put(toJson(it)) } }.toString()).apply()
    }

    private fun toJson(item: AppNotification) = JSONObject().apply {
        put("id", item.id); put("event", item.event); put("title", item.title); put("body", item.body)
        put("pair", item.pair); put("type", item.type); put("entry", item.entry); put("tp", item.tp); put("sl", item.sl)
        put("signalId", item.signalId); put("timestamp", item.timestamp); put("read", item.read)
    }

    private fun fromJson(o: JSONObject) = AppNotification(
        id = o.optLong("id"), event = o.optString("event"), title = o.optString("title"), body = o.optString("body"),
        pair = o.optString("pair"), type = o.optString("type"), entry = o.optString("entry"), tp = o.optString("tp"),
        sl = o.optString("sl"), signalId = o.optString("signalId"), timestamp = o.optLong("timestamp"), read = o.optBoolean("read")
    )

    companion object { private const val KEY_ITEMS = "items" }
}
