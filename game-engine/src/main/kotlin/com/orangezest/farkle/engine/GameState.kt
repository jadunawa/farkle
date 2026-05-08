package com.orangezest.farkle.engine

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val name: String,
    val totalScore: Int = 0,
    val isOnBoard: Boolean = false,
)

@Serializable
sealed interface TurnPhase {
    @Serializable
    data object WaitingToRoll : TurnPhase

    @Serializable
    data class SelectingDice(
        val rollResult: List<Int>,
        val selectedIndices: Set<Int> = emptySet(),
        val turnScore: Int = 0,
    ) : TurnPhase

    @Serializable
    data class Farkled(val lostPoints: Int) : TurnPhase

    @Serializable
    data class OfferSteal(
        val remainingDice: Int,
        val previousTotal: Int,
    ) : TurnPhase

    @Serializable
    data object PassingDevice : TurnPhase

    @Serializable
    data class GameOver(val winnerIndex: Int) : TurnPhase
}

@Serializable
data class GameUiState(
    val players: List<Player>,
    val currentPlayerIndex: Int = 0,
    val turnPhase: TurnPhase = TurnPhase.WaitingToRoll,
    val runningTotal: Int = 0,
    val diceKept: List<Int> = emptyList(),
    val remainingDiceCount: Int = 6,
    val config: GameConfig = GameConfig.DEFAULT,
    @kotlinx.serialization.Transient
    val stateHistory: List<GameUiState> = emptyList(),
    val lastBankedAmount: Int = 0,
    val lastBankedDiceRemaining: Int = 0,
    val finalRoundTriggerIndex: Int? = null,
) {
    init {
        require(players.isEmpty() || currentPlayerIndex in players.indices) {
            "currentPlayerIndex $currentPlayerIndex out of bounds for ${players.size} players"
        }
    }
    val currentPlayer: Player get() = players[currentPlayerIndex]
}
