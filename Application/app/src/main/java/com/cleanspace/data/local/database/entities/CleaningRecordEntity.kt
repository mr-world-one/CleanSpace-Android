package com.cleanspace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(
    tableName = "cleaning_records",
    indices = [
        Index(value = ["date"], name = "idx_date"),
        Index(value = ["wasSuccessful"], name = "idx_successful"),
    ],
)
data class CleaningRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val freedBytes: Long,
    val filesCount: Int,
    val categories: String,
    val wasSuccessful: Boolean,
) {
    fun getCategoriesList(): List<String> = try {
        val arr = JSONArray(categories)
        buildList {
            for (i in 0 until arr.length()) add(arr.optString(i))
        }
    } catch (_: Exception) {
        emptyList()
    }
}
