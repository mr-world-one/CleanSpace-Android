package com.cleanspace.data.local.database

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cleanspace.data.local.database.entities.CleaningRecordEntity
import com.cleanspace.data.local.database.entities.WhitelistEntity
import org.json.JSONArray
import kotlin.random.Random

class SeedDatabaseWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = CleanSpaceDatabase.getInstance(applicationContext)
        val cleaningRecordDao = db.cleaningRecordDao()
        val whitelistDao = db.whitelistDao()

        seedWhitelist(whitelistDao)
        seedCleaningRecords(cleaningRecordDao)
        return Result.success()
    }

    private suspend fun seedWhitelist(whitelistDao: com.cleanspace.data.local.database.dao.WhitelistDao) {
        // 10 directories
        val base = listOf(
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Documents",
            "/storage/emulated/0/DCIM",
            "/storage/emulated/0/Pictures",
            "/storage/emulated/0/Movies",
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Telegram",
            "/storage/emulated/0/WhatsApp",
            "/storage/emulated/0/Android/media",
            "/storage/emulated/0/Android/data",
        )

        base.forEachIndexed { index, path ->
            val enabled = index % 4 != 0
            whitelistDao.insert(WhitelistEntity(path = path, isEnabled = enabled))
        }
    }

    private suspend fun seedCleaningRecords(cleaningRecordDao: com.cleanspace.data.local.database.dao.CleaningRecordDao) {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L

        val possibleCategories = listOf(
            "CACHE",
            "DUPLICATE",
            "TEMPORARY",
            "LARGE",
        )

        // 15 records over last 30 days
        repeat(15) { i ->
            val daysAgo = Random.nextInt(0, 30)
            val date = now - daysAgo * dayMs - i * 60_000L

            val categoriesCount = Random.nextInt(1, 4)
            val categories = possibleCategories.shuffled().take(categoriesCount)
            val categoriesJson = JSONArray(categories).toString()

            val freedBytes = Random.nextLong(5L * 1024 * 1024, 450L * 1024 * 1024) // 5MB..450MB
            val filesCount = Random.nextInt(5, 250)
            val wasSuccessful = Random.nextInt(0, 10) != 0

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
    }
}
