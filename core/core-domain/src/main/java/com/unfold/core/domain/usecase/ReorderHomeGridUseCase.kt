package com.unfold.core.domain.usecase

import com.unfold.core.domain.repository.AppRepository
import javax.inject.Inject

class ReorderHomeGridUseCase @Inject constructor(
    private val appRepo: AppRepository
) {
    suspend operator fun invoke(packageName: String, newPosition: Int) {
        appRepo.setGridPosition(packageName, newPosition)
    }
}

