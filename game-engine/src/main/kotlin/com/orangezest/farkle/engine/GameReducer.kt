package com.orangezest.farkle.engine

class GameReducer(
    private val scoringEngine: ScoringEngine,
    private val diceRoller: DiceRoller,
) {
    fun reduce(state: GameUiState, event: GameEvent): GameUiState = when (event) {
        is GameEvent.RollDice -> handleRoll(state)
        is GameEvent.ToggleDie -> handleToggle(state, event.index)
        is GameEvent.Bank -> handleBank(state)
        is GameEvent.Undo -> handleUndo(state)
        is GameEvent.AcknowledgeFarkle -> handleAcknowledgeFarkle(state)
        is GameEvent.ReadyForTurn -> handleReadyForTurn(state)
        is GameEvent.StartFresh -> handleStartFresh(state)
        is GameEvent.StealRoll -> handleStealRoll(state, event)
    }

    private fun handleRoll(state: GameUiState): GameUiState {
        val phase = state.turnPhase
        var currentRunning = state.runningTotal
        var diceCount = state.remainingDiceCount
        var keptDice = state.diceKept

        if (phase is TurnPhase.SelectingDice && phase.selectedIndices.isNotEmpty()) {
            val selectedDice = phase.selectedIndices.map { phase.rollResult[it] }
            val selectionScore = scoringEngine.scoreSelection(phase.rollResult, selectedDice)
            currentRunning += selectionScore
            keptDice = keptDice + selectedDice
            diceCount -= phase.selectedIndices.size

            if (diceCount == 0 && state.config.hotDiceEnabled) {
                diceCount = 6
                keptDice = emptyList()
            }
        }

        if (diceCount == 0) return state

        val stateWithHistory = state.copy(
            stateHistory = state.stateHistory + state.copy(stateHistory = emptyList())
        )
        val roll = diceRoller.roll(diceCount)

        return if (scoringEngine.isFarkle(roll)) {
            stateWithHistory.copy(
                turnPhase = TurnPhase.Farkled(lostPoints = currentRunning),
                runningTotal = 0,
                remainingDiceCount = diceCount,
                diceKept = emptyList(),
            )
        } else {
            stateWithHistory.copy(
                turnPhase = TurnPhase.SelectingDice(rollResult = roll),
                runningTotal = currentRunning,
                remainingDiceCount = diceCount,
                diceKept = keptDice,
            )
        }
    }

    private fun handleToggle(state: GameUiState, index: Int): GameUiState {
        val phase = state.turnPhase as? TurnPhase.SelectingDice ?: return state
        val newSelected = if (index in phase.selectedIndices) {
            phase.selectedIndices - index
        } else {
            phase.selectedIndices + index
        }
        val selectedDice = newSelected.map { phase.rollResult[it] }
        val turnScore = if (selectedDice.isEmpty()) 0
            else scoringEngine.scoreSelection(phase.rollResult, selectedDice)
        return state.copy(
            turnPhase = phase.copy(selectedIndices = newSelected, turnScore = turnScore),
        )
    }

    private fun handleBank(state: GameUiState): GameUiState {
        val phase = state.turnPhase as? TurnPhase.SelectingDice ?: return state
        if (phase.selectedIndices.isEmpty()) return state

        val selectedDice = phase.selectedIndices.map { phase.rollResult[it] }
        val selectionScore = scoringEngine.scoreSelection(phase.rollResult, selectedDice)
        if (selectionScore == 0) return state

        val totalTurnScore = state.runningTotal + selectionScore
        val player = state.currentPlayer

        if (!player.isOnBoard && totalTurnScore < state.config.minimumToBoard) {
            return state
        }

        val updatedPlayer = player.copy(
            totalScore = player.totalScore + totalTurnScore,
            isOnBoard = true,
        )
        val updatedPlayers = state.players.toMutableList().apply {
            set(state.currentPlayerIndex, updatedPlayer)
        }

        val remainingDice = (state.remainingDiceCount - phase.selectedIndices.size).let {
            if (it == 0) 6 else it
        }

        if (updatedPlayer.totalScore >= state.config.targetScore && state.finalRoundTriggerIndex == null) {
            return state.copy(
                players = updatedPlayers,
                turnPhase = TurnPhase.PassingDevice,
                runningTotal = 0,
                remainingDiceCount = 6,
                lastBankedAmount = totalTurnScore,
                lastBankedDiceRemaining = remainingDice,
                stateHistory = emptyList(),
                finalRoundTriggerIndex = state.currentPlayerIndex,
            )
        }

        return state.copy(
            players = updatedPlayers,
            turnPhase = TurnPhase.PassingDevice,
            runningTotal = 0,
            remainingDiceCount = 6,
            lastBankedAmount = totalTurnScore,
            lastBankedDiceRemaining = remainingDice,
            stateHistory = emptyList(),
        )
    }

    private fun handleUndo(state: GameUiState): GameUiState {
        if (state.stateHistory.isEmpty()) return state
        return state.stateHistory.last()
    }

    private fun handleAcknowledgeFarkle(state: GameUiState): GameUiState {
        if (state.turnPhase !is TurnPhase.Farkled) return state
        return state.copy(
            turnPhase = TurnPhase.PassingDevice,
            lastBankedAmount = 0,
            lastBankedDiceRemaining = 0,
        )
    }

    private fun handleReadyForTurn(state: GameUiState): GameUiState {
        if (state.turnPhase != TurnPhase.PassingDevice) return state
        val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size

        if (state.finalRoundTriggerIndex != null && nextPlayerIndex == state.finalRoundTriggerIndex) {
            val winnerIndex = state.players.indices.maxBy { state.players[it].totalScore }
            return state.copy(
                turnPhase = TurnPhase.GameOver(winnerIndex = winnerIndex),
            )
        }

        if (state.config.piggybackingEnabled && state.lastBankedAmount > 0) {
            return state.copy(
                currentPlayerIndex = nextPlayerIndex,
                turnPhase = TurnPhase.OfferSteal(
                    remainingDice = state.lastBankedDiceRemaining,
                    previousTotal = state.lastBankedAmount,
                ),
                runningTotal = 0,
                remainingDiceCount = 6,
                diceKept = emptyList(),
                stateHistory = emptyList(),
            )
        }

        return state.copy(
            currentPlayerIndex = nextPlayerIndex,
            turnPhase = TurnPhase.WaitingToRoll,
            runningTotal = 0,
            remainingDiceCount = 6,
            diceKept = emptyList(),
            stateHistory = emptyList(),
        )
    }

    private fun handleStartFresh(state: GameUiState): GameUiState {
        return state.copy(
            turnPhase = TurnPhase.WaitingToRoll,
            runningTotal = 0,
            remainingDiceCount = 6,
        )
    }

    private fun handleStealRoll(state: GameUiState, event: GameEvent.StealRoll): GameUiState {
        return state.copy(
            turnPhase = TurnPhase.WaitingToRoll,
            runningTotal = event.previousTotal,
            remainingDiceCount = event.remainingDice,
        )
    }
}
