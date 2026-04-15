package com.cleanspace.data.models

import com.cleanspace.domain.utils.FileSizeFormatter

data class ScanResult(
    val items: List<FileItem>,
    val totalSize: Long,
    val scanDuration: Long,
    val categoriesSummary: Map<FileType, Long>,
    val duplicatesGroups: List<List<FileItem>>,
    val filesCountByType: Map<FileType, Int>,
) {
    fun getFormattedTotalSize(): String = FileSizeFormatter.formatBytes(totalSize)

    fun getFormattedDuration(): String {
        val ms = scanDuration.coerceAtLeast(0)
        val seconds = ms / 1000
        val minutes = seconds / 60
        val remSeconds = seconds % 60
        return if (minutes > 0) "${minutes}х ${remSeconds}с" else "${remSeconds}с"
    }

    fun getPercentageForType(type: FileType): Int {
        if (totalSize <= 0) return 0
        val size = categoriesSummary[type] ?: 0L
        return ((size.toDouble() / totalSize.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    }
}

