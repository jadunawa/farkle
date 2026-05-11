package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.*
import kotlinx.coroutines.delay

@Composable
fun AdaptiveGameScreen(
    state: GameUiState,
    scoringEngine: ScoringEngine,
    onEvent: (GameEvent) -> Unit,
    onNewGame: () -> Unit,
    windowWidthClass: WindowWidthSizeClass,
) {
    if (windowWidthClass == WindowWidthSizeClass.Expanded) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Scoreboard(
                players = state.players,
                currentPlayerIndex = state.currentPlayerIndex,
                minimumToBoard = state.config.minimumToBoard,
                modifier = Modifier.width(200.dp),
            )
            GameScreen(
                state = state,
                scoringEngine = scoringEngine,
                onEvent = onEvent,
                onNewGame = onNewGame,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        GameScreen(
            state = state,
            scoringEngine = scoringEngine,
            onEvent = onEvent,
            onNewGame = onNewGame,
        )
    }
}

@Composable
fun GameScreen(
    state: GameUiState,
    scoringEngine: ScoringEngine,
    onEvent: (GameEvent) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationAngle = remember(state.currentPlayerIndex, state.players.size) {
        when (state.players.size) {
            2 -> if (state.currentPlayerIndex == 1) 180f else 0f
            else -> state.currentPlayerIndex * 90f
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Scoreboard(
            players = state.players,
            currentPlayerIndex = state.currentPlayerIndex,
            minimumToBoard = state.config.minimumToBoard,
            modifier = Modifier.rotate(rotationAngle),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center,
        ) {
            when (val phase = state.turnPhase) {
                is TurnPhase.WaitingToRoll -> {
                    Button(
                        onClick = { onEvent(GameEvent.RollDice) },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(64.dp),
                    ) {
                        Text("Roll Dice", style = MaterialTheme.typography.titleLarge)
                    }
                }

                is TurnPhase.SelectingDice -> {
                    val selectedDice = phase.selectedIndices.map { phase.rollResult[it] }
                    val selectionScore = if (selectedDice.isNotEmpty()) {
                        scoringEngine.scoreSelection(phase.rollResult, selectedDice)
                    } else 0
                    val totalIfBanked = state.runningTotal + selectionScore
                    val canBank = phase.selectedIndices.isNotEmpty() &&
                        selectionScore > 0 &&
                        (state.currentPlayer.isOnBoard || totalIfBanked >= state.config.minimumToBoard)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (state.runningTotal > 0) {
                            Text(
                                text = "Running: ${state.runningTotal}",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }

                        DiceArea(
                            phase = phase,
                            scoringEngine = scoringEngine,
                            onToggleDie = { onEvent(GameEvent.ToggleDie(it)) },
                            diceKept = state.diceKept,
                        )

                        ActionBar(
                            canRoll = phase.selectedIndices.isNotEmpty() && selectionScore > 0,
                            canBank = canBank,
                            bankAmount = totalIfBanked,
                            onRoll = { onEvent(GameEvent.RollDice) },
                            onBank = { onEvent(GameEvent.Bank) },
                            onUndo = { onEvent(GameEvent.Undo) },
                            canUndo = state.stateHistory.isNotEmpty(),
                        )
                    }
                }

                is TurnPhase.Farkled -> {
                    FarkleOverlay(
                        lostPoints = phase.lostPoints,
                        onDismiss = { onEvent(GameEvent.AcknowledgeFarkle) },
                    )
                }

                is TurnPhase.PassingDevice -> {
                    val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size
                    Text(
                        text = "${state.players[nextPlayerIndex].name}'s turn",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    LaunchedEffect(Unit) {
                        delay(800)
                        onEvent(GameEvent.ReadyForTurn)
                    }
                }

                is TurnPhase.OfferSteal -> {
                    PassDeviceScreen(
                        nextPlayerName = state.currentPlayer.name,
                        offerSteal = phase,
                        onReady = { onEvent(GameEvent.StartFresh) },
                        onSteal = { onEvent(GameEvent.StealRoll(phase.remainingDice, phase.previousTotal)) },
                    )
                }

                is TurnPhase.GameOver -> {
                    GameOverScreen(
                        winner = state.players[phase.winnerIndex],
                        players = state.players,
                        onNewGame = onNewGame,
                    )
                }
            }
        }
    }
}
