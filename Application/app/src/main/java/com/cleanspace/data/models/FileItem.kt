package com.cleanspace.data.models

import java.util.UUID

enum class FileSource {
    FILE_PATH,
    SAF_TREE,
    MEDIA_STORE,
}

/**
 * Представляє один файл у системі.
 * Не містить Android-залежностей.
 * Всі поля immutable (val) крім [isSelected].
 */
data class FileItem(
    val id: String = UUID.randomUUID().toString(),
    val path: String,
    val uri: String? = null,
    val source: FileSource = FileSource.FILE_PATH,
    val name: String,
    val sizeBytes: Long,
    val type: FileType,
    var isSelected: Boolean = false,
    val lastModified: Long,
    val isDeletable: Boolean,
    val parentDirectory: String,
    val hash: String? = null,
)

