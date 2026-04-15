package com.cleanspace.utils

import android.os.Environment
import android.os.StatFs

object StorageHelper {

    data class StorageInfo(
        val totalBytes: Long,
        val freeBytes: Long,
        val usedBytes: Long,
    )

    fun getPrimaryStorageInfo(): StorageInfo {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val total = stat.blockSizeLong * stat.blockCountLong
        val free = stat.blockSizeLong * stat.availableBlocksLong
        val used = (total - free).coerceAtLeast(0)
        return StorageInfo(totalBytes = total, freeBytes = free, usedBytes = used)
    }
}

