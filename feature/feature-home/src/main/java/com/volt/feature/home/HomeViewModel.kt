package com.volt.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volt.core.domain.model.AppInfo
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
    val systemStats: SystemStats? = null,
    val isLoading: Boolean = true
)

sealed interface HomeUiIntent {
    data class ReorderGrid(val fromIndex: Int, val toIndex: Int) : HomeUiIntent
    data class OpenApp(val packageName: String) : HomeUiIntent
    data class OpenFolder(val folderId: String) : HomeUiIntent
    data object RefreshStats : HomeUiIntent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val getSystemStats: GetSystemStatsUseCase,
    private val reorderGrid: ReorderHomeGridUseCase,
    private val appRepository: com.volt.core.domain.repository.AppRepository
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

        // Observe system stats
        getSystemStats(pollIntervalMs = 3000L)
            .onEach { stats ->
                _uiState.value = _uiState.value.copy(systemStats = stats)
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
            HomeUiIntent.RefreshStats -> {
                // Automatically refreshed by Flow subscription
            }
        }
    }
}
