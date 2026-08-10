package com.unfold.core.domain.repository

import com.unfold.core.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun observeApps(includeHidden: Boolean): Flow<List<AppInfo>>
    suspend fun refreshFromPackageManager()
    suspend fun setHidden(packageName: String, hidden: Boolean)
    suspend fun setGridPosition(packageName: String, position: Int?)
    suspend fun recordLaunch(packageName: String)
}

