package com.unfold.core.domain.repository

import com.unfold.core.domain.model.ThemeConfig
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun observeTheme(): Flow<ThemeConfig>
    suspend fun updateTheme(config: ThemeConfig)
    suspend fun resetTheme()
}

