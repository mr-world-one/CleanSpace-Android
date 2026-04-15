package com.cleanspace.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cleanspace.data.local.database.dao.CleaningRecordDao
import com.cleanspace.data.local.database.dao.WhitelistDao
import com.cleanspace.data.local.database.entities.CleaningRecordEntity
import com.cleanspace.data.local.database.entities.WhitelistEntity

@Database(
    entities = [CleaningRecordEntity::class, WhitelistEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class CleanSpaceDatabase : RoomDatabase() {

    abstract fun cleaningRecordDao(): CleaningRecordDao
    abstract fun whitelistDao(): WhitelistDao

    companion object {
        @Volatile
        private var INSTANCE: CleanSpaceDatabase? = null

        fun getInstance(context: Context): CleanSpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CleanSpaceDatabase::class.java,
                    "cleanspace.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
