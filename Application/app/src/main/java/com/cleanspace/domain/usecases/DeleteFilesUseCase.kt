package com.cleanspace.domain.usecases

import com.cleanspace.data.models.FileItem
import com.cleanspace.data.repository.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteFilesUseCase @Inject constructor(
    private val repository: ScanRepository,
) {
    suspend operator fun invoke(
        files: List<FileItem>,
        onProgress: suspend (Int) -> Unit,
    ): Result<Long> = withContext(Dispatchers.IO) {
        repository.deleteFiles(files, onProgress)
    }
}

