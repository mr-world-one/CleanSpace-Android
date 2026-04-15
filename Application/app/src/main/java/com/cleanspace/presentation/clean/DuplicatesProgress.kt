package com.cleanspace.presentation.clean

/** UI progress for duplicate search. */
data class DuplicatesProgress(
    val percent: Int,
    val stage: String,
    val processedGroups: Int,
    val totalGroups: Int,
)

