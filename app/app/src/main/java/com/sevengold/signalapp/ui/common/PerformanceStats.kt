package com.sevengold.signalapp.ui.common

import com.sevengold.signalapp.data.model.Signal
import com.sevengold.signalapp.data.model.SignalStatus
import com.sevengold.signalapp.data.model.SignalType
import java.util.Calendar

/**
 * XAUUSD: pergerakan harga $1 setara 10 pip. Dipakai buat nerjemahin hasil sinyal
 * (yang disimpan dalam satuan harga/$) ke satuan pip yang lebih familiar buat trader.
 */
private const val XAUUSD_PIPS_PER_DOLLAR = 10.0

/**
 * Hasil realisasi 1 sinyal dalam pip.
 * Cuma bermakna untuk status TP_HIT (positif = profit) & SL_HIT (negatif = rugi).
 * ACTIVE / BE / CANCELLED selalu 0.0 dan diabaikan total di statistik performa.
 */
fun Signal.realizedPips(): Double {
    val priceDiff = when (type) {
        SignalType.BUY -> when (status) {
            SignalStatus.TP_HIT -> tp - entry
            SignalStatus.SL_HIT -> sl - entry
            else -> 0.0
        }
        SignalType.SELL -> when (status) {
            SignalStatus.TP_HIT -> entry - tp
            SignalStatus.SL_HIT -> entry - sl
            else -> 0.0
        }
    }
    return priceDiff * XAUUSD_PIPS_PER_DOLLAR
}

enum class StatsPeriod(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan")
}

data class SignalStats(
    val totalSignals: Int,
    val wins: Int,
    val losses: Int,
    val winRatePercent: Double,
    val totalPips: Double
)

private fun Calendar.atStartOfDay(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun periodStartMillis(period: StatsPeriod, now: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    return when (period) {
        StatsPeriod.DAILY -> cal.atStartOfDay().timeInMillis

        StatsPeriod.WEEKLY -> cal.apply {
            firstDayOfWeek = Calendar.MONDAY
            // mundurin ke hari Senin minggu ini
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                add(Calendar.DAY_OF_MONTH, -1)
            }
        }.atStartOfDay().timeInMillis

        StatsPeriod.MONTHLY -> cal.apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.atStartOfDay().timeInMillis
    }
}

/**
 * Statistik performa (winrate + total pip) untuk 1 periode (harian/mingguan/bulanan).
 * CANCELLED dan BE TIDAK dihitung sama sekali — baik ke jumlah sinyal maupun winrate.
 * Cuma TP_HIT (menang) dan SL_HIT (kalah) yang masuk hitungan.
 */
fun List<Signal>.toPerformanceStats(period: StatsPeriod, now: Long = System.currentTimeMillis()): SignalStats {
    val start = periodStartMillis(period, now)
    val relevant = filter {
        it.createdAt >= start && (it.status == SignalStatus.TP_HIT || it.status == SignalStatus.SL_HIT)
    }
    val wins = relevant.count { it.status == SignalStatus.TP_HIT }
    val losses = relevant.count { it.status == SignalStatus.SL_HIT }
    val total = wins + losses
    val winRate = if (total > 0) (wins.toDouble() / total.toDouble()) * 100.0 else 0.0
    val totalPips = relevant.sumOf { it.realizedPips() }
    return SignalStats(
        totalSignals = total,
        wins = wins,
        losses = losses,
        winRatePercent = winRate,
        totalPips = totalPips
    )
}
