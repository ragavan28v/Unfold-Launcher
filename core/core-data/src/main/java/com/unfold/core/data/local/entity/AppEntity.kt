package com.unfold.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.unfold.core.domain.model.AppInfo

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val appId: String,
    val packageName: String,
    val activityName: String,
    val userSerial: Long,
    val label: String,
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val customLabel: String? = null,
    val folderId: String? = null,
    val gridPosition: Int? = null,
    val category: String? = null,
    val installTimestamp: Long,
    val lastUsedTimestamp: Long = 0L,
    val launchCount: Long = 0L
) {
    fun toDomain(): AppInfo {
        return AppInfo(
            appId = appId,
            packageName = packageName,
            activityName = activityName,
            userSerial = userSerial,
            label = label,
            isHidden = isHidden,
            isLocked = isLocked,
            customLabel = customLabel,
            folderId = folderId,
            gridPosition = gridPosition,
            category = category,
            installTimestamp = installTimestamp,
            lastUsedTimestamp = lastUsedTimestamp,
            launchCount = launchCount
        )
    }

    companion object {
        fun fromDomain(app: AppInfo): AppEntity {
            return AppEntity(
                appId = app.appId,
                packageName = app.packageName,
                activityName = app.activityName,
                userSerial = app.userSerial,
                label = app.label,
                isHidden = app.isHidden,
                isLocked = app.isLocked,
                customLabel = app.customLabel,
                folderId = app.folderId,
                gridPosition = app.gridPosition,
                category = app.category,
                installTimestamp = app.installTimestamp,
                lastUsedTimestamp = app.lastUsedTimestamp,
                launchCount = app.launchCount
            )
        }
    }
}

