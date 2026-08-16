package com.unfold.core.domain.model

enum class TimelineItemType {
    EVENT,
    REMINDER,
    NOTE
}

data class TimelineItem(
    val id: String,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val type: TimelineItemType,
    val isAllDay: Boolean = false,
    val location: String? = null
)
