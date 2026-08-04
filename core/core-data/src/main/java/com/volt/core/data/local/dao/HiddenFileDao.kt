package com.volt.core.data.local.dao

import androidx.room.*
import com.volt.core.data.local.entity.HiddenFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenFileDao {
    @Query("SELECT * FROM hidden_files ORDER BY addedTimestamp DESC")
    fun observeAllHiddenFiles(): Flow<List<HiddenFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenFile(file: HiddenFileEntity)

    @Delete
    suspend fun deleteHiddenFile(file: HiddenFileEntity)
}
