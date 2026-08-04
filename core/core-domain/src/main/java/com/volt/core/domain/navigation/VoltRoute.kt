package com.volt.core.domain.navigation

sealed class VoltRoute(val route: String) {
    data object Home : VoltRoute("home")
    data object AppDrawer : VoltRoute("drawer")
    data class Folder(val folderId: String) : VoltRoute("folder/$folderId") {
        companion object { const val PATTERN = "folder/{folderId}" }
    }
    data object HiddenSpace : VoltRoute("hidden_space")
    data object HiddenFiles : VoltRoute("hidden_files")
    data object WidgetPicker : VoltRoute("widget_picker")
    data object Settings : VoltRoute("settings")
    data object ThemeEditor : VoltRoute("settings/theme")
    data object GestureSettings : VoltRoute("settings/gestures")
    data object GestureTrainer : VoltRoute("settings/gestures/train/{gestureType}") {
        const val PATTERN = "settings/gestures/train/{gestureType}"
    }
    data object IconPackPicker : VoltRoute("settings/icon_pack")
    data object BackupRestore : VoltRoute("settings/backup")
    data object UniversalSearch : VoltRoute("search")
    data object FocusMode : VoltRoute("focus")
}
