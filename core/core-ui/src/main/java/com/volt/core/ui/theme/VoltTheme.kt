package com.volt.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class VoltThemeColors(
    val bgVoid: Color = BgVoid,
    val bgPanel: Color = BgPanel,
    val panelBorder: Color = BgPanelBorder,
    val surfaceIconBase: Color = SurfaceIconBase,
    val accentPrimary: Color = AccentPrimary,
    val accentSecondary: Color = AccentSecondary,
    val accentWarn: Color = AccentWarn,
    val accentDanger: Color = AccentDanger,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted,
    val bevelIntensity: Float = 0.6f,
    val blurRadius: Dp = 24.dp,
    val panelOpacity: Float = 0.72f
)

val LocalVoltTheme = staticCompositionLocalOf { VoltThemeColors() }

object VoltTheme {
    val current: VoltThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVoltTheme.current
}

@Composable
fun VoltTheme(
    colors: VoltThemeColors = VoltThemeColors(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalVoltTheme provides colors) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                background = colors.bgVoid,
                surface = colors.bgPanel,
                primary = colors.accentPrimary,
                secondary = colors.accentSecondary,
                error = colors.accentDanger
            ),
            typography = VoltTypography,
            shapes = VoltShapes,
            content = content
        )
    }
}
