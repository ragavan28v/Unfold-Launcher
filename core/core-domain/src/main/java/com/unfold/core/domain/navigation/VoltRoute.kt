package com.unfold.core.domain.navigation

sealed class UnfoldRoute(val route: String) {
    data object Home : UnfoldRoute("home")
    data object AppDrawer : UnfoldRoute("drawer")
    data class Folder(val folderId: String) : UnfoldRoute("folder/$folderId") {
        companion object { const val PATTERN = "folder/{folderId}" }
    }
    data object HiddenSpace : UnfoldRoute("hidden_space")
    data object HiddenFiles : UnfoldRoute("hidden_files")
    data object WidgetPicker : UnfoldRoute("widget_picker")
    data object Settings : UnfoldRoute("settings")
    data object ThemeEditor : UnfoldRoute("settings/theme")
    data object GestureSettings : UnfoldRoute("settings/gestures")
    data object GestureTrainer : UnfoldRoute("settings/gestures/train/{gestureType}") {
        const val PATTERN = "settings/gestures/train/{gestureType}"
    }
    data object IconPackPicker : UnfoldRoute("settings/icon_pack")
    data object BackupRestore : UnfoldRoute("settings/backup")
    data object UniversalSearch : UnfoldRoute("search")
    data object FocusMode : UnfoldRoute("focus")
}


