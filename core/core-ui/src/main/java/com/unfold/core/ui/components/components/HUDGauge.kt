package com.unfold.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfold.core.ui.theme.LocalUnfoldTheme

@Composable
fun HUDGauge(
    modifier: Modifier = Modifier,
    value: Float,
    label: String,
    valueText: String,
    ringColor: Color = LocalUnfoldTheme.current.accentPrimary,
    warningThreshold: Float? = 0.15f
) {
    val theme = LocalUnfoldTheme.current
    val gaugeColor = if (warningThreshold != null && value < warningThreshold) {
        theme.accentWarn
    } else {
        ringColor
    }

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            val strokeWidth = 6.dp.toPx()
            // 1. Draw track
            drawCircle(
                color = theme.panelBorder,
                style = Stroke(width = strokeWidth)
            )
            // 2. Draw progress arc
            drawArc(
                color = gaugeColor,
                startAngle = -90f,
                sweepAngle = 360f * value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valueText,
                color = theme.textPrimary,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label.uppercase(),
                color = theme.textSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}


