package com.volt.core.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.volt.core.ui.theme.LocalVoltTheme

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = LocalVoltTheme.current.blurRadius,
    opacity: Float = LocalVoltTheme.current.panelOpacity,
    borderColor: Color = LocalVoltTheme.current.panelBorder,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalVoltTheme.current
    val panelShape = RoundedCornerShape(cornerRadius)
    val basePanelColor = theme.bgPanel.copy(alpha = opacity)

    Box(
        modifier = modifier
            .border(1.dp, borderColor, panelShape)
            .clip(panelShape)
    ) {
        // Blurred background layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(basePanelColor)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
                        Modifier.blur(blurRadius)
                    } else {
                        Modifier
                    }
            )
        )

        content()
    }
}
