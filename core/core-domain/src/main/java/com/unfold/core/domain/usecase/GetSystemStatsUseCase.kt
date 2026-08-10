package com.unfold.core.domain.usecase

import com.unfold.core.domain.model.SystemStats
import com.unfold.core.domain.repository.SystemStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSystemStatsUseCase @Inject constructor(
    private val statsRepo: SystemStatsRepository
) {
    operator fun invoke(pollIntervalMs: Long = 3000L): Flow<SystemStats> {
        return statsRepo.observeStats(pollIntervalMs)
    }
}

