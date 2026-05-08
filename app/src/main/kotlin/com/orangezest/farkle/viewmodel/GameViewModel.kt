package com.orangezest.farkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orangezest.farkle.data.GameStateRepository
import com.orangezest.farkle.engine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val reducer: GameReducer,
    private val gameStateRepository: GameStateRepository,
    val scoringEngine: ScoringEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(players = emptyList()))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val hasSavedGame: StateFlow<Boolean> = gameStateRepository.savedGame
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startGame(playerNames: List<String>) {
        _uiState.value = GameUiState(
            players = playerNames.map { Player(name = it) },
        )
    }

    fun resumeGame() {
        viewModelScope.launch {
            gameStateRepository.savedGame.first()?.let { saved ->
                _uiState.value = saved
            }
        }
    }

    fun onEvent(event: GameEvent) {
        _uiState.value = reducer.reduce(_uiState.value, event)

        if (_uiState.value.turnPhase == TurnPhase.PassingDevice) {
            viewModelScope.launch {
                gameStateRepository.saveGame(_uiState.value)
            }
        }
    }
}
