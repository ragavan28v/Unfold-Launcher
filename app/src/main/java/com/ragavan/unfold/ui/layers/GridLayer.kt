package com.ragavan.unfold.ui.layers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun GridLayer() {

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val spacing = 80f

        val gridColor =
            Color(0xFF6FD8FF).copy(alpha = 0.05f)

        var x = 0f

        while (x <= size.width) {

            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height)
            )

            x += spacing
        }

        var y = 0f

        while (y <= size.height) {

            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y)
            )

            y += spacing
        }

    }

}