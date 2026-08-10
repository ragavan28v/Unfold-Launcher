package com.unfold.core.data.repositoryimpl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        
        val currentEntities = appDao.getAllApps().associateBy { it.packageName }
        var nextGridPosIndex = 0

        val newEntities = resolveInfos.map { info ->
            val packageName = info.activityInfo.packageName
            val activityName = info.activityInfo.name
            val label = info.loadLabel(pm).toString()
            val existing = currentEntities[packageName]

            val assignedPosition = if (existing != null) {
                existing.gridPosition
            } else {
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
                packageName = packageName,
                activityName = activityName,
                label = label,
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

        // Defensive check: if database has old configuration without dock apps, force re-assign
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

    override suspend fun setHidden(packageName: String, hidden: Boolean) {
        appDao.setHidden(packageName, hidden)
    }

    override suspend fun setGridPosition(packageName: String, position: Int?) {
        appDao.setGridPosition(packageName, position)
    }

    override suspend fun recordLaunch(packageName: String) {
        appDao.recordLaunch(packageName, System.currentTimeMillis())
    }
}


