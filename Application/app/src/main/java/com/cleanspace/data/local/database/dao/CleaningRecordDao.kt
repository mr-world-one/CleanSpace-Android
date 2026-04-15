package com.cleanspace.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cleanspace.data.local.database.entities.CleaningRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CleaningRecordDao {

    @Insert
    suspend fun insert(record: CleaningRecordEntity)

    @Query("SELECT * FROM cleaning_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<CleaningRecordEntity>>

    @Query("SELECT COALESCE(SUM(freedBytes), 0) FROM cleaning_records")
    fun getTotalFreedBytes(): Flow<Long>

    @Query("SELECT * FROM cleaning_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getRecordsBetween(startDate: Long, endDate: Long): List<CleaningRecordEntity>

    @Query("DELETE FROM cleaning_records WHERE date < :beforeDate")
    suspend fun deleteOldRecords(beforeDate: Long)

    @Query("SELECT date FROM cleaning_records ORDER BY date DESC LIMIT 1")
    fun getLastCleaningDate(): Flow<Long?>

    @Query(
        "SELECT categories AS category, COALESCE(SUM(freedBytes), 0) AS totalFreedBytes " +
            "FROM cleaning_records GROUP BY categories ORDER BY totalFreedBytes DESC"
    )
    fun getStatisticsByCategory(): Flow<List<CategoryStat>>
}

