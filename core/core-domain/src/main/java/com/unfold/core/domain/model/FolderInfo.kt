package com.unfold.core.domain.model

data class FolderInfo(
    val id: String,
    val name: String,
    val gridPosition: Int,
    val accentColorOverride: String? = null,
    val apps: List<AppInfo> = emptyList()
)

