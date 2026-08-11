package com.unfold.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unfold.core.domain.model.ActionType
import com.unfold.core.domain.model.AppDrawerLayoutMode
import com.unfold.core.domain.model.AppDrawerSortingMode
import com.unfold.core.domain.model.AppDrawerStyleMode
import com.unfold.core.domain.model.AppDrawerSearchBarPosition
import com.unfold.core.domain.model.AppDrawerViewMode
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.model.DockBackgroundMode
import com.unfold.core.domain.model.DockRowsMode
import com.unfold.core.domain.model.GestureBinding
import com.unfold.core.domain.model.GestureType
import com.unfold.core.domain.model.HomeAppPlacementMode
import com.unfold.core.domain.model.ThemeConfig
import com.unfold.core.domain.repository.AppRepository
import com.unfold.core.domain.repository.GestureRepository
import com.unfold.core.domain.repository.ThemeRepository
import com.unfold.core.domain.usecase.GetInstalledAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LauncherSettingsUiState(
    val installedApps: List<AppInfo> = emptyList(),
    val gestureBindings: Map<GestureType, GestureBinding> = emptyMap(),
    val themeConfig: ThemeConfig = ThemeConfig(),
    val drawerViewMode: AppDrawerViewMode = AppDrawerViewMode.GRID,
    val drawerSearchBarPosition: AppDrawerSearchBarPosition = AppDrawerSearchBarPosition.TOP,
    val drawerShowKeyboardOnOpen: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class LauncherSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val appRepository: AppRepository,
    private val gestureRepository: GestureRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val drawerPrefs = context.getSharedPreferences(DRAWER_PREFS_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(LauncherSettingsUiState())
    val uiState: StateFlow<LauncherSettingsUiState> = _uiState.asStateFlow()

    init {
        themeRepository.observeTheme()
            .onEach { config ->
                _uiState.value = _uiState.value.copy(themeConfig = config)
            }
            .launchIn(viewModelScope)

        getInstalledApps(includeHidden = false)
            .onEach { apps ->
                _uiState.value = _uiState.value.copy(
                    installedApps = apps.sortedBy { it.label.lowercase() },
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)

        gestureRepository.observeBindings()
            .onEach { bindings ->
                _uiState.value = _uiState.value.copy(
                    gestureBindings = bindings.associateBy { it.gestureType }
                )
            }
            .launchIn(viewModelScope)

        _uiState.value = _uiState.value.copy(
            drawerViewMode = loadDrawerViewMode(),
            drawerSearchBarPosition = loadDrawerSearchBarPosition(),
            drawerShowKeyboardOnOpen = loadDrawerShowKeyboardOnOpen()
        )
    }

    fun saveGestureBinding(
        gestureType: GestureType,
        actionType: ActionType,
        targetPackage: String? = null,
        targetShortcutId: String? = null
    ) {
        viewModelScope.launch {
            gestureRepository.setBinding(
                GestureBinding(
                    gestureType = gestureType,
                    actionType = actionType,
                    targetPackage = targetPackage,
                    targetShortcutId = targetShortcutId,
                    isUserModified = true
                )
            )
        }
    }

    fun updateThemeConfig(config: ThemeConfig) {
        viewModelScope.launch {
            val previous = _uiState.value.themeConfig
            themeRepository.updateTheme(config)
            if (shouldRepackHome(previous, config)) {
                normalizeHomeApps(config)
            }
            if (shouldRepackDock(previous, config)) {
                normalizeDockApps(config)
            }
        }
    }

    fun resetLauncherSettings() {
        viewModelScope.launch {
            themeRepository.resetTheme()
            gestureRepository.resetToDefaults()
            drawerPrefs.edit().clear().apply()
            appRepository.refreshFromPackageManager()
            _uiState.value = _uiState.value.copy(
                themeConfig = ThemeConfig(),
                drawerViewMode = AppDrawerViewMode.GRID,
                drawerSearchBarPosition = AppDrawerSearchBarPosition.TOP,
                drawerShowKeyboardOnOpen = true
            )
        }
    }

    fun setDrawerViewMode(viewMode: AppDrawerViewMode) {
        _uiState.value = _uiState.value.copy(drawerViewMode = viewMode)
        drawerPrefs.edit().putString(KEY_DRAWER_VIEW_MODE, viewMode.name).apply()
    }

    fun setDrawerSearchBarPosition(position: AppDrawerSearchBarPosition) {
        _uiState.value = _uiState.value.copy(drawerSearchBarPosition = position)
        drawerPrefs.edit().putString(KEY_DRAWER_SEARCH_BAR_POSITION, position.name).apply()
    }

    fun setDrawerShowKeyboardOnOpen(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(drawerShowKeyboardOnOpen = enabled)
        drawerPrefs.edit().putBoolean(KEY_DRAWER_SHOW_KEYBOARD, enabled).apply()
    }

    private suspend fun normalizeHomeApps(config: ThemeConfig) {
        if (config.homeAppPlacementMode != HomeAppPlacementMode.AUTO_ARRANGE) return

        val ordered = _uiState.value.installedApps
            .mapNotNull { app ->
                val position = app.gridPosition ?: return@mapNotNull null
                if (position in 0 until 100) app to position else null
            }
            .sortedBy { it.second }

        for ((index, item) in ordered.withIndex()) {
            val app = item.first
            val targetPosition = index
            appRepository.setGridPosition(app.appId, targetPosition)
        }
    }

    private suspend fun normalizeDockApps(config: ThemeConfig) {
        if (config.dockRowsMode == DockRowsMode.HIDDEN) return

        val ordered = _uiState.value.installedApps
            .mapNotNull { app ->
                val position = app.gridPosition ?: return@mapNotNull null
                if (position >= 100) app to position else null
            }
            .sortedBy { it.second }

        for ((index, item) in ordered.withIndex()) {
            val app = item.first
            val targetPosition = 100 + index
            appRepository.setGridPosition(app.appId, targetPosition)
        }
    }

    private fun shouldRepackHome(previous: ThemeConfig, next: ThemeConfig): Boolean {
        return previous.homeGridColumns != next.homeGridColumns ||
            previous.homeGridRows != next.homeGridRows ||
            (previous.homeAppPlacementMode != next.homeAppPlacementMode && next.homeAppPlacementMode == HomeAppPlacementMode.AUTO_ARRANGE)
    }

    private fun shouldRepackDock(previous: ThemeConfig, next: ThemeConfig): Boolean {
        return previous.dockRowsMode != next.dockRowsMode ||
            previous.dockIconCount != next.dockIconCount
    }

    private fun loadDrawerViewMode(): AppDrawerViewMode {
        val stored = drawerPrefs.getString(KEY_DRAWER_VIEW_MODE, AppDrawerViewMode.GRID.name)
        return runCatching { AppDrawerViewMode.valueOf(stored ?: AppDrawerViewMode.GRID.name) }
            .getOrDefault(AppDrawerViewMode.GRID)
    }

    private fun loadDrawerSearchBarPosition(): AppDrawerSearchBarPosition {
        val stored = drawerPrefs.getString(KEY_DRAWER_SEARCH_BAR_POSITION, AppDrawerSearchBarPosition.TOP.name)
        return runCatching { AppDrawerSearchBarPosition.valueOf(stored ?: AppDrawerSearchBarPosition.TOP.name) }
            .getOrDefault(AppDrawerSearchBarPosition.TOP)
    }

    private fun loadDrawerShowKeyboardOnOpen(): Boolean {
        return drawerPrefs.getBoolean(KEY_DRAWER_SHOW_KEYBOARD, true)
    }

    private companion object {
        const val DRAWER_PREFS_NAME = "drawer_view_preferences"
        const val KEY_DRAWER_VIEW_MODE = "key_view_mode"
        const val KEY_DRAWER_SEARCH_BAR_POSITION = "key_search_bar_position"
        const val KEY_DRAWER_SHOW_KEYBOARD = "key_show_keyboard_on_open"
    }
}

