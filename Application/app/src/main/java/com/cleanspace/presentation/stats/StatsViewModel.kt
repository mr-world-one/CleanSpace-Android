package com.cleanspace.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanspace.data.local.database.dao.CategoryStat
import com.cleanspace.data.local.database.entities.CleaningRecordEntity
import com.cleanspace.data.repository.StatsRepository
import com.cleanspace.presentation.scan.ScanResultHolder
import com.github.mikephil.charting.data.PieEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepository: StatsRepository,
) : ViewModel() {

    val records: StateFlow<List<CleaningRecordEntity>> =
        statsRepository.getAllCleaningRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalFreedBytes: StateFlow<Long> =
        statsRepository.getTotalFreedBytes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val categoryStats: StateFlow<List<CategoryStat>> =
        statsRepository.getStatisticsByCategory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pieEntries: StateFlow<List<PieEntry>> =
        combine(records, ScanResultHolder.lastScanFlow) { recs, scan ->
            val fromScan = StatsChartHelper.buildCategoryPieFromScan(scan)
            if (fromScan.isNotEmpty()) fromScan else StatsChartHelper.buildCategoryPie(recs)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
