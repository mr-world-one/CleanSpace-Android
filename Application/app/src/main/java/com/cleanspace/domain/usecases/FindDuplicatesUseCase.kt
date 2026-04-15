package com.cleanspace.domain.usecases

import android.content.ContentResolver
import android.net.Uri
import com.cleanspace.data.models.FileItem
import com.cleanspace.data.models.FileSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject

class FindDuplicatesUseCase @Inject constructor(
    private val contentResolver: ContentResolver,
) {

    /**
     * Finds duplicates.
     * 1) group by (name + size)
     * 2) hash only a bounded subset of local FILE_PATH items up to [maxHashBytes]
     */
    suspend operator fun invoke(
        items: List<FileItem>,
        maxHashBytes: Long = 5L * 1024L * 1024L,
        maxCandidateGroups: Int = 1500,
        maxFilesToHash: Int = 2500,
        onProgress: (suspend (processed: Int, total: Int) -> Unit)? = null,
    ): List<List<FileItem>> = withContext(Dispatchers.IO) {
        val candidates = items
            .groupBy { it.name.lowercase() to it.sizeBytes }
            .values
            .asSequence()
            .filter { it.size >= 2 }
            .take(maxCandidateGroups)
            .toList()

        val result = mutableListOf<List<FileItem>>()
        var processed = 0
        var hashedCount = 0

        for (group in candidates) {
            processed++
            onProgress?.invoke(processed, candidates.size)

            val size = group.first().sizeBytes
            if (size <= 0L) continue

            // For large files keep probable duplicates by name+size.
            if (size > maxHashBytes) {
                result.add(group)
                continue
            }

            // Hashing content URIs can be very slow on some devices; use name+size grouping for them.
            val hasUriSources = group.any { it.source != FileSource.FILE_PATH }
            if (hasUriSources || hashedCount >= maxFilesToHash) {
                result.add(group)
                continue
            }

            val hashed = group.map { item ->
                val hash = if (hashedCount < maxFilesToHash) {
                    hashedCount++
                    computeMd5(item)
                } else {
                    null
                }
                item.copy(hash = hash)
            }

            val byHash = hashed.groupBy { it.hash }
                .filterKeys { !it.isNullOrBlank() }
                .values
                .filter { it.size >= 2 }

            if (byHash.isNotEmpty()) result.addAll(byHash) else result.add(group)
        }

        result
    }

    private fun computeMd5(item: FileItem): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            openStream(item)?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    md.update(buffer, 0, read)
                }
            } ?: return null
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun openStream(item: FileItem): InputStream? {
        return when (item.source) {
            FileSource.MEDIA_STORE, FileSource.SAF_TREE -> {
                val uriStr = item.uri ?: return null
                val u = Uri.parse(uriStr)
                contentResolver.openInputStream(u)
            }

            FileSource.FILE_PATH -> {
                runCatching { FileInputStream(File(item.path)) }.getOrNull()
            }
        }
    }
}
