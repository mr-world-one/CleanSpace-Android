package com.cleanspace.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cleanspace.data.local.database.entities.WhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dir: WhitelistEntity)

    @Delete
    suspend fun delete(dir: WhitelistEntity)

    @Query("SELECT * FROM whitelist WHERE isEnabled = 1")
    fun getEnabledDirectories(): Flow<List<WhitelistEntity>>

    @Query("SELECT * FROM whitelist")
    fun getAllDirectories(): Flow<List<WhitelistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE path = :path AND isEnabled = 1)")
    suspend fun isInWhitelist(path: String): Boolean

    @Query("DELETE FROM whitelist")
    suspend fun clearAll()
}

