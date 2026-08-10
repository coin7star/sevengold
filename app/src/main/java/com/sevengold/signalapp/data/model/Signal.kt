package com.sevengold.signalapp.data.model

enum class SignalType { BUY, SELL }

/**
 * TP_HIT & SL_HIT menggantikan status lama "CLOSED", supaya kita bisa bedain
 * sinyal yang closed karena menang (TP_HIT) vs kalah (SL_HIT) untuk hitung winrate.
 * ACTIVE & CANCELLED tetap sama; BE (breakeven) & CANCELLED TIDAK dihitung ke winrate.
 */
enum class SignalStatus { ACTIVE, BE, CANCELLED, TP_HIT, SL_HIT }

data class Signal(
    val id: String = "",
    val pair: String = "XAUUSD",
    val type: SignalType = SignalType.BUY,
    val entry: Double = 0.0,
    val tp: Double = 0.0,
    val sl: Double = 0.0,
    val status: SignalStatus = SignalStatus.ACTIVE,
    val note: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "pair" to pair,
        "type" to type.name,
        "entry" to entry,
        "tp" to tp,
        "sl" to sl,
        "status" to status.name,
        "note" to note,
        "createdBy" to createdBy,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): Signal {
            if (map == null) return Signal(id = id)
            return Signal(
                id = id,
                pair = map["pair"] as? String ?: "XAUUSD",
                type = runCatching { SignalType.valueOf(map["type"] as? String ?: "BUY") }.getOrDefault(SignalType.BUY),
                entry = (map["entry"] as? Number)?.toDouble() ?: 0.0,
                tp = (map["tp"] as? Number)?.toDouble() ?: 0.0,
                sl = (map["sl"] as? Number)?.toDouble() ?: 0.0,
                status = runCatching { SignalStatus.valueOf(map["status"] as? String ?: "ACTIVE") }.getOrDefault(SignalStatus.ACTIVE),
                note = map["note"] as? String ?: "",
                createdBy = map["createdBy"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
