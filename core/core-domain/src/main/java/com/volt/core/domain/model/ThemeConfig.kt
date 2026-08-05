package com.volt.core.domain.model

enum class HomeAppPlacementMode {
    AUTO_ARRANGE,
    MANUAL_PLACEMENT,
    LOCK_LAYOUT
}

enum class DockRowsMode {
    ONE_ROW,
    TWO_ROWS,
    HIDDEN
}

enum class DockBackgroundMode {
    DEFAULT,
    SOLID,
    TRANSPARENT,
    BLUR
}

enum class AppDrawerLayoutMode {
    VERTICAL_LIST,
    HORIZONTAL_PAGES,
    ALPHABETIC_GRID
}

enum class AppDrawerSortingMode {
    ALPHABETICAL,
    RECENT,
    CUSTOM
}

enum class AppDrawerStyleMode {
    OPEN,
    CLOSED,
    TRANSPARENT,
    BLUR,
    CARD
}

data class ThemeConfig(
    val accentPrimaryHex: String = "#38BDF8",
    val accentSecondaryHex: String = "#6366F1",
    val bevelIntensity: Float = 0.6f,
    val blurRadiusDp: Float = 24f,
    val panelOpacity: Float = 0.72f,
    val timeAdaptiveHue: Boolean = true,
    val reducedMotion: Boolean = false,
    val homeGridColumns: Int = 4,
    val homeGridRows: Int = 3,
    val homeIconSize: Int = 72,
    val homeLabelsEnabled: Boolean = true,
    val homeAppPlacementMode: HomeAppPlacementMode = HomeAppPlacementMode.AUTO_ARRANGE,
    val dockRowsMode: DockRowsMode = DockRowsMode.ONE_ROW,
    val dockIconCount: Int = 6,
    val dockIconSize: Int = 56,
    val dockLabelsEnabled: Boolean = true,
    val dockBackgroundMode: DockBackgroundMode = DockBackgroundMode.DEFAULT,
    val dockBackgroundHex: String = "#12161E",
    val appDrawerLayoutMode: AppDrawerLayoutMode = AppDrawerLayoutMode.ALPHABETIC_GRID,
    val appDrawerGridRows: Int = 5,
    val appDrawerGridColumns: Int = 4,
    val appDrawerIconSize: Int = 64,
    val appDrawerSortingMode: AppDrawerSortingMode = AppDrawerSortingMode.ALPHABETICAL,
    val appDrawerStyleMode: AppDrawerStyleMode = AppDrawerStyleMode.CARD,
    val iconPackPackage: String = "",
    val soundFeedbackEnabled: Boolean = false
)
