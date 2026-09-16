package com.nakamoto.bitshock

import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BitshockFormatting {
    fun formatPrice(value: Double): String {
        return if (value >= 100_000) {
            "$${String.format(Locale.US, "%.1fk", value / 1000.0)}"
        } else {
            NumberFormat.getCurrencyInstance(Locale.US).apply {
                maximumFractionDigits = 0
            }.format(value)
        }
    }

    fun formatCount(n: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(n)

    fun formatVsize(vbytes: Long): String = when {
        vbytes >= 1_000_000 -> String.format(Locale.US, "%.2f MB", vbytes / 1_000_000.0)
        vbytes >= 1_000 -> String.format(Locale.US, "%.1f kB", vbytes / 1000.0)
        else -> "$vbytes vB"
    }

    fun formatBtc(sats: Long): String {
        val btc = sats / 100_000_000.0
        return when {
            btc >= 1 -> String.format(Locale.US, "%.2f BTC", btc)
            btc >= 0.001 -> String.format(Locale.US, "%.4f BTC", btc)
            else -> String.format(Locale.US, "%.6f BTC", btc)
        }
    }

    fun timeAgo(timestampSec: Long): String {
        val mins = TimeUnit.MILLISECONDS.toMinutes(Date().time - timestampSec * 1000)
        return when {
            mins < 1 -> "just now"
            mins < 60 -> "${mins}m ago"
            else -> "${mins / 60}h ago"
        }
    }

    fun loadingOrDash(state: WatchUiState): String =
        if (state.loading) "…" else "—"
}
