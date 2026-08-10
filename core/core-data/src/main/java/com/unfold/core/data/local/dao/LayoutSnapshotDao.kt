package com.unfold.core.data.local.dao

import androidx.room.*
import com.unfold.core.data.local.entity.LayoutSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LayoutSnapshotDao {
    @Query("SELECT * FROM layout_snapshots ORDER BY createdTimestamp DESC")
    fun observeAllSnapshots(): Flow<List<LayoutSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: LayoutSnapshotEntity)

    @Delete
    suspend fun deleteSnapshot(snapshot: LayoutSnapshotEntity)
}

