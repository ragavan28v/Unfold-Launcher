package com.unfold.core.data.repositoryimpl

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserManager
import com.unfold.core.data.local.AppDatabaseSeedData
import com.unfold.core.data.local.dao.AppDao
import com.unfold.core.data.local.dao.FolderDao
import com.unfold.core.data.local.entity.AppEntity
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import com.unfold.core.data.local.entity.GestureEntity
import com.unfold.core.domain.navigation.UnfoldRoute

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val folderDao: FolderDao,
    private val gestureDao: com.unfold.core.data.local.dao.GestureDao
) : AppRepository {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        launcherApps?.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) {
                repositoryScope.launch {
                    refreshFromPackageManager()
                }
            }
            override fun onPackageAdded(packageName: String, user: android.os.UserHandle) {
                repositoryScope.launch {
                    refreshFromPackageManager()
                }
            }
            override fun onPackageChanged(packageName: String, user: android.os.UserHandle) {
                repositoryScope.launch {
                    refreshFromPackageManager()
                }
            }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {
                repositoryScope.launch {
                    refreshFromPackageManager()
                }
            }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {
                repositoryScope.launch {
                    refreshFromPackageManager()
                }
            }
        })
    }

    override fun observeApps(includeHidden: Boolean): Flow<List<AppInfo>> {
        return appDao.observeApps(includeHidden).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshFromPackageManager() = withContext(Dispatchers.IO) {
        if (folderDao.getAllFolders().isEmpty()) {
            folderDao.insertFolders(AppDatabaseSeedData.defaultFolders())
        }

        val defaultGestures = AppDatabaseSeedData.defaultGestures()
        defaultGestures.forEach { defaultGesture ->
            if (gestureDao.getBinding(defaultGesture.gestureType) == null) {
                gestureDao.insertBinding(defaultGesture)
            }
        }

        val pm = context.packageManager
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val userManager = context.getSystemService(UserManager::class.java)
        val currentEntities = appDao.getAllApps().associateBy { it.appId }
        val discoveredApps = linkedMapOf<String, DiscoveredApp>()

        fun registerResolveInfo(
            info: android.content.pm.ResolveInfo,
            userSerial: Long
        ) {
            val activityInfo = info.activityInfo ?: return
            val packageName = activityInfo.packageName
            val activityName = activityInfo.name
            val label = info.loadLabel(pm).toString()
            val appId = buildAppId(packageName, activityName, userSerial)
            discoveredApps[appId] = DiscoveredApp(
                appId = appId,
                packageName = packageName,
                activityName = activityName,
                userSerial = userSerial,
                label = label
            )
        }

        fun registerActivityInfo(
            packageName: String,
            activityName: String,
            label: String,
            userSerial: Long
        ) {
            val appId = buildAppId(packageName, activityName, userSerial)
            discoveredApps[appId] = DiscoveredApp(
                appId = appId,
                packageName = packageName,
                activityName = activityName,
                userSerial = userSerial,
                label = label
            )
        }

        val profiles = launcherApps?.profiles?.ifEmpty { listOf(Process.myUserHandle()) }
            ?: listOf(Process.myUserHandle())

        if (launcherApps != null) {
            profiles.forEach { userHandle ->
                val userSerial = runCatching {
                    userManager?.getSerialNumberForUser(userHandle) ?: 0L
                }.getOrDefault(0L)

                    launcherApps.getActivityList(null, userHandle).forEach { activityInfo ->
                        val componentName = activityInfo.componentName
                        registerActivityInfo(
                            packageName = componentName.packageName,
                            activityName = componentName.className,
                            label = activityInfo.label?.toString().orEmpty(),
                            userSerial = userSerial
                        )
                    }
                }
            }

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL).forEach { info ->
            registerResolveInfo(info, 0L)
        }

        val contactsIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_APP_CONTACTS)
        }
        pm.queryIntentActivities(contactsIntent, PackageManager.MATCH_ALL).forEach { info ->
            registerResolveInfo(info, 0L)
        }

        val sortedApps = discoveredApps.values.sortedBy { it.label.lowercase() }
        val isFirstRun = currentEntities.isEmpty()
        var nextGridPosIndex = 0

        val newEntities = sortedApps.map { app ->
            val existing = currentEntities[app.appId]
            val assignedPosition = existing?.gridPosition ?: run {
                if (isFirstRun) {
                    val pos = if (nextGridPosIndex < 6) {
                        100 + nextGridPosIndex
                    } else if (nextGridPosIndex < 12) {
                        nextGridPosIndex - 6
                    } else {
                        null
                    }
                    nextGridPosIndex++
                    pos
                } else {
                    null
                }
            }

            val category = existing?.category ?: AppDatabaseSeedData.resolveCategory(app.packageName, app.label)
            val folderId = existing?.folderId ?: AppDatabaseSeedData.folderIdForCategory(category)

            AppEntity(
                appId = app.appId,
                packageName = app.packageName,
                activityName = app.activityName,
                userSerial = app.userSerial,
                label = app.label,
                isHidden = existing?.isHidden ?: false,
                isLocked = existing?.isLocked ?: false,
                customLabel = existing?.customLabel,
                folderId = if (isFirstRun) folderId else existing?.folderId,
                gridPosition = assignedPosition,
                category = if (isFirstRun) category else existing?.category,
                installTimestamp = existing?.installTimestamp ?: System.currentTimeMillis(),
                lastUsedTimestamp = existing?.lastUsedTimestamp ?: 0L,
                launchCount = existing?.launchCount ?: 0L
            )
        }

        val hasDockApps = newEntities.any { it.gridPosition != null && it.gridPosition >= 100 }
        val allPositionedApps = newEntities.filter { it.gridPosition != null }
        val finalEntitiesWithPositions = if (!hasDockApps && allPositionedApps.isNotEmpty()) {
            newEntities.mapIndexed { index, entity ->
                if (entity.gridPosition != null) {
                    val newPos = if (index < 6) 100 + index else index - 6
                    entity.copy(gridPosition = if (newPos < 100 && newPos >= 18) null else newPos)
                } else {
                    entity
                }
            }
        } else {
            newEntities
        }

        // Deduplicate positions: guarantee no two apps share the same non-null gridPosition
        val occupiedPositions = mutableSetOf<Int>()
        val finalEntities = finalEntitiesWithPositions.map { entity ->
            val pos = entity.gridPosition
            if (pos != null) {
                if (occupiedPositions.contains(pos)) {
                    entity.copy(gridPosition = null)
                } else {
                    occupiedPositions.add(pos)
                    entity
                }
            } else {
                entity
            }
        }

        appDao.insertApps(finalEntities)
        appDao.deleteAppsNotInList(finalEntities.map { it.appId })
    }

    override suspend fun setHidden(appId: String, hidden: Boolean) {
        appDao.setHidden(appId, hidden)
    }

    override suspend fun setGridPosition(appId: String, position: Int?) {
        appDao.setGridPosition(appId, position)
    }

    override suspend fun recordLaunch(appId: String) {
        appDao.recordLaunch(appId, System.currentTimeMillis())
    }

    private fun buildAppId(packageName: String, activityName: String, userSerial: Long): String {
        return "$packageName/$activityName@$userSerial"
    }

    private data class DiscoveredApp(
        val appId: String,
        val packageName: String,
        val activityName: String,
        val userSerial: Long,
        val label: String
    )
}


