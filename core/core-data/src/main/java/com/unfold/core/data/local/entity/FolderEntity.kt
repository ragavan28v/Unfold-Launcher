package com.unfold.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.model.FolderInfo

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gridPosition: Int,
    val accentColorOverride: String? = null
) {
    fun toDomain(apps: List<AppInfo> = emptyList()): FolderInfo {
        return FolderInfo(
            id = id,
            name = name,
            gridPosition = gridPosition,
            accentColorOverride = accentColorOverride,
            apps = apps
        )
    }

    companion object {
        fun fromDomain(folder: FolderInfo): FolderEntity {
            return FolderEntity(
                id = folder.id,
                name = folder.name,
                gridPosition = folder.gridPosition,
                accentColorOverride = folder.accentColorOverride
            )
        }
    }
}

