package com.example.clickerapp.util

object NumberFormatter {
    /**
     * Форматирует число в красивый формат: 1K, 1.5M, 2.3B и т.д.
     */
    fun format(number: Long): String {
        return when {
            number >= 1_000_000_000_000L -> {
                val trillions = number / 1_000_000_000_000.0
                String.format("%.1fT", trillions)
            }
            number >= 1_000_000_000L -> {
                val billions = number / 1_000_000_000.0
                String.format("%.1fB", billions)
            }
            number >= 1_000_000L -> {
                val millions = number / 1_000_000.0
                String.format("%.1fM", millions)
            }
            number >= 1_000L -> {
                val thousands = number / 1_000.0
                String.format("%.1fK", thousands)
            }
            else -> number.toString()
        }
    }

    /**
     * Форматирует число для отображения в UI (без десятичных для больших чисел)
     */
    fun formatCompact(number: Long): String {
        return when {
            number >= 1_000_000_000_000L -> {
                val trillions = number / 1_000_000_000_000L
                "${trillions}T"
            }
            number >= 1_000_000_000L -> {
                val billions = number / 1_000_000_000L
                "${billions}B"
            }
            number >= 1_000_000L -> {
                val millions = number / 1_000_000L
                "${millions}M"
            }
            number >= 1_000L -> {
                val thousands = number / 1_000L
                "${thousands}K"
            }
            else -> number.toString()
        }
    }
}
