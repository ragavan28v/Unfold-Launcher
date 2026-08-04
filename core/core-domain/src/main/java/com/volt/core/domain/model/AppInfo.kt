package com.volt.core.domain.model

data class AppInfo(
    val packageName: String,
    val activityName: String,
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
)
