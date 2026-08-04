package com.volt.core.domain.usecase

import com.volt.core.domain.model.GestureBinding
import com.volt.core.domain.model.GestureType
import com.volt.core.domain.repository.GestureRepository
import javax.inject.Inject

class ResolveGestureActionUseCase @Inject constructor(
    private val gestureRepo: GestureRepository
) {
    suspend operator fun invoke(gestureType: GestureType): GestureBinding? {
        return gestureRepo.getBinding(gestureType)
    }
}
