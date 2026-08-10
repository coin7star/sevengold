package com.sevengold.signalapp.data.model

data class ReferralSettings(
    val rewardPremiumDays: Int = 2,
    val welcomeVoucherPercent: Int = 10,
    val enabled: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "rewardPremiumDays" to rewardPremiumDays,
        "welcomeVoucherPercent" to welcomeVoucherPercent,
        "enabled" to enabled
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): ReferralSettings {
            if (map == null) return ReferralSettings()
            return ReferralSettings(
                rewardPremiumDays = (map["rewardPremiumDays"] as? Number)?.toInt() ?: 2,
                welcomeVoucherPercent = (map["welcomeVoucherPercent"] as? Number)?.toInt() ?: 10,
                enabled = map["enabled"] as? Boolean ?: true
            )
        }
    }
}
