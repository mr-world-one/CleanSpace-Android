package com.cleanspace.domain.utils

import kotlin.math.log10
import kotlin.math.pow

object FileSizeFormatter {
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        val unit = units[digitGroups - 1]
        return String.format("%.1f %s", value, unit)
    }
}

