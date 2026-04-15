package com.cleanspace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelist",
    indices = [
        Index(value = ["isEnabled"], name = "idx_enabled"),
    ],
)
data class WhitelistEntity(
    @PrimaryKey
    val path: String,
    val isEnabled: Boolean = true,
)
