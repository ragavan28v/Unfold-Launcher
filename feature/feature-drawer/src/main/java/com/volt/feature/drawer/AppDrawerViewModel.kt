package com.volt.feature.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volt.core.domain.model.AppInfo
import com.volt.core.domain.usecase.GetInstalledAppsUseCase
import com.volt.core.domain.usecase.ToggleHiddenAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isLoading: Boolean = true
)

sealed interface AppDrawerUiIntent {
    data class Search(val query: String) : AppDrawerUiIntent
    data class HideApp(val packageName: String) : AppDrawerUiIntent
    data class OpenApp(val packageName: String) : AppDrawerUiIntent
}

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val toggleHiddenApp: ToggleHiddenAppUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        getInstalledApps(includeHidden = false)
            .onEach { apps ->
                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    filteredApps = filterApps(apps, _uiState.value.searchQuery),
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
                    filteredApps = filterApps(_uiState.value.apps, intent.query)
                )
            }
            is AppDrawerUiIntent.HideApp -> {
                viewModelScope.launch {
                    toggleHiddenApp(intent.packageName, true)
                }
            }
            is AppDrawerUiIntent.OpenApp -> {
                // Launch handled directly in UI
            }
        }
    }

    private fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isEmpty()) return apps.sortedBy { it.label.lowercase() }
        return apps.filter {
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }.sortedBy { it.label.lowercase() }
    }
}
