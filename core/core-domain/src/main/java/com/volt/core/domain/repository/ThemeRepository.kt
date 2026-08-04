package com.volt.core.domain.repository

import com.volt.core.domain.model.ThemeConfig
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun observeTheme(): Flow<ThemeConfig>
    suspend fun updateTheme(config: ThemeConfig)
}
