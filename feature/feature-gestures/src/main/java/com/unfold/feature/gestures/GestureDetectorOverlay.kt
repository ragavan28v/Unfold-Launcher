package com.unfold.feature.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.unfold.core.domain.model.GestureType
import kotlin.math.abs

@Composable
fun GestureDetectorOverlay(
    modifier: Modifier = Modifier,
    onGestureDetected: (GestureType) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var fingerCount = 1
                val startPosition = down.position
                var totalDrag = Offset.Zero
                var gestureRecognized = false
                var childConsumed = false
                
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    
                    if (change.isConsumed || childConsumed) {
                        childConsumed = true
                        continue
                    }
                    
                    fingerCount = maxOf(fingerCount, event.changes.size)
                    totalDrag = change.position - startPosition
                    
                    if (totalDrag.getDistance() > SWIPE_THRESHOLD_PX &&
                        abs(totalDrag.x) > abs(totalDrag.y) * DIRECTION_RATIO
                    ) {
                        gestureRecognized = true
                    }
                    
                    if (gestureRecognized) {
                        change.consume()
                    }
                }

                if (gestureRecognized) {
                    val gesture = classifyGesture(totalDrag, fingerCount)
                    gesture?.let(onGestureDetected)
                }
            }
        }
    ) { content() }
}

private const val SWIPE_THRESHOLD_PX = 160f
private const val DIRECTION_RATIO = 1.15f

private fun classifyGesture(drag: Offset, fingerCount: Int): GestureType? {
    if (drag.getDistance() < SWIPE_THRESHOLD_PX) return null
    val isHorizontal = abs(drag.x) > abs(drag.y) * DIRECTION_RATIO
    return when {
        isHorizontal && drag.x < 0 && fingerCount == 1 -> GestureType.SWIPE_LEFT_1F
        isHorizontal && drag.x > 0 && fingerCount == 1 -> GestureType.SWIPE_RIGHT_1F
        isHorizontal && drag.x < 0 && fingerCount == 2 -> GestureType.SWIPE_LEFT_2F
        isHorizontal && drag.x > 0 && fingerCount == 2 -> GestureType.SWIPE_RIGHT_2F
        !isHorizontal && drag.y < 0 && fingerCount == 1 -> GestureType.SWIPE_UP_1F
        !isHorizontal && drag.y < 0 && fingerCount == 2 -> GestureType.SWIPE_UP_2F
        !isHorizontal && drag.y > 0 && fingerCount == 1 -> GestureType.SWIPE_DOWN_1F
        else -> null
    }
}

