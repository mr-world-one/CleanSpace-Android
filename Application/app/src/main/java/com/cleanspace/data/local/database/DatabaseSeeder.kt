package com.cleanspace.data.local.database

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object DatabaseSeeder {
    private const val UNIQUE_WORK_NAME = "seed_cleanspace_db"

    fun enqueueIfNeeded(context: Context) {
        val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>().build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}

