package com.unfold.core.domain.repository

import com.unfold.core.domain.model.GestureBinding
import com.unfold.core.domain.model.GestureType
import kotlinx.coroutines.flow.Flow

interface GestureRepository {
    fun observeBindings(): Flow<List<GestureBinding>>
    suspend fun getBinding(gestureType: GestureType): GestureBinding?
    suspend fun setBinding(binding: GestureBinding)
    suspend fun resetToDefaults()
}

