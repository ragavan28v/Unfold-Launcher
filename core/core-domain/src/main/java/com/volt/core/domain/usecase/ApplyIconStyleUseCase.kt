package com.volt.core.domain.usecase

import com.volt.core.domain.model.ThemeConfig
import com.volt.core.domain.repository.ThemeRepository
import javax.inject.Inject

class ApplyIconStyleUseCase @Inject constructor(
    private val themeRepo: ThemeRepository
) {
    suspend operator fun invoke(config: ThemeConfig) {
        themeRepo.updateTheme(config)
    }
}
