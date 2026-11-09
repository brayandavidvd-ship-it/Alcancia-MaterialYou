package com.example.ui.components

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatAmount(amount: Double, symbol: String = "$"): String {
        val formatter = NumberFormat.getNumberInstance(Locale("es", "MX")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formattedNumber = formatter.format(amount)
        return "$symbol$formattedNumber"
    }

    fun formatCompact(amount: Double, symbol: String = "$"): String {
        val formatter = NumberFormat.getNumberInstance(Locale("es", "MX")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "$symbol${formatter.format(amount)}"
    }
}
