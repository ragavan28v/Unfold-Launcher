package com.unfold.core.domain.repository

import com.unfold.core.domain.model.SystemStats
import kotlinx.coroutines.flow.Flow

interface SystemStatsRepository {
    fun observeStats(pollIntervalMs: Long): Flow<SystemStats>
}

