package com.cleanspace.presentation.clean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanspace.data.models.FileItem
import com.cleanspace.data.models.FileType
import com.cleanspace.data.repository.DeleteRequestException
import com.cleanspace.data.repository.StatsRepository
import com.cleanspace.domain.usecases.DeleteFilesUseCase
import com.cleanspace.domain.usecases.FindDuplicatesUseCase
import com.cleanspace.presentation.scan.ScanResultHolder
import com.cleanspace.utils.AppLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CleanViewModel @Inject constructor(
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val statsRepository: StatsRepository,
    private val findDuplicatesUseCase: FindDuplicatesUseCase,
) : ViewModel() {

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _selectedSize = MutableStateFlow(0L)
    val selectedSize: StateFlow<Long> = _selectedSize.asStateFlow()

    private val _selectedCount = MutableStateFlow(0)
    val selectedCount: StateFlow<Int> = _selectedCount.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _deleteProgress = MutableStateFlow(0)
    val deleteProgress: StateFlow<Int> = _deleteProgress.asStateFlow()

    private val eventsChannel = Channel<DeletePermissionEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    enum class Mode { ALL, DUPLICATES }

    private val _mode = MutableStateFlow(Mode.ALL)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _listItems = MutableStateFlow<List<CleanListItem>>(emptyList())
    val listItems: StateFlow<List<CleanListItem>> = _listItems.asStateFlow()

    private var duplicateGroups: List<List<FileItem>> = emptyList()

    private val _duplicatesProgress = MutableStateFlow<DuplicatesProgress?>(null)
    val duplicatesProgress: StateFlow<DuplicatesProgress?> = _duplicatesProgress.asStateFlow()

    private val _isFindingDuplicates = MutableStateFlow(false)
    val isFindingDuplicates: StateFlow<Boolean> = _isFindingDuplicates.asStateFlow()

    private var pendingDeleteIds: Set<String>? = null

    fun setMode(mode: Mode) {
        // Duplicates are computed on-demand; if user opens duplicates screen before running the search,
        // keep showing the normal list instead of an empty screen.
        if (mode == Mode.DUPLICATES && duplicateGroups.isEmpty()) {
            _mode.value = Mode.ALL
        } else {
            _mode.value = mode
        }
        rebuildListItems()
        updateSelectionSummary()
    }

    fun loadFromLastScan(filterType: FileType? = null, filterTypes: Set<FileType>? = null) {
        val scan = ScanResultHolder.lastScan
        val list = scan?.items.orEmpty().let { items ->
            when {
                filterTypes != null -> items.filter { filterTypes.contains(it.type) }
                filterType != null -> items.filter { it.type == filterType }
                else -> items
            }
        }
        // Duplicates are computed on-demand; we only pick up groups if they were already calculated.
        duplicateGroups = (scan?.duplicatesGroups ?: emptyList())
        _files.value = list

        // If UI requested duplicates mode but we don't have groups yet, stay in ALL and let the user press the button.
        if (_mode.value == Mode.DUPLICATES && duplicateGroups.isEmpty()) {
            _mode.value = Mode.ALL
        }

        rebuildListItems()
        updateSelectionSummary()
    }

    fun startFindDuplicates() {
        if (_isFindingDuplicates.value) return

        viewModelScope.launch {
            _isFindingDuplicates.value = true
            _duplicatesProgress.value = DuplicatesProgress(
                percent = 0,
                stage = "Підготовка",
                processedGroups = 0,
                totalGroups = 0,
            )

            try {
                val all = _files.value

                val candidates = withContext(Dispatchers.Default) {
                    all.groupBy { it.name.lowercase() to it.sizeBytes }
                        .values
                        .filter { it.size >= 2 && ((it.firstOrNull()?.sizeBytes ?: 0L) > 0L) }
                }

                _duplicatesProgress.value = _duplicatesProgress.value?.copy(
                    stage = "Пошук кандидатів",
                    processedGroups = 0,
                    totalGroups = candidates.size,
                    percent = 5,
                )

                _duplicatesProgress.value = _duplicatesProgress.value?.copy(stage = "Перевірка та хешування", percent = 15)
                val groups = findDuplicatesUseCase(all) { processed, total ->
                    val safeTotal = total.coerceAtLeast(1)
                    val percent = 15 + ((processed * 80) / safeTotal) // 15..95
                    _duplicatesProgress.value = DuplicatesProgress(
                        percent = percent.coerceIn(15, 95),
                        stage = "Перевірка та хешування",
                        processedGroups = processed,
                        totalGroups = total,
                    )
                }

                duplicateGroups = groups
                ScanResultHolder.lastScan = ScanResultHolder.lastScan?.copy(duplicatesGroups = groups)

                _duplicatesProgress.value = DuplicatesProgress(
                    percent = 100,
                    stage = "Готово",
                    processedGroups = candidates.size,
                    totalGroups = candidates.size,
                )

                _mode.value = Mode.DUPLICATES
                rebuildListItems()
                updateSelectionSummary()
            } catch (_: Exception) {
                _duplicatesProgress.value = null
            } finally {
                _isFindingDuplicates.value = false
            }
        }
    }

    fun selectDuplicatesKeepOne(groupId: String? = null) {
        val groups = if (groupId == null) duplicateGroups else {
            duplicateGroups.filter { buildGroupId(it) == groupId }
        }

        val idsToSelect = groups.flatMap { g ->
            g.sortedBy { it.lastModified }.drop(1).map { it.id }
        }.toSet()

        _files.update { list ->
            list.map { f ->
                if (idsToSelect.contains(f.id)) f.copy(isSelected = true) else f
            }
        }
        rebuildListItems()
        updateSelectionSummary()
    }

    private fun rebuildListItems() {
        val currentFiles = _files.value
        _listItems.value = when (_mode.value) {
            Mode.ALL -> currentFiles.map { CleanListItem.FileRow(groupId = null, isOriginal = false, file = it) }
            Mode.DUPLICATES -> buildDuplicateListItems(currentFiles)
        }
    }

    private fun buildDuplicateListItems(allFiles: List<FileItem>): List<CleanListItem> {
        if (duplicateGroups.isEmpty()) return emptyList()

        val byId = allFiles.associateBy { it.id }
        val out = mutableListOf<CleanListItem>()

        for (group in duplicateGroups) {
            val resolved = group.mapNotNull { byId[it.id] }
            if (resolved.size < 2) continue

            val groupSorted = resolved.sortedBy { it.lastModified }
            val originalId = groupSorted.first().id

            val groupId = buildGroupId(groupSorted)
            val title = groupSorted.first().name
            val totalSize = groupSorted.sumOf { it.sizeBytes }

            out.add(
                CleanListItem.DuplicateGroupHeader(
                    groupId = groupId,
                    title = title,
                    count = groupSorted.size,
                    totalSizeBytes = totalSize,
                )
            )
            groupSorted.forEach { f ->
                out.add(CleanListItem.FileRow(groupId = groupId, isOriginal = (f.id == originalId), file = f))
            }
        }

        return out
    }

    private fun buildGroupId(group: List<FileItem>): String {
        val name = group.firstOrNull()?.name ?: ""
        val size = group.firstOrNull()?.sizeBytes ?: 0L
        return "$name|$size"
    }

    fun toggleFileSelection(fileId: String) {
        _files.update { list ->
            list.map { f ->
                if (f.id == fileId) f.copy(isSelected = !f.isSelected) else f
            }
        }
        rebuildListItems()
        updateSelectionSummary()
    }

    fun selectAll() {
        _files.update { it.map { f -> f.copy(isSelected = true) } }
        rebuildListItems()
        updateSelectionSummary()
    }

    fun deselectAll() {
        _files.update { it.map { f -> f.copy(isSelected = false) } }
        rebuildListItems()
        updateSelectionSummary()
    }

    suspend fun deleteSelectedFiles(): Result<Long> {
        val selected = _files.value.filter { it.isSelected }
        if (selected.isEmpty()) return Result.success(0L)

        AppLog.d("UI deleteSelectedFiles count=${'$'}{selected.size} sources=${'$'}{selected.groupBy { it.source }.mapValues { it.value.size }}")

        _isDeleting.value = true
        _deleteProgress.value = 0
        pendingDeleteIds = selected.map { it.id }.toSet()

        val result: Result<Long> = try {
            deleteFilesUseCase(selected) { p ->
                _deleteProgress.value = p
            }
        } catch (e: DeleteRequestException) {
            AppLog.d("Delete requires system confirmation")
            _isDeleting.value = false
            _deleteProgress.value = 0
            return Result.failure(e)
        } catch (t: Throwable) {
            AppLog.e("deleteSelectedFiles failed", t)
            _isDeleting.value = false
            _deleteProgress.value = 0
            return Result.failure(t)
        }

        result.onSuccess { freedBytes ->
            if (freedBytes <= 0L) {
                _isDeleting.value = false
                _deleteProgress.value = 0
                return result
            }

            val deletedIds = selected.map { it.id }.toSet()
            applyDeletionToState(
                deletedIds = deletedIds,
                freedBytes = freedBytes,
                deletedItems = selected,
            )
        }

        _isDeleting.value = false
        _deleteProgress.value = 0
        return result
    }

    /**
     * Android 11+ createDeleteRequest can perform deletion itself after user confirms.
     * In that case retry may return 0 rows, so we finalize using pending selection.
     */
    suspend fun finalizePendingDeletionAfterSystemConfirmation(): Result<Long> {
        val pending = pendingDeleteIds ?: return Result.success(0L)
        val pendingItems = _files.value.filter { pending.contains(it.id) }
        if (pendingItems.isEmpty()) {
            pendingDeleteIds = null
            return Result.success(0L)
        }

        val freedBytes = pendingItems.sumOf { it.sizeBytes }
        applyDeletionToState(
            deletedIds = pending,
            freedBytes = freedBytes,
            deletedItems = pendingItems,
        )
        return Result.success(freedBytes)
    }

    private fun applyDeletionToState(
        deletedIds: Set<String>,
        freedBytes: Long,
        deletedItems: List<FileItem>,
    ) {
        pendingDeleteIds = null

        val categories = deletedItems.map { it.type.name }.distinct()
        viewModelScope.launch {
            statsRepository.addCleaningRecord(
                freedBytes = freedBytes,
                filesCount = deletedItems.size,
                categories = categories,
            )
        }

        _files.update { list -> list.filterNot { deletedIds.contains(it.id) } }

        ScanResultHolder.lastScan = ScanResultHolder.lastScan?.let { prev ->
            val newItems = prev.items.filterNot { deletedIds.contains(it.id) }
            val newCats = newItems.groupBy { it.type }.mapValues { (_, v) -> v.sumOf { it.sizeBytes } }
            val newCounts = newItems.groupBy { it.type }.mapValues { (_, v) -> v.size }
            prev.copy(
                items = newItems,
                totalSize = newItems.sumOf { it.sizeBytes },
                categoriesSummary = newCats,
                filesCountByType = newCounts,
                duplicatesGroups = prev.duplicatesGroups
                    .map { g -> g.filterNot { deletedIds.contains(it.id) } }
                    .filter { it.size >= 2 },
            )
        }

        rebuildListItems()
        updateSelectionSummary()
        _isDeleting.value = false
        _deleteProgress.value = 0
    }

    private fun updateSelectionSummary() {
        val selected = _files.value.filter { it.isSelected }
        _selectedCount.value = selected.size
        _selectedSize.value = selected.sumOf { it.sizeBytes }
    }
}
