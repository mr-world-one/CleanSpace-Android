package com.cleanspace.presentation.dashboard

import com.cleanspace.data.models.FileType

data class DashboardCategorySummary(
    val type: FileType,
    val count: Int,
    val totalBytes: Long,
)

