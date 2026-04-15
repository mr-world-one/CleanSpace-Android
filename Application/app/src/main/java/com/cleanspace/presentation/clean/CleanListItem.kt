package com.cleanspace.presentation.clean

import com.cleanspace.data.models.FileItem

sealed interface CleanListItem {
    data class DuplicateGroupHeader(
        val groupId: String,
        val title: String,
        val count: Int,
        val totalSizeBytes: Long,
    ) : CleanListItem

    data class FileRow(
        val groupId: String?,
        val isOriginal: Boolean,
        val file: FileItem,
    ) : CleanListItem
}
