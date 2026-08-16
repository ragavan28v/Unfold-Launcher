package com.unfold.core.domain.repository

import com.unfold.core.domain.model.TimelineItem
import kotlinx.coroutines.flow.Flow

interface TimelineRepository {
    fun getTodayTimeline(): Flow<List<TimelineItem>>
    fun getTimeline(startTimeMillis: Long, endTimeMillis: Long): Flow<List<TimelineItem>>
}
