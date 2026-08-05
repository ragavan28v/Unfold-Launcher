package com.volt.feature.drawer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volt.core.domain.model.AppDrawerLayoutMode
import com.volt.core.domain.model.AppDrawerSortingMode
import com.volt.core.domain.model.AppDrawerStyleMode
import com.volt.core.domain.model.AppInfo
import com.volt.core.domain.model.AppDrawerSearchBarPosition
import com.volt.core.domain.model.AppDrawerViewMode
import com.volt.core.domain.model.DockRowsMode
import com.volt.core.domain.model.ThemeConfig
import com.volt.core.domain.usecase.GetInstalledAppsUseCase
import com.volt.core.domain.usecase.ToggleHiddenAppUseCase
import com.volt.core.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDrawerUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val filteredApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val viewMode: AppDrawerViewMode = AppDrawerViewMode.GRID,
    val searchBarPosition: AppDrawerSearchBarPosition = AppDrawerSearchBarPosition.TOP,
    val showKeyboardOnOpen: Boolean = true,
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val iconSize: Int = 64,
    val sortingMode: AppDrawerSortingMode = AppDrawerSortingMode.ALPHABETICAL,
    val styleMode: AppDrawerStyleMode = AppDrawerStyleMode.CARD,
    val dockRowsMode: DockRowsMode = DockRowsMode.ONE_ROW,
    val dockIconCount: Int = 6,
    val homeGridColumns: Int = 4,
    val homeGridRows: Int = 3,
    val layoutMode: AppDrawerLayoutMode = AppDrawerLayoutMode.ALPHABETIC_GRID
)

sealed interface AppDrawerUiIntent {
    data class Search(val query: String) : AppDrawerUiIntent
    data class OpenApp(val packageName: String) : AppDrawerUiIntent
    data class HideApp(val packageName: String) : AppDrawerUiIntent
    data class PinToHome(val packageName: String) : AppDrawerUiIntent
    data class PinToDock(val packageName: String) : AppDrawerUiIntent
    data class SetViewMode(val viewMode: AppDrawerViewMode) : AppDrawerUiIntent
    data class SetSearchBarPosition(val position: AppDrawerSearchBarPosition) : AppDrawerUiIntent
    data class SetShowKeyboardOnOpen(val enabled: Boolean) : AppDrawerUiIntent
}

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getInstalledApps: com.volt.core.domain.usecase.GetInstalledAppsUseCase,
    private val toggleHiddenApp: com.volt.core.domain.usecase.ToggleHiddenAppUseCase,
    private val appRepository: com.volt.core.domain.repository.AppRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val drawerPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            viewMode = loadViewMode(),
            searchBarPosition = loadSearchBarPosition(),
            showKeyboardOnOpen = loadShowKeyboardOnOpen()
        )
        themeRepository.observeTheme()
            .onEach { config ->
                _uiState.value = _uiState.value.copy(
                    gridColumns = config.appDrawerGridColumns.coerceIn(3, 6),
                    gridRows = config.appDrawerGridRows.coerceIn(1, 6),
                    iconSize = config.appDrawerIconSize.coerceIn(30, 100),
                    sortingMode = config.appDrawerSortingMode,
                    styleMode = config.appDrawerStyleMode,
                    dockRowsMode = config.dockRowsMode,
                    dockIconCount = config.dockIconCount.coerceIn(0, 6),
                    homeGridColumns = config.homeGridColumns.coerceIn(3, 6),
                    homeGridRows = config.homeGridRows.coerceIn(1, 3),
                    layoutMode = config.appDrawerLayoutMode,
                    filteredApps = filterApps(
                        _uiState.value.apps,
                        _uiState.value.searchQuery,
                        config.appDrawerSortingMode
                    )
                )
            }
            .launchIn(viewModelScope)
        getInstalledApps(includeHidden = false)
            .onEach { apps ->
                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    filteredApps = filterApps(apps, _uiState.value.searchQuery, _uiState.value.sortingMode),
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: AppDrawerUiIntent) {
        when (intent) {
            is AppDrawerUiIntent.Search -> {
                _uiState.value = _uiState.value.copy(
                    searchQuery = intent.query,
                    filteredApps = filterApps(_uiState.value.apps, intent.query, _uiState.value.sortingMode)
                )
            }
            is AppDrawerUiIntent.OpenApp -> {
                viewModelScope.launch {
                    appRepository.recordLaunch(intent.packageName)
                }
            }
            is AppDrawerUiIntent.HideApp -> {
                viewModelScope.launch {
                    toggleHiddenApp(intent.packageName, true)
                }
            }
            is AppDrawerUiIntent.PinToHome -> {
                viewModelScope.launch {
                    val occupiedPositions = _uiState.value.apps.mapNotNull { it.gridPosition }.toSet()
                    val capacity = (_uiState.value.homeGridColumns * _uiState.value.homeGridRows).coerceAtLeast(0)
                    val emptyIndex = (0 until capacity).firstOrNull { it !in occupiedPositions }
                    if (emptyIndex != null) {
                        appRepository.setGridPosition(intent.packageName, emptyIndex)
                    }
                }
            }
            is AppDrawerUiIntent.PinToDock -> {
                viewModelScope.launch {
                    val occupiedPositions = _uiState.value.apps.mapNotNull { it.gridPosition }.toSet()
                    val capacity = if (_uiState.value.dockRowsMode == DockRowsMode.TWO_ROWS) {
                        _uiState.value.dockIconCount * 2
                    } else {
                        _uiState.value.dockIconCount
                    }
                    val emptyIndex = (100 until (100 + capacity)).firstOrNull { it !in occupiedPositions }
                    if (emptyIndex != null) {
                        appRepository.setGridPosition(intent.packageName, emptyIndex)
                    }
                }
            }
            is AppDrawerUiIntent.SetViewMode -> {
                _uiState.value = _uiState.value.copy(viewMode = intent.viewMode)
                saveViewMode(intent.viewMode)
            }
            is AppDrawerUiIntent.SetSearchBarPosition -> {
                _uiState.value = _uiState.value.copy(searchBarPosition = intent.position)
                saveSearchBarPosition(intent.position)
            }
            is AppDrawerUiIntent.SetShowKeyboardOnOpen -> {
                _uiState.value = _uiState.value.copy(showKeyboardOnOpen = intent.enabled)
                saveShowKeyboardOnOpen(intent.enabled)
            }
        }
    }

    private fun filterApps(
        apps: List<AppInfo>,
        query: String,
        sortingMode: AppDrawerSortingMode
    ): List<AppInfo> {
        val filtered = if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
        return sortApps(filtered, sortingMode)
    }

    private fun sortApps(
        apps: List<AppInfo>,
        sortingMode: AppDrawerSortingMode
    ): List<AppInfo> {
        return when (sortingMode) {
            AppDrawerSortingMode.RECENT -> apps.sortedWith(
                compareByDescending<AppInfo> { it.lastUsedTimestamp }
                    .thenByDescending { it.launchCount }
                    .thenBy { it.label.lowercase() }
            )
            AppDrawerSortingMode.CUSTOM -> apps.sortedWith(
                compareBy<AppInfo> { it.gridPosition == null }
                    .thenBy { it.gridPosition ?: Int.MAX_VALUE }
                    .thenBy { it.label.lowercase() }
            )
            AppDrawerSortingMode.ALPHABETICAL -> apps.sortedBy { it.label.lowercase() }
        }
    }

    private fun loadViewMode(): AppDrawerViewMode {
        val stored = drawerPrefs.getString(KEY_VIEW_MODE, AppDrawerViewMode.GRID.name)
        return runCatching { AppDrawerViewMode.valueOf(stored ?: AppDrawerViewMode.GRID.name) }
            .getOrDefault(AppDrawerViewMode.GRID)
    }

    private fun saveViewMode(viewMode: AppDrawerViewMode) {
        drawerPrefs.edit().putString(KEY_VIEW_MODE, viewMode.name).apply()
    }

    private fun loadSearchBarPosition(): AppDrawerSearchBarPosition {
        val stored = drawerPrefs.getString(KEY_SEARCH_BAR_POSITION, AppDrawerSearchBarPosition.TOP.name)
        return runCatching { AppDrawerSearchBarPosition.valueOf(stored ?: AppDrawerSearchBarPosition.TOP.name) }
            .getOrDefault(AppDrawerSearchBarPosition.TOP)
    }

    private fun saveSearchBarPosition(position: AppDrawerSearchBarPosition) {
        drawerPrefs.edit().putString(KEY_SEARCH_BAR_POSITION, position.name).apply()
    }

    private fun loadShowKeyboardOnOpen(): Boolean {
        return drawerPrefs.getBoolean(KEY_SHOW_KEYBOARD_ON_OPEN, true)
    }

    private fun saveShowKeyboardOnOpen(enabled: Boolean) {
        drawerPrefs.edit().putBoolean(KEY_SHOW_KEYBOARD_ON_OPEN, enabled).apply()
    }

    private companion object {
        const val PREFS_NAME = "drawer_view_preferences"
        const val KEY_VIEW_MODE = "key_view_mode"
        const val KEY_SEARCH_BAR_POSITION = "key_search_bar_position"
        const val KEY_SHOW_KEYBOARD_ON_OPEN = "key_show_keyboard_on_open"
    }
}
