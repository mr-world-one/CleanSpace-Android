package com.cleanspace.di

import android.content.Context
import com.cleanspace.data.local.database.CleanSpaceDatabase
import com.cleanspace.data.local.database.dao.CleaningRecordDao
import com.cleanspace.data.local.database.dao.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CleanSpaceDatabase =
        CleanSpaceDatabase.getInstance(context)

    @Provides
    fun provideCleaningRecordDao(db: CleanSpaceDatabase): CleaningRecordDao = db.cleaningRecordDao()

    @Provides
    fun provideWhitelistDao(db: CleanSpaceDatabase): WhitelistDao = db.whitelistDao()
}

