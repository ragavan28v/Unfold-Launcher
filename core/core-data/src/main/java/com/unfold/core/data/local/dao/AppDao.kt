package com.unfold.core.data.local.dao

import androidx.room.*
import com.unfold.core.data.local.entity.AppEntity
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

    @Query("UPDATE apps SET isHidden = :hidden WHERE appId = :appId")
    suspend fun setHidden(appId: String, hidden: Boolean)

    @Query("UPDATE apps SET gridPosition = :position WHERE appId = :appId")
    suspend fun setGridPosition(appId: String, position: Int?)

    @Query("UPDATE apps SET folderId = :folderId WHERE appId = :appId")
    suspend fun setFolderId(appId: String, folderId: String?)

    @Query("UPDATE apps SET folderId = :folderId WHERE appId IN (:appIds)")
    suspend fun setFolderForApps(folderId: String?, appIds: List<String>)

    @Query("UPDATE apps SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: String)

    @Query("UPDATE apps SET launchCount = launchCount + 1, lastUsedTimestamp = :timestamp WHERE appId = :appId")
    suspend fun recordLaunch(appId: String, timestamp: Long)

    @Query("SELECT * FROM apps WHERE appId = :appId")
    suspend fun getApp(appId: String): AppEntity?
}

