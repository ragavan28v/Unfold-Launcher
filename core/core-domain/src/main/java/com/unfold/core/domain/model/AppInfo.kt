package com.unfold.core.domain.model

data class AppInfo(
    val appId: String,
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
)

