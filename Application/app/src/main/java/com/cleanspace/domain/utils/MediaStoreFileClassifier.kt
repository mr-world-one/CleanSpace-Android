package com.cleanspace.domain.utils

import com.cleanspace.data.models.FileType

/**
 * Classifier for MediaStore entries, based on RELATIVE_PATH + name + size.
 * No java.io.File usage (because entries come from ContentProvider).
 */
object MediaStoreFileClassifier {

    fun classify(
        volumeRelativePath: String,
        displayName: String,
        sizeBytes: Long,
        largeThreshold: Long,
        mediaFamily: MediaFamily,
    ): FileType {
        if (sizeBytes > largeThreshold) return FileType.LARGE

        val rel = volumeRelativePath.lowercase()
        val name = displayName.lowercase()

        // High-signal buckets
        if (rel.contains("download")) return FileType.DOWNLOADS
        if (rel.contains("pictures/screenshots") || rel.contains("screenshots")) return FileType.SCREENSHOTS
        if (rel.contains("android/media")) {
            // Rough heuristics for messengers
            if (rel.contains("whatsapp") || rel.contains("telegram") || rel.contains("viber") || rel.contains("messenger") || rel.contains("signal")) {
                return FileType.MESSENGERS
            }
        }

        // Temporary-ish by extension
        val ext = name.substringAfterLast('.', "")
        if (ext in setOf("tmp", "temp", "log")) return FileType.TEMPORARY
        if (ext == "cache") return FileType.CACHE

        // Fallback by media family
        return when (mediaFamily) {
            MediaFamily.IMAGE -> FileType.MEDIA_IMAGES
            MediaFamily.VIDEO -> FileType.MEDIA_VIDEO
            MediaFamily.AUDIO -> FileType.MEDIA_AUDIO
        }
    }

    enum class MediaFamily { IMAGE, VIDEO, AUDIO }
}

