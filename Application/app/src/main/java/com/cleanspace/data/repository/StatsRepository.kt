package com.cleanspace.data.repository

import com.cleanspace.data.local.database.dao.CategoryStat
import com.cleanspace.data.local.database.entities.CleaningRecordEntity
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    fun getAllCleaningRecords(): Flow<List<CleaningRecordEntity>>
    fun getTotalFreedBytes(): Flow<Long>
    fun getCleaningCount(): Flow<Int>

    suspend fun addCleaningRecord(
        freedBytes: Long,
        filesCount: Int,
        categories: List<String>,
        wasSuccessful: Boolean = true,
        date: Long = System.currentTimeMillis(),
    )

    fun getLastCleaningDate(): Flow<Long?>

    /** Returns aggregated stats grouped by the raw `categories` JSON string (current DB design). */
    fun getStatisticsByCategory(): Flow<List<CategoryStat>>
}

