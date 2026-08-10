package com.unfold.core.domain.usecase

import com.unfold.core.domain.model.ThemeConfig
import com.unfold.core.domain.repository.ThemeRepository
import javax.inject.Inject

class ApplyIconStyleUseCase @Inject constructor(
    private val themeRepo: ThemeRepository
) {
    suspend operator fun invoke(config: ThemeConfig) {
        themeRepo.updateTheme(config)
    }
}

