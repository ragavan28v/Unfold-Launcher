package com.unfold.core.domain.repository

import com.unfold.core.domain.model.FolderInfo
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun observeFolders(): Flow<List<FolderInfo>>
    suspend fun createFolder(name: String, appIds: List<String>): String
    suspend fun renameFolder(folderId: String, name: String)
    suspend fun deleteFolder(folderId: String)
    suspend fun updateFolderApps(folderId: String, appIds: List<String>)
    suspend fun reorderFolders(folderIdsInOrder: List<String>)
}
