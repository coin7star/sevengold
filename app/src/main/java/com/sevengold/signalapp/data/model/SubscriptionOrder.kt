package com.sevengold.signalapp.data.model

enum class SubscriptionOrderStatus { PENDING, APPROVED, REJECTED }

data class SubscriptionOrder(
    val id: String = "",
    val uid: String = "",
    val email: String = "",
    val packageId: String = "",
    val packageName: String = "",
    val price: Long = 0L,
    val durationDays: Int = 0,
    val status: SubscriptionOrderStatus = SubscriptionOrderStatus.PENDING,
    val createdAt: Long = 0L,
    val approvedAt: Long? = null,
    val rejectedAt: Long? = null,
    val adminNote: String = ""
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): SubscriptionOrder {
            if (map == null) return SubscriptionOrder(id = id)
            return SubscriptionOrder(
                id = id,
                uid = map["uid"] as? String ?: "",
                email = map["email"] as? String ?: "",
                packageId = map["packageId"] as? String ?: "",
                packageName = map["packageName"] as? String ?: "",
                price = (map["price"] as? Number)?.toLong() ?: 0L,
                durationDays = (map["durationDays"] as? Number)?.toInt() ?: 0,
                status = runCatching { SubscriptionOrderStatus.valueOf(map["status"] as? String ?: "PENDING") }.getOrDefault(SubscriptionOrderStatus.PENDING),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                approvedAt = (map["approvedAt"] as? Number)?.toLong(),
                rejectedAt = (map["rejectedAt"] as? Number)?.toLong(),
                adminNote = map["adminNote"] as? String ?: ""
            )
        }
    }
}
