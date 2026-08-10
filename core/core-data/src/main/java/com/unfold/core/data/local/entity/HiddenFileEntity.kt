package com.unfold.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_files")
data class HiddenFileEntity(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val addedTimestamp: Long
)

