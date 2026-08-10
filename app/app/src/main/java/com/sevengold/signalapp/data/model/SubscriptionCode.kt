package com.sevengold.signalapp.data.model

/**
 * Kode langganan yang dibuat ADMIN dari panel admin.
 * USER/PREMIUM redeem kode ini untuk naik jadi PREMIUM atau menambah masa aktifnya.
 */
data class SubscriptionCode(
    val code: String = "",
    val durationDays: Int = 30,
    val isUsed: Boolean = false,
    val usedByUid: String? = null,
    val usedAt: Long? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "code" to code,
        "durationDays" to durationDays,
        "isUsed" to isUsed,
        "usedByUid" to usedByUid,
        "usedAt" to usedAt,
        "createdBy" to createdBy,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): SubscriptionCode {
            if (map == null) return SubscriptionCode()
            return SubscriptionCode(
                code = map["code"] as? String ?: "",
                durationDays = (map["durationDays"] as? Number)?.toInt() ?: 30,
                isUsed = map["isUsed"] as? Boolean ?: false,
                usedByUid = map["usedByUid"] as? String,
                usedAt = (map["usedAt"] as? Number)?.toLong(),
                createdBy = map["createdBy"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
