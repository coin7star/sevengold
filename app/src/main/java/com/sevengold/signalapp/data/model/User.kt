package com.sevengold.signalapp.data.model

/**
 * Role hierarki aplikasi:
 * - ADMIN   : bisa publish/ubah sinyal & generate kode langganan
 * - PREMIUM : bisa lihat sinyal secara penuh selama belum expired
 * - USER    : baru daftar, sinyal tampil blur sampai redeem kode
 */
enum class Role {
    ADMIN, PREMIUM, USER;

    companion object {
        fun fromString(value: String?): Role =
            entries.find { it.name == value } ?: USER
    }
}

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val role: Role = Role.USER,
    // Waktu (epoch millis) kapan status premium berakhir. Null = belum pernah premium.
    val premiumExpiryMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Referral: kode pribadi yang bisa dibagikan ke teman.
    val referralCode: String = "",
    val referredByUid: String? = null,
    // Ditandai server setelah teman berhasil berlangganan untuk mencegah reward dobel.
    val referralRewardGranted: Boolean = false,
    val referralSuccessfulCount: Int = 0,
    val referralRewardDaysEarned: Int = 0,
    // Voucher welcome untuk teman baru; default 10% dan dipakai manual saat berlangganan.
    val welcomeVoucherCode: String = "",
    val welcomeVoucherPercent: Int = 0,
    val welcomeVoucherUsed: Boolean = false,
    // Telegram notification connection. These fields are safe client metadata;
    // the bot token itself never lives in the app.
    val telegramChatId: String? = null,
    val telegramUsername: String? = null,
    val telegramConnectedAt: Long? = null,
    val telegramConnectionCode: String = "",
    val telegramConnectionExpiresAt: Long? = null,
    val telegramNotificationEvents: List<String> = emptyList()
) {
    val telegramConnected: Boolean
        get() = !telegramChatId.isNullOrBlank()

    /** PREMIUM dianggap aktif hanya jika role == PREMIUM DAN belum lewat expiry. */
    val isPremiumActive: Boolean
        get() = role == Role.PREMIUM && (premiumExpiryMillis ?: 0L) > System.currentTimeMillis()

    /** Role efektif yang dipakai untuk routing UI (auto-turun ke USER kalau expired). */
    val effectiveRole: Role
        get() = when {
            role == Role.ADMIN -> Role.ADMIN
            role == Role.PREMIUM && isPremiumActive -> Role.PREMIUM
            else -> Role.USER
        }

    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "role" to role.name,
        "premiumExpiryMillis" to premiumExpiryMillis,
        "createdAt" to createdAt,
        "referralCode" to referralCode,
        "referredByUid" to referredByUid,
        "referralRewardGranted" to referralRewardGranted,
        "referralSuccessfulCount" to referralSuccessfulCount,
        "referralRewardDaysEarned" to referralRewardDaysEarned,
        "welcomeVoucherCode" to welcomeVoucherCode,
        "welcomeVoucherPercent" to welcomeVoucherPercent,
        "welcomeVoucherUsed" to welcomeVoucherUsed,
        "telegramChatId" to telegramChatId,
        "telegramUsername" to telegramUsername,
        "telegramConnectedAt" to telegramConnectedAt,
        "telegramConnectionCode" to telegramConnectionCode,
        "telegramConnectionExpiresAt" to telegramConnectionExpiresAt,
        "telegramNotificationEvents" to telegramNotificationEvents
    )

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>?): AppUser {
            if (map == null) return AppUser(uid = uid)
            return AppUser(
                uid = uid,
                email = map["email"] as? String ?: "",
                role = Role.fromString(map["role"] as? String),
                premiumExpiryMillis = (map["premiumExpiryMillis"] as? Number)?.toLong(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                referralCode = map["referralCode"] as? String ?: "",
                referredByUid = map["referredByUid"] as? String,
                referralRewardGranted = map["referralRewardGranted"] as? Boolean ?: false,
                referralSuccessfulCount = (map["referralSuccessfulCount"] as? Number)?.toInt() ?: 0,
                referralRewardDaysEarned = (map["referralRewardDaysEarned"] as? Number)?.toInt() ?: 0,
                welcomeVoucherCode = map["welcomeVoucherCode"] as? String ?: "",
                welcomeVoucherPercent = (map["welcomeVoucherPercent"] as? Number)?.toInt() ?: 0,
                welcomeVoucherUsed = map["welcomeVoucherUsed"] as? Boolean ?: false,
                telegramChatId = map["telegramChatId"] as? String,
                telegramUsername = map["telegramUsername"] as? String,
                telegramConnectedAt = (map["telegramConnectedAt"] as? Number)?.toLong(),
                telegramConnectionCode = map["telegramConnectionCode"] as? String ?: "",
                telegramConnectionExpiresAt = (map["telegramConnectionExpiresAt"] as? Number)?.toLong(),
                telegramNotificationEvents = (map["telegramNotificationEvents"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }
    }
}
