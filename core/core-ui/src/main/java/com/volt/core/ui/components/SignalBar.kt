package com.volt.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.volt.core.ui.theme.LocalVoltTheme

@Composable
fun SignalBar(
    modifier: Modifier = Modifier,
    level: Int,
    activeColor: Color = LocalVoltTheme.current.accentPrimary
) {
    val theme = LocalVoltTheme.current
    val barCount = 4

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..barCount) {
            val isActive = i <= level
            val color = if (isActive) activeColor else theme.panelBorder
            val barHeight = (4 * i + 4).dp

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}
