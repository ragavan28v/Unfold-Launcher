package com.unfold.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unfold.core.domain.model.ThemeConfig
import com.unfold.core.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeEditorUiState(
    val themeConfig: ThemeConfig = ThemeConfig(),
    val isLoading: Boolean = true
)

sealed interface ThemeEditorUiIntent {
    data class UpdatePrimaryColor(val hex: String) : ThemeEditorUiIntent
    data class UpdateSecondaryColor(val hex: String) : ThemeEditorUiIntent
    data class UpdateBevelIntensity(val intensity: Float) : ThemeEditorUiIntent
    data class UpdateBlurRadius(val radius: Float) : ThemeEditorUiIntent
    data class UpdatePanelOpacity(val opacity: Float) : ThemeEditorUiIntent
}

@HiltViewModel
class ThemeEditorViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeEditorUiState())
    val uiState: StateFlow<ThemeEditorUiState> = _uiState.asStateFlow()

    init {
        themeRepository.observeTheme()
            .onEach { config ->
                _uiState.value = ThemeEditorUiState(themeConfig = config, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ThemeEditorUiIntent) {
        val currentConfig = _uiState.value.themeConfig
        when (intent) {
            is ThemeEditorUiIntent.UpdatePrimaryColor -> {
                updateTheme(currentConfig.copy(accentPrimaryHex = intent.hex))
            }
            is ThemeEditorUiIntent.UpdateSecondaryColor -> {
                updateTheme(currentConfig.copy(accentSecondaryHex = intent.hex))
            }
            is ThemeEditorUiIntent.UpdateBevelIntensity -> {
                updateTheme(currentConfig.copy(bevelIntensity = intent.intensity))
            }
            is ThemeEditorUiIntent.UpdateBlurRadius -> {
                updateTheme(currentConfig.copy(blurRadiusDp = intent.radius))
            }
            is ThemeEditorUiIntent.UpdatePanelOpacity -> {
                updateTheme(currentConfig.copy(panelOpacity = intent.opacity))
            }
        }
    }

    private fun updateTheme(config: ThemeConfig) {
        viewModelScope.launch {
            themeRepository.updateTheme(config)
        }
    }
}

