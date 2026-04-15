package com.cleanspace.domain.utils

import com.cleanspace.data.models.FileType

object SafFileClassifier {

    fun classify(
        documentName: String,
        parentDirectory: String,
        sizeBytes: Long,
        largeThreshold: Long,
    ): FileType {
        val nameLower = documentName.lowercase()
        val parentLower = parentDirectory.lowercase()
        val ext = nameLower.substringAfterLast('.', missingDelimiterValue = "")

        // Cache-like locations/files first for new "Кеш телефону" card.
        if (parentLower.contains("cache") || ext == "cache") return FileType.CACHE

        // Media categories for Dashboard filters.
        if (ext in setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif")) {
            return if (parentLower.contains("screenshot")) FileType.SCREENSHOTS else FileType.MEDIA_IMAGES
        }
        if (ext in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp")) return FileType.MEDIA_VIDEO
        if (ext in setOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus")) return FileType.MEDIA_AUDIO

        if (parentLower.contains("download") || parentLower.contains("завантаж")) return FileType.DOWNLOADS

        if (parentLower.contains("telegram") || parentLower.contains("whatsapp") || parentLower.contains("viber")) {
            return FileType.MESSENGERS
        }

        if (ext in setOf("tmp", "temp", "log")) return FileType.TEMPORARY

        if (sizeBytes > largeThreshold) return FileType.LARGE

        return FileType.UNKNOWN
    }
}
