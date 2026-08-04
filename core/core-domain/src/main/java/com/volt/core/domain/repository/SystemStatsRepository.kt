package com.volt.core.domain.repository

import com.volt.core.domain.model.SystemStats
import kotlinx.coroutines.flow.Flow

interface SystemStatsRepository {
    fun observeStats(pollIntervalMs: Long): Flow<SystemStats>
}
