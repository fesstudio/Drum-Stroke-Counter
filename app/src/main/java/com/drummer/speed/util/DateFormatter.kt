package com.drummer.speed.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    fun formatDate(timestamp: Long, locale: Locale): String {
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", locale)
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long, locale: Locale): String {
        val sdf = SimpleDateFormat("HH:mm", locale)
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long, locale: Locale): String {
        val sdf = SimpleDateFormat("dd/MM", locale)
        return sdf.format(Date(timestamp))
    }
}
