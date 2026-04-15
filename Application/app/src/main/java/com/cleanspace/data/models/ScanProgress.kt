package com.cleanspace.data.models

data class ScanProgress(
    val percent: Int,
    val currentDirectory: String,
    val filesFound: Int,
    val totalSizeFound: Long,
    val isCancelled: Boolean,
    val currentDepth: Int,
)

