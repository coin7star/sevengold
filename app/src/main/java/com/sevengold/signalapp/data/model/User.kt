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
    val createdAt: Long = System.currentTimeMillis()
) {
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
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>?): AppUser {
            if (map == null) return AppUser(uid = uid)
            return AppUser(
                uid = uid,
                email = map["email"] as? String ?: "",
                role = Role.fromString(map["role"] as? String),
                premiumExpiryMillis = (map["premiumExpiryMillis"] as? Number)?.toLong(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
