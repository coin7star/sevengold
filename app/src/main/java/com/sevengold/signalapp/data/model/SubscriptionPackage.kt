package com.sevengold.signalapp.data.model

data class SubscriptionPackage(
    val id: String = "",
    val name: String = "",
    val price: Long = 0L,
    val durationDays: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    val sortOrder: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "price" to price,
        "durationDays" to durationDays,
        "label" to label,
        "enabled" to enabled,
        "sortOrder" to sortOrder
    )

    companion object {
        fun defaults(): List<SubscriptionPackage> = listOf(
            SubscriptionPackage("starter", "Starter", 10_000, 7, "", true, 0),
            SubscriptionPackage("basic", "Basic", 15_000, 10, "", true, 1),
            SubscriptionPackage("pro", "Pro", 30_000, 20, "", true, 2),
            SubscriptionPackage("vip", "VIP", 50_000, 30, "BEST VALUE", true, 3)
        )

        fun fromMap(map: Map<String, Any?>?): SubscriptionPackage {
            if (map == null) return SubscriptionPackage()
            return SubscriptionPackage(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                price = (map["price"] as? Number)?.toLong() ?: 0L,
                durationDays = (map["durationDays"] as? Number)?.toInt() ?: 0,
                label = map["label"] as? String ?: "",
                enabled = map["enabled"] as? Boolean ?: true,
                sortOrder = (map["sortOrder"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
