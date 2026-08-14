package com.unfold.core.data.repositoryimpl

import com.unfold.core.data.local.dao.AppDao
import com.unfold.core.data.local.dao.FolderDao
import com.unfold.core.data.local.entity.FolderEntity
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.model.FolderInfo
import com.unfold.core.domain.repository.FolderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val appDao: AppDao
) : FolderRepository {

    override fun observeFolders(): Flow<List<FolderInfo>> {
        return combine(
            folderDao.observeAllFolders(),
            appDao.observeApps(includeHidden = false).map { apps ->
                apps.map { it.toDomain() }
            }
        ) { folders, apps ->
            val appsByFolder = apps
                .filter { !it.folderId.isNullOrBlank() }
                .groupBy { it.folderId!! }

            folders
                .sortedBy { it.gridPosition }
                .map { folder ->
                    folder.toDomain(
                        apps = appsByFolder[folder.id]
                            .orEmpty()
                            .sortedBy { it.label.lowercase() }
                    )
                }
        }
    }

    override suspend fun createFolder(name: String, appIds: List<String>): String = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Folder name cannot be blank" }

        val nextPosition = (folderDao.getAllFolders().maxOfOrNull { it.gridPosition } ?: -1) + 1
        val folderId = "folder_${System.currentTimeMillis()}"

        folderDao.insertFolder(
            FolderEntity(
                id = folderId,
                name = trimmedName,
                gridPosition = nextPosition
            )
        )

        if (appIds.isNotEmpty()) {
            appDao.setFolderForApps(folderId, appIds.distinct())
        }

        folderId
    }

    override suspend fun renameFolder(folderId: String, name: String) = withContext(Dispatchers.IO) {
        val folder = folderDao.getFolderById(folderId) ?: return@withContext
        val trimmedName = name.trim()
        if (trimmedName.isNotBlank() && trimmedName != folder.name) {
            folderDao.updateFolder(folder.copy(name = trimmedName))
        }
    }

    override suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        val folder = folderDao.getFolderById(folderId) ?: return@withContext
        appDao.clearFolder(folderId)
        folderDao.deleteFolder(folder)
    }

    override suspend fun updateFolderApps(folderId: String, appIds: List<String>) = withContext(Dispatchers.IO) {
        folderDao.getFolderById(folderId) ?: return@withContext
        appDao.clearFolder(folderId)
        val distinctAppIds = appIds.distinct()
        if (distinctAppIds.isNotEmpty()) {
            appDao.setFolderForApps(folderId, distinctAppIds)
        }
    }

    override suspend fun reorderFolders(folderIdsInOrder: List<String>) = withContext(Dispatchers.IO) {
        folderIdsInOrder.distinct().forEachIndexed { index, folderId ->
            folderDao.updateFolderPosition(folderId, index)
        }
    }
}
