package com.example.currencydashboard.utils

import android.content.Context
import com.example.currencydashboard.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Format a timestamp in milliseconds to a readable time string (HH:mm:ss)
 */
fun formatTimestamp(timestamp: Long, context: Context): String {
    if (timestamp == 0L) return context.getString(R.string.never)
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Format a currency rate with appropriate precision based on its value
 */
fun formatRate(rate: Double, context: Context): String {
    return when {
        rate == 1.0 -> {
            "1.00"
        }
        rate > 1000 -> {
            String.format("%.2f", rate)
        }
        rate > 0 -> {
            String.format("%.4f", rate)
        }
        else -> {
            context.getString(R.string.no_value)
        }
    }
}

/**
 * Format a change percentage with a plus sign for positive values
 */
fun formatChange(change: Double, context: Context): String {
    return if (change > 0) {
        context.getString(R.string.positive_change_format, change)
    } else if (change < 0) {
        context.getString(R.string.negative_change_format, change)
    } else {
        context.getString(R.string.zero_change)
    }
}

/**
 * Get the currency symbol for a given currency code
 */
fun getCurrencySymbol(code: String): String {
    return when (code) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        "BTC" -> "₿"
        "ETH" -> "Ξ"
        else -> code.take(1)
    }
} 