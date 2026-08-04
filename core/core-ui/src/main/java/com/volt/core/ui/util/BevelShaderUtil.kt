package com.volt.core.ui.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

fun DrawScope.drawCarvedBevel(
    radius: Float,
    intensity: Float,
    isInverted: Boolean,       // true while pressed
    accentColor: Color
) {
    val darkOffset = if (isInverted) Offset(-3f, -4f) else Offset(3f, 4f)
    val lightOffset = if (isInverted) Offset(3f, 4f) else Offset(-2f, -2f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.55f * intensity), Color.Transparent),
            center = center + darkOffset
        ),
        radius = radius
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.10f * intensity), Color.Transparent),
            center = center + lightOffset
        ),
        radius = radius
    )
    drawCircle(
        color = accentColor.copy(alpha = 0.35f),
        radius = radius,
        style = Stroke(width = 1.dp.toPx())
    )
}
