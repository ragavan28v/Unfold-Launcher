package com.ragavan.unfold.engine.gesture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

class GestureState {

    companion object {
        const val MAX_DRAG = 1200f
    }

    var dragOffset by mutableFloatStateOf(0f)
        private set

    val progress: Float
        get() = (-dragOffset / MAX_DRAG).coerceIn(0f, 1f)

    fun update(delta: Float) {

        dragOffset =
            (dragOffset + delta)
                .coerceIn(-MAX_DRAG, 0f)

    }

    fun reset() {

        dragOffset = 0f

    }

}