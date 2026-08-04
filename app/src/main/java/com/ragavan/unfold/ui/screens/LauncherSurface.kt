package com.ragavan.unfold.ui.screens

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.ragavan.unfold.engine.gesture.GestureState

@Composable
fun LauncherSurface(

    gestureState: GestureState,

    content: @Composable BoxScope.() -> Unit

) {

    Box(

        modifier = Modifier

            .fillMaxSize()

            .graphicsLayer {

                translationY =
                    gestureState.progress * -220f

            }

            .pointerInput(Unit) {

                detectDragGestures(

                    onDrag = { change, drag ->

                        change.consume()

                        if (

                            kotlin.math.abs(drag.y) >
                            kotlin.math.abs(drag.x)

                        ) {

                            gestureState.update(
                                drag.y
                            )

                        }

                    },

                    onDragEnd = {

                        gestureState.reset()

                    }

                )

            }

    ) {

        content()

    }

}