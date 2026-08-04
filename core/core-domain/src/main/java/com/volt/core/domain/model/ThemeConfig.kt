package com.volt.core.domain.model

data class ThemeConfig(
    val accentPrimaryHex: String = "#38BDF8",
    val accentSecondaryHex: String = "#6366F1",
    val bevelIntensity: Float = 0.6f,
    val blurRadiusDp: Float = 24f,
    val panelOpacity: Float = 0.72f,
    val timeAdaptiveHue: Boolean = true,
    val reducedMotion: Boolean = false,
    val gridColumns: Int = 4,
    val gridRows: Int = 6,
    val iconPackPackage: String = "",
    val soundFeedbackEnabled: Boolean = false
)
