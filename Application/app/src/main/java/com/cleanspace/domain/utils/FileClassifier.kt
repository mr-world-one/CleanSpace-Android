package com.cleanspace.domain.utils

import com.cleanspace.data.models.FileType
import java.io.File

object FileClassifier {

    fun classify(file: File, largeThreshold: Long): FileType {
        val path = file.absolutePath
        return when {
            isSystemFile(path) -> FileType.SYSTEM
            isCacheFile(path) -> FileType.CACHE
            isTemporaryFile(file.name) -> FileType.TEMPORARY
            isLargeFile(file.length(), largeThreshold) -> FileType.LARGE
            else -> FileType.UNKNOWN
        }
    }

    private fun isCacheFile(path: String): Boolean {
        val p = path.lowercase()
        return p.contains("/cache/") || p.endsWith(".cache")
    }

    private fun isTemporaryFile(name: String): Boolean {
        val ext = getFileExtension(name)
        return ext in setOf("tmp", "temp", "log")
    }

    private fun isSystemFile(path: String): Boolean {
        val p = path.lowercase()
        val systemPaths = listOf("/system", "/proc", "/dev", "/sys", "/data/data", "/data/app")
        return systemPaths.any { p.startsWith(it) }
    }

    private fun isLargeFile(size: Long, threshold: Long): Boolean = size > threshold

    private fun getFileExtension(name: String): String {
        val idx = name.lastIndexOf('.')
        return if (idx == -1 || idx == name.length - 1) "" else name.substring(idx + 1).lowercase()
    }
}

