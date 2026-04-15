package com.cleanspace.domain.usecases

import com.cleanspace.data.models.ScanProgress
import com.cleanspace.data.models.ScanResult
import com.cleanspace.data.repository.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanFilesUseCase @Inject constructor(
    private val repository: ScanRepository,
) {
    suspend operator fun invoke(
        onProgress: suspend (ScanProgress) -> Unit,
    ): ScanResult = withContext(Dispatchers.IO) {
        repository.scanFiles(onProgress)
    }

    fun cancel() = repository.cancelScan()
}

