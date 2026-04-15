package com.cleanspace.data.repository

import com.cleanspace.data.models.FileItem
import com.cleanspace.data.models.ScanProgress
import com.cleanspace.data.models.ScanResult

interface ScanRepository {
    suspend fun scanFiles(
        onProgress: suspend (ScanProgress) -> Unit,
    ): ScanResult

    suspend fun deleteFiles(
        files: List<FileItem>,
        onProgress: suspend (Int) -> Unit,
    ): Result<Long>

    fun cancelScan()
    fun isScanning(): Boolean
}

