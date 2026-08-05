package com.volt.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volt.core.domain.model.AppInfo
import com.volt.core.domain.model.DockRowsMode
import com.volt.core.domain.model.DockBackgroundMode
import com.volt.core.domain.model.GestureBinding
import com.volt.core.domain.model.GestureType
import com.volt.core.domain.model.FolderInfo
import com.volt.core.domain.model.SystemStats
import com.volt.core.domain.usecase.GetInstalledAppsUseCase
import com.volt.core.domain.usecase.GetSystemStatsUseCase
import com.volt.core.domain.usecase.ReorderHomeGridUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomePanelType {
    CLOCK_WEATHER,
    SYSTEM_HUD,
    MEDIA,
    FLASHLIGHT
}

data class HomeUiState(
    val panels: List<HomePanelType> = listOf(HomePanelType.CLOCK_WEATHER, HomePanelType.SYSTEM_HUD),
    val gridApps: List<AppInfo> = emptyList(),
    val folders: List<FolderInfo> = emptyList(),
    val gestureBindings: Map<GestureType, GestureBinding> = emptyMap(),
    val systemStats: SystemStats? = null,
    val isLoading: Boolean = true,
    val gridColumns: Int = 4,
    val gridRows: Int = 3,
    val homeIconSize: Int = 72,
    val homeLabelsEnabled: Boolean = true,
    val dockIconSize: Int = 56,
    val dockIconCount: Int = 6,
    val dockRowsMode: DockRowsMode = DockRowsMode.ONE_ROW,
    val dockBackgroundMode: DockBackgroundMode = DockBackgroundMode.DEFAULT,
    val dockBackgroundHex: String = "#12161E"
)

sealed interface HomeUiIntent {
    data class ReorderGrid(val fromIndex: Int, val toIndex: Int) : HomeUiIntent
    data class OpenApp(val packageName: String) : HomeUiIntent
    data class OpenFolder(val folderId: String) : HomeUiIntent
    data class UnpinApp(val packageName: String) : HomeUiIntent
    data class MoveApp(val packageName: String, val targetPosition: Int?) : HomeUiIntent
    data object RefreshStats : HomeUiIntent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val getSystemStats: GetSystemStatsUseCase,
    private val reorderGrid: ReorderHomeGridUseCase,
    private val gestureRepository: com.volt.core.domain.repository.GestureRepository,
    private val appRepository: com.volt.core.domain.repository.AppRepository,
    private val themeRepository: com.volt.core.domain.repository.ThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Sync package manager apps on start
        viewModelScope.launch {
            try {
                appRepository.refreshFromPackageManager()
            } catch (e: Exception) {
                // Ignore sync errors on startup
            }
        }

        // Observe installed launchable apps
        getInstalledApps(includeHidden = false)
            .onEach { apps ->
                val gridApps = apps.filter { it.gridPosition != null }.sortedBy { it.gridPosition }
                _uiState.value = _uiState.value.copy(
                    gridApps = gridApps,
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

        // Observe system stats
        getSystemStats(pollIntervalMs = 3000L)
            .onEach { stats ->
                _uiState.value = _uiState.value.copy(systemStats = stats)
            }
            .launchIn(viewModelScope)

        // Observe theme config to get dynamic column count (N)
        themeRepository.observeTheme()
            .onEach { themeConfig ->
                _uiState.value = _uiState.value.copy(
                    gridColumns = themeConfig.homeGridColumns.coerceIn(3, 6),
                    gridRows = themeConfig.homeGridRows.coerceIn(1, 3),
                    homeIconSize = themeConfig.homeIconSize.coerceIn(30, 100),
                    homeLabelsEnabled = themeConfig.homeLabelsEnabled,
                    dockIconSize = themeConfig.dockIconSize.coerceIn(30, 100),
                    dockIconCount = themeConfig.dockIconCount.coerceIn(0, 6),
                    dockRowsMode = themeConfig.dockRowsMode,
                    dockBackgroundMode = themeConfig.dockBackgroundMode,
                    dockBackgroundHex = themeConfig.dockBackgroundHex
                )
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: HomeUiIntent) {
        when (intent) {
            is HomeUiIntent.ReorderGrid -> {
                viewModelScope.launch {
                    val list = _uiState.value.gridApps.toMutableList()
                    if (intent.fromIndex in list.indices && intent.toIndex in list.indices) {
                        val item = list.removeAt(intent.fromIndex)
                        list.add(intent.toIndex, item)
                        list.forEachIndexed { index, app ->
                            reorderGrid(app.packageName, index)
                        }
                    }
                }
            }
            is HomeUiIntent.OpenApp -> {
                // Handled in UI navigation or launch activity
            }
            is HomeUiIntent.OpenFolder -> {
                // Handled in UI navigation
            }
            is HomeUiIntent.UnpinApp -> {
                viewModelScope.launch {
                    appRepository.setGridPosition(intent.packageName, null)
                }
            }
            is HomeUiIntent.MoveApp -> {
                viewModelScope.launch {
                    appRepository.setGridPosition(intent.packageName, intent.targetPosition)
                }
            }
            HomeUiIntent.RefreshStats -> {
                // Automatically refreshed by Flow subscription
            }
        }
    }
}
