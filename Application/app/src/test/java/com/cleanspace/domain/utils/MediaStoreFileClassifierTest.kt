package com.cleanspace.domain.utils

import com.cleanspace.data.models.FileType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreFileClassifierTest {

    @Test
    fun screenshots_are_classified() {
        val type = MediaStoreFileClassifier.classify(
            volumeRelativePath = "Pictures/Screenshots/",
            displayName = "Screenshot_2026.png",
            sizeBytes = 1000,
            largeThreshold = 10_000_000,
            mediaFamily = MediaStoreFileClassifier.MediaFamily.IMAGE,
        )
        assertEquals(FileType.SCREENSHOTS, type)
    }

    @Test
    fun downloads_are_classified() {
        val type = MediaStoreFileClassifier.classify(
            volumeRelativePath = "Download/",
            displayName = "file.mp4",
            sizeBytes = 1000,
            largeThreshold = 10_000_000,
            mediaFamily = MediaStoreFileClassifier.MediaFamily.VIDEO,
        )
        assertEquals(FileType.DOWNLOADS, type)
    }

    @Test
    fun large_wins_over_other_rules() {
        val type = MediaStoreFileClassifier.classify(
            volumeRelativePath = "Pictures/Screenshots/",
            displayName = "Screenshot_big.png",
            sizeBytes = 20_000_000,
            largeThreshold = 10_000_000,
            mediaFamily = MediaStoreFileClassifier.MediaFamily.IMAGE,
        )
        assertEquals(FileType.LARGE, type)
    }
}

