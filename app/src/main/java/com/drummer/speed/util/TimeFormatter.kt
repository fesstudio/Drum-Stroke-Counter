package com.drummer.speed.util

import android.content.Context
import com.drummer.speed.R
import java.util.Locale

object TimeFormatter {
    fun format(totalSeconds: Int, context: Context): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        } else {
            "$seconds${context.getString(R.string.sec)}"
        }
    }

    fun formatWithSec(totalSeconds: Int, context: Context): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) {
            String.format(Locale.getDefault(), "%d:%02d ${context.getString(R.string.sec)}", minutes, seconds)
        } else {
            "$seconds ${context.getString(R.string.sec)}"
        }
    }
}
