package com.unfold.feature.hiddenspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.usecase.GetInstalledAppsUseCase
import com.unfold.core.domain.usecase.ToggleHiddenAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HiddenSpaceUiState(
    val allApps: List<AppInfo> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HiddenSpaceViewModel @Inject constructor(
    getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val toggleHiddenAppUseCase: ToggleHiddenAppUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HiddenSpaceUiState())
    val uiState: StateFlow<HiddenSpaceUiState> = _uiState.asStateFlow()

    init {
        getInstalledAppsUseCase(includeHidden = true)
            .onEach { apps ->
                _uiState.value = HiddenSpaceUiState(
                    allApps = apps.sortedBy { it.label.lowercase() },
                    hiddenApps = apps.filter { it.isHidden }.sortedBy { it.label.lowercase() },
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun setHidden(appId: String, hidden: Boolean) {
        viewModelScope.launch {
            toggleHiddenAppUseCase(appId, hidden)
        }
    }
}
