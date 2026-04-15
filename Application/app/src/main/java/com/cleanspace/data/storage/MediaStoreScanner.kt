package com.cleanspace.data.storage

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.cleanspace.data.models.FileItem
import com.cleanspace.data.models.FileSource
import com.cleanspace.data.models.FileType
import com.cleanspace.domain.utils.MediaStoreFileClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Scans MediaStore for images/video/audio.
     * Returns FileItem with uri + source=MEDIA_STORE.
     */
    fun scanAll(largeThreshold: Long, maxItems: Int = 5000): List<FileItem> {
        val items = ArrayList<FileItem>(1024)
        scanImages(largeThreshold, items, maxItems)
        scanVideo(largeThreshold, items, maxItems)
        scanAudio(largeThreshold, items, maxItems)
        return items
    }

    private fun scanImages(largeThreshold: Long, out: MutableList<FileItem>, maxItems: Int) {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )

        context.contentResolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
            ?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val relCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)

                while (c.moveToNext() && out.size < maxItems) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "(unknown)"
                    val size = c.getLong(sizeCol).coerceAtLeast(0L)
                    val dateSec = c.getLong(dateCol)
                    val rel = c.getString(relCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val type = MediaStoreFileClassifier.classify(
                        volumeRelativePath = rel,
                        displayName = name,
                        sizeBytes = size,
                        largeThreshold = largeThreshold,
                        mediaFamily = MediaStoreFileClassifier.MediaFamily.IMAGE,
                    )

                    out.add(
                        FileItem(
                            path = "MS:${contentUri}",
                            uri = contentUri.toString(),
                            source = FileSource.MEDIA_STORE,
                            name = name,
                            sizeBytes = size,
                            type = type,
                            lastModified = dateSec * 1000L,
                            isDeletable = type != FileType.SYSTEM,
                            parentDirectory = rel,
                        )
                    )
                }
            }
    }

    private fun scanVideo(largeThreshold: Long, out: MutableList<FileItem>, maxItems: Int) {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )

        context.contentResolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
            ?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val relCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)

                while (c.moveToNext() && out.size < maxItems) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "(unknown)"
                    val size = c.getLong(sizeCol).coerceAtLeast(0L)
                    val dateSec = c.getLong(dateCol)
                    val rel = c.getString(relCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val type = MediaStoreFileClassifier.classify(
                        volumeRelativePath = rel,
                        displayName = name,
                        sizeBytes = size,
                        largeThreshold = largeThreshold,
                        mediaFamily = MediaStoreFileClassifier.MediaFamily.VIDEO,
                    )

                    out.add(
                        FileItem(
                            path = "MS:${contentUri}",
                            uri = contentUri.toString(),
                            source = FileSource.MEDIA_STORE,
                            name = name,
                            sizeBytes = size,
                            type = type,
                            lastModified = dateSec * 1000L,
                            isDeletable = type != FileType.SYSTEM,
                            parentDirectory = rel,
                        )
                    )
                }
            }
    }

    private fun scanAudio(largeThreshold: Long, out: MutableList<FileItem>, maxItems: Int) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )

        context.contentResolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
            ?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val relCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)

                while (c.moveToNext() && out.size < maxItems) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "(unknown)"
                    val size = c.getLong(sizeCol).coerceAtLeast(0L)
                    val dateSec = c.getLong(dateCol)
                    val rel = c.getString(relCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val type = MediaStoreFileClassifier.classify(
                        volumeRelativePath = rel,
                        displayName = name,
                        sizeBytes = size,
                        largeThreshold = largeThreshold,
                        mediaFamily = MediaStoreFileClassifier.MediaFamily.AUDIO,
                    )

                    out.add(
                        FileItem(
                            path = "MS:${contentUri}",
                            uri = contentUri.toString(),
                            source = FileSource.MEDIA_STORE,
                            name = name,
                            sizeBytes = size,
                            type = type,
                            lastModified = dateSec * 1000L,
                            isDeletable = type != FileType.SYSTEM,
                            parentDirectory = rel,
                        )
                    )
                }
            }
    }
}
