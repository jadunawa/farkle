package com.orangezest.farkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orangezest.farkle.data.SettingsRepository
import com.orangezest.farkle.engine.GameConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val gameConfig: StateFlow<GameConfig> = repository.gameConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GameConfig.DEFAULT)

    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hapticEnabled: StateFlow<Boolean> = repository.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<String> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun updateTargetScore(value: Int) = viewModelScope.launch { repository.updateTargetScore(value) }
    fun updateMinimumToBoard(value: Int) = viewModelScope.launch { repository.updateMinimumToBoard(value) }
    fun updateHotDice(enabled: Boolean) = viewModelScope.launch { repository.updateHotDice(enabled) }
    fun updatePiggybacking(enabled: Boolean) = viewModelScope.launch { repository.updatePiggybacking(enabled) }
    fun updateSoundEnabled(enabled: Boolean) = viewModelScope.launch { repository.updateSoundEnabled(enabled) }
    fun updateHapticEnabled(enabled: Boolean) = viewModelScope.launch { repository.updateHapticEnabled(enabled) }
    fun updateThemeMode(mode: String) = viewModelScope.launch { repository.updateThemeMode(mode) }
}
