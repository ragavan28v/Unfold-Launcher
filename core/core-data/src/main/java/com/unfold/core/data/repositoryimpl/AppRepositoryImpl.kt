package com.unfold.core.data.repositoryimpl

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserManager
import com.unfold.core.data.local.dao.AppDao
import com.unfold.core.data.local.entity.AppEntity
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val gestureDao: com.unfold.core.data.local.dao.GestureDao
) : AppRepository {

    override fun observeApps(includeHidden: Boolean): Flow<List<AppInfo>> {
        return appDao.observeApps(includeHidden).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshFromPackageManager() = withContext(Dispatchers.IO) {
        // Defensively seed default gestures on startup/sync to support pre-existing databases
        val defaultGestures = listOf(
            GestureEntity("SWIPE_LEFT_1F", "OPEN_INTENT", targetIntentUri = "tel:"),
            GestureEntity("SWIPE_RIGHT_1F", "LAUNCH_APP", targetPackage = "com.whatsapp"),
            GestureEntity("SWIPE_LEFT_2F", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.HiddenSpace.route),
            GestureEntity("SWIPE_RIGHT_2F", "OPEN_INTENT", targetIntentUri = "market://details?id="),
            GestureEntity("SWIPE_DOWN_1F", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.UniversalSearch.route),
            GestureEntity("SWIPE_UP_1F", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.AppDrawer.route)
        )
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
        var nextGridPosIndex = 0

        val newEntities = sortedApps.map { app ->
            val existing = currentEntities[app.appId]
            val assignedPosition = existing?.gridPosition ?: run {
                val pos = if (nextGridPosIndex < 6) {
                    100 + nextGridPosIndex
                } else if (nextGridPosIndex < 12) {
                    nextGridPosIndex - 6
                } else {
                    null
                }
                nextGridPosIndex++
                pos
            }

            AppEntity(
                appId = app.appId,
                packageName = app.packageName,
                activityName = app.activityName,
                userSerial = app.userSerial,
                label = app.label,
                isHidden = existing?.isHidden ?: false,
                isLocked = existing?.isLocked ?: false,
                customLabel = existing?.customLabel,
                folderId = existing?.folderId,
                gridPosition = assignedPosition,
                category = existing?.category,
                installTimestamp = existing?.installTimestamp ?: System.currentTimeMillis(),
                lastUsedTimestamp = existing?.lastUsedTimestamp ?: 0L,
                launchCount = existing?.launchCount ?: 0L
            )
        }

        val hasDockApps = newEntities.any { it.gridPosition != null && it.gridPosition >= 100 }
        val allPositionedApps = newEntities.filter { it.gridPosition != null }
        val finalEntities = if (!hasDockApps && allPositionedApps.isNotEmpty()) {
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

        appDao.insertApps(finalEntities)
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


