package com.unfold.core.domain.usecase

import com.unfold.core.domain.model.TimelineItem
import com.unfold.core.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTimelineUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository
) {
    operator fun invoke(startTimeMillis: Long? = null, endTimeMillis: Long? = null): Flow<List<TimelineItem>> {
        return if (startTimeMillis != null && endTimeMillis != null) {
            timelineRepository.getTimeline(startTimeMillis, endTimeMillis)
        } else {
            timelineRepository.getTodayTimeline()
        }
    }
}
