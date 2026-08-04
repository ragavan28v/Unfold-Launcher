package com.volt.core.data.local.dao

import androidx.room.*
import com.volt.core.data.local.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps")
    fun observeAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps")
    suspend fun getAllApps(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE isHidden = 0 OR isHidden = :includeHidden")
    fun observeApps(includeHidden: Boolean): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Query("UPDATE apps SET isHidden = :hidden WHERE packageName = :packageName")
    suspend fun setHidden(packageName: String, hidden: Boolean)

    @Query("UPDATE apps SET gridPosition = :position WHERE packageName = :packageName")
    suspend fun setGridPosition(packageName: String, position: Int?)

    @Query("UPDATE apps SET launchCount = launchCount + 1, lastUsedTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun recordLaunch(packageName: String, timestamp: Long)

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getApp(packageName: String): AppEntity?
}
