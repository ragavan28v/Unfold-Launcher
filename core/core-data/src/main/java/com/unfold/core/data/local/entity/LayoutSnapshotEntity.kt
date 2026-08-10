package com.unfold.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layout_snapshots")
data class LayoutSnapshotEntity(
    @PrimaryKey val id: String,
    val jsonPayload: String,
    val createdTimestamp: Long,
    val label: String
)

