package com.cleanspace.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanspace.data.models.FileType
import com.cleanspace.data.repository.StatsRepository
import com.cleanspace.presentation.scan.ScanResultHolder
import com.cleanspace.utils.StorageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    statsRepository: StatsRepository,
) : ViewModel() {

    val lastCleanupDate: StateFlow<Long?> =
        statsRepository.getLastCleaningDate()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val totalFreedBytes: StateFlow<Long> =
        statsRepository.getTotalFreedBytes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val cleaningCount: StateFlow<Int> =
        statsRepository.getCleaningCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _storageInfo = MutableStateFlow(StorageHelper.getPrimaryStorageInfo())
    val storageInfo: StateFlow<StorageHelper.StorageInfo> = _storageInfo.asStateFlow()

    private val _categoryCards = MutableStateFlow<Map<FileType, DashboardCategorySummary>>(emptyMap())
    val categoryCards: StateFlow<Map<FileType, DashboardCategorySummary>> = _categoryCards.asStateFlow()

    init {
        // Initial fill (in case we already have a scan)
        rebuildCategoryCards()
        refresh()
    }

    fun refresh() {
        _storageInfo.value = StorageHelper.getPrimaryStorageInfo()
        rebuildCategoryCards()
    }

    private fun rebuildCategoryCards() {
        val scan = ScanResultHolder.lastScan

        val items = scan?.items.orEmpty()
        val counts = items.groupBy { it.type }.mapValues { (_, v) -> v.size }
        val sizes = items.groupBy { it.type }.mapValues { (_, v) -> v.sumOf { it.sizeBytes } }

        val dupGroups = scan?.duplicatesGroups.orEmpty()
        val duplicatesCalculated = dupGroups.isNotEmpty()
        val dupCount = if (!duplicatesCalculated) 0 else dupGroups.sumOf { g -> (g.size - 1).coerceAtLeast(0) }
        val dupBytes = if (!duplicatesCalculated) 0L else dupGroups.sumOf { g ->
            g.sortedBy { it.lastModified }.drop(1).sumOf { it.sizeBytes }
        }

        val photoCount = (counts[FileType.MEDIA_IMAGES] ?: 0) + (counts[FileType.SCREENSHOTS] ?: 0)
        val photoBytes = (sizes[FileType.MEDIA_IMAGES] ?: 0L) + (sizes[FileType.SCREENSHOTS] ?: 0L)

        val audioVideoCount = (counts[FileType.MEDIA_AUDIO] ?: 0) + (counts[FileType.MEDIA_VIDEO] ?: 0)
        val audioVideoBytes = (sizes[FileType.MEDIA_AUDIO] ?: 0L) + (sizes[FileType.MEDIA_VIDEO] ?: 0L)

        val cacheCount = counts[FileType.CACHE] ?: 0
        val cacheBytes = sizes[FileType.CACHE] ?: 0L

        val map = linkedMapOf<FileType, DashboardCategorySummary>()

        fun put(type: FileType, count: Int, bytes: Long) {
            map[type] = DashboardCategorySummary(type = type, count = count, totalBytes = bytes)
        }

        // Card #1: Photos
        put(FileType.MEDIA_IMAGES, photoCount, photoBytes)
        // Card #2: Duplicates
        put(FileType.DUPLICATE, dupCount, dupBytes)
        // Card #3: Audio/Video
        put(FileType.MEDIA_AUDIO, audioVideoCount, audioVideoBytes)
        // Card #4: Accessible phone cache
        put(FileType.CACHE, cacheCount, cacheBytes)

        _categoryCards.value = map
    }
}
