package com.unfold.core.domain.usecase

import com.unfold.core.domain.repository.AppRepository
import javax.inject.Inject

class ToggleHiddenAppUseCase @Inject constructor(
    private val appRepo: AppRepository
) {
    suspend operator fun invoke(packageName: String, hidden: Boolean) {
        appRepo.setHidden(packageName, hidden)
    }
}

