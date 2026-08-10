package com.unfold.core.domain.usecase

import com.unfold.core.domain.model.GestureBinding
import com.unfold.core.domain.model.GestureType
import com.unfold.core.domain.repository.GestureRepository
import javax.inject.Inject

class ResolveGestureActionUseCase @Inject constructor(
    private val gestureRepo: GestureRepository
) {
    suspend operator fun invoke(gestureType: GestureType): GestureBinding? {
        return gestureRepo.getBinding(gestureType)
    }
}

