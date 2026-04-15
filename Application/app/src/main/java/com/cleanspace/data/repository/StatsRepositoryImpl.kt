package com.cleanspace.data.repository

import com.cleanspace.data.local.database.dao.CleaningRecordDao
import com.cleanspace.data.local.database.dao.CategoryStat
import com.cleanspace.data.local.database.entities.CleaningRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val cleaningRecordDao: CleaningRecordDao,
) : StatsRepository {

    override fun getAllCleaningRecords(): Flow<List<CleaningRecordEntity>> =
        cleaningRecordDao.getAllRecords()

    override fun getTotalFreedBytes(): Flow<Long> =
        cleaningRecordDao.getTotalFreedBytes()

    override fun getCleaningCount(): Flow<Int> =
        cleaningRecordDao.getAllRecords().map { it.size }

    override suspend fun addCleaningRecord(
        freedBytes: Long,
        filesCount: Int,
        categories: List<String>,
        wasSuccessful: Boolean,
        date: Long,
    ) {
        val categoriesJson = JSONArray(categories).toString()
        cleaningRecordDao.insert(
            CleaningRecordEntity(
                date = date,
                freedBytes = freedBytes,
                filesCount = filesCount,
                categories = categoriesJson,
                wasSuccessful = wasSuccessful,
            )
        )
    }

    override fun getLastCleaningDate(): Flow<Long?> =
        cleaningRecordDao.getLastCleaningDate()

    override fun getStatisticsByCategory(): Flow<List<CategoryStat>> =
        cleaningRecordDao.getStatisticsByCategory()
}

