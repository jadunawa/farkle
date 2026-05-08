package com.orangezest.farkle.engine

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameReducerTest {

    private val twoPlayers = listOf(Player("Alice"), Player("Bob"))
    private val fakeDice = FakeDiceRoller(listOf(1, 5, 3, 4, 6, 2))

    private fun reducer(diceRoller: DiceRoller = fakeDice) =
        GameReducer(ScoringEngine(GameConfig.DEFAULT), diceRoller)

    private fun initialState(players: List<Player> = twoPlayers) =
        GameUiState(players = players)

    @Nested
    inner class RollDice {
        @Test
        fun `transitions from WaitingToRoll to SelectingDice`() {
            val state = initialState()
            val newState = reducer().reduce(state, GameEvent.RollDice)
            assertIs<TurnPhase.SelectingDice>(newState.turnPhase)
        }

        @Test
        fun `roll result contains correct number of dice`() {
            val state = initialState()
            val newState = reducer().reduce(state, GameEvent.RollDice)
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertEquals(6, phase.rollResult.size)
        }

        @Test
        fun `rolls only remaining dice after keeping some`() {
            val state = initialState().copy(remainingDiceCount = 3)
            val roller = FakeDiceRoller(listOf(1, 5, 3))
            val newState = reducer(roller).reduce(state, GameEvent.RollDice)
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertEquals(3, phase.rollResult.size)
        }

        @Test
        fun `farkle when no scoring dice rolled`() {
            val noScoreRoller = FakeDiceRoller(listOf(2, 3, 4, 6, 6, 4))
            val state = initialState()
            val newState = reducer(noScoreRoller).reduce(state, GameEvent.RollDice)
            assertIs<TurnPhase.Farkled>(newState.turnPhase)
        }

        @Test
        fun `farkle loses running total`() {
            val noScoreRoller = FakeDiceRoller(listOf(2, 3, 4, 6, 6, 4))
            val state = initialState().copy(runningTotal = 300)
            val newState = reducer(noScoreRoller).reduce(state, GameEvent.RollDice)
            val phase = newState.turnPhase as TurnPhase.Farkled
            assertEquals(300, phase.lostPoints)
            assertEquals(0, newState.runningTotal)
        }

        @Test
        fun `saves state to history before rolling`() {
            val state = initialState()
            val newState = reducer().reduce(state, GameEvent.RollDice)
            assertEquals(1, newState.stateHistory.size)
        }

        @Test
        fun `tracks kept dice when rolling after selecting`() {
            val roller = FakeDiceRoller(listOf(1, 5, 3, 4, 6, 2, 1, 2, 3, 4))
            val state = initialState()
            val afterRoll = reducer(roller).reduce(state, GameEvent.RollDice)
            val afterSelect = reducer(roller).reduce(afterRoll, GameEvent.ToggleDie(0))
            val afterReroll = reducer(roller).reduce(afterSelect, GameEvent.RollDice)
            assertEquals(listOf(1), afterReroll.diceKept)
        }

        @Test
        fun `clears kept dice on hot dice reset`() {
            val roller = FakeDiceRoller(listOf(1, 5, 1, 2, 3, 4, 5, 6))
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 300,
                remainingDiceCount = 2,
                diceKept = listOf(1, 1, 5, 5),
            )
            val newState = reducer(roller).reduce(state, GameEvent.RollDice)
            assertEquals(emptyList<Int>(), newState.diceKept)
        }
    }

    @Nested
    inner class ToggleDie {
        @Test
        fun `selects a die by index`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                )
            )
            val newState = reducer().reduce(state, GameEvent.ToggleDie(0))
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertTrue(0 in phase.selectedIndices)
        }

        @Test
        fun `deselects a previously selected die`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                    selectedIndices = setOf(0),
                )
            )
            val newState = reducer().reduce(state, GameEvent.ToggleDie(0))
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertTrue(0 !in phase.selectedIndices)
        }

        @Test
        fun `computes turnScore for valid selection`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                )
            )
            val newState = reducer().reduce(state, GameEvent.ToggleDie(0))
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertEquals(100, phase.turnScore)
        }

        @Test
        fun `turnScore is zero for invalid selection`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                )
            )
            val newState = reducer().reduce(state, GameEvent.ToggleDie(2))
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertEquals(0, phase.turnScore)
        }
    }

    @Nested
    inner class Bank {
        @Test
        fun `adds running total plus selection to player score`() {
            val state = initialState().copy(
                players = listOf(Player("Alice", isOnBoard = true), Player("Bob")),
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 200,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertEquals(350, newState.players[0].totalScore)
        }

        @Test
        fun `transitions to PassingDevice`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 400,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertIs<TurnPhase.PassingDevice>(newState.turnPhase)
        }

        @Test
        fun `player gets on board when banking at least minimum`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 1, 1, 5, 5, 2),
                    selectedIndices = setOf(0, 1, 2, 3, 4),
                ),
                runningTotal = 100,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertTrue(newState.players[0].isOnBoard)
        }

        @Test
        fun `cannot bank below minimum when not on board`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                    selectedIndices = setOf(0),
                ),
                runningTotal = 0,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertIs<TurnPhase.SelectingDice>(newState.turnPhase)
        }
    }

    @Nested
    inner class Undo {
        @Test
        fun `restores previous state from history`() {
            val previousState = initialState()
            val currentState = previousState.copy(
                runningTotal = 150,
                stateHistory = listOf(previousState),
            )
            val newState = reducer().reduce(currentState, GameEvent.Undo)
            assertEquals(0, newState.runningTotal)
            assertTrue(newState.stateHistory.isEmpty())
        }

        @Test
        fun `no-op when history is empty`() {
            val state = initialState()
            val newState = reducer().reduce(state, GameEvent.Undo)
            assertEquals(state, newState)
        }
    }

    @Nested
    inner class TurnTransition {
        @Test
        fun `ReadyForTurn advances to next player`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 0,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertEquals(1, newState.currentPlayerIndex)
            assertIs<TurnPhase.WaitingToRoll>(newState.turnPhase)
        }

        @Test
        fun `wraps around to first player`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 1,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertEquals(0, newState.currentPlayerIndex)
        }

        @Test
        fun `resets running total and dice count on new turn`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.PassingDevice,
                runningTotal = 500,
                remainingDiceCount = 3,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertEquals(0, newState.runningTotal)
            assertEquals(6, newState.remainingDiceCount)
        }

        @Test
        fun `AcknowledgeFarkle transitions to PassingDevice`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.Farkled(lostPoints = 300),
            )
            val newState = reducer().reduce(state, GameEvent.AcknowledgeFarkle)
            assertIs<TurnPhase.PassingDevice>(newState.turnPhase)
        }
    }

    @Nested
    inner class HotDice {
        @Test
        fun `continuing after selecting all dice resets to 6 dice with running total`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 300,
                remainingDiceCount = 2,
            )
            val roller = FakeDiceRoller(listOf(1, 2, 3, 4, 5, 6))
            val newState = reducer(roller).reduce(state, GameEvent.RollDice)
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertEquals(6, phase.rollResult.size)
            assertEquals(450, newState.runningTotal)
        }

        @Test
        fun `hot dice disabled - cannot roll with zero remaining dice`() {
            val config = GameConfig.DEFAULT.copy(hotDiceEnabled = false)
            val engine = ScoringEngine(config)
            val roller = FakeDiceRoller(listOf(1, 5))
            val noHotReducer = GameReducer(engine, roller)

            val state = GameUiState(
                players = twoPlayers,
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 300,
                remainingDiceCount = 2,
                config = config,
            )
            val newState = noHotReducer.reduce(state, GameEvent.RollDice)
            assertEquals(state.turnPhase, newState.turnPhase)
        }
    }

    @Nested
    inner class FinalRound {
        @Test
        fun `reaching target score sets finalRoundTriggerIndex and transitions to PassingDevice`() {
            val state = initialState().copy(
                players = listOf(Player("Alice", totalScore = 9800, isOnBoard = true), Player("Bob")),
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 1, 5, 4, 6, 2),
                    selectedIndices = setOf(0, 1, 2),
                ),
                runningTotal = 0,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertIs<TurnPhase.PassingDevice>(newState.turnPhase)
            assertEquals(0, newState.finalRoundTriggerIndex)
            assertEquals(10050, newState.players[0].totalScore)
        }

        @Test
        fun `other players get one turn after final round triggers`() {
            val state = initialState().copy(
                players = listOf(
                    Player("Alice", totalScore = 10050, isOnBoard = true),
                    Player("Bob", totalScore = 5000, isOnBoard = true),
                ),
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 0,
                finalRoundTriggerIndex = 0,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertEquals(1, newState.currentPlayerIndex)
            assertIs<TurnPhase.WaitingToRoll>(newState.turnPhase)
        }

        @Test
        fun `game ends when rotation returns to trigger player`() {
            val state = initialState().copy(
                players = listOf(
                    Player("Alice", totalScore = 10050, isOnBoard = true),
                    Player("Bob", totalScore = 5300, isOnBoard = true),
                ),
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 1,
                finalRoundTriggerIndex = 0,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertIs<TurnPhase.GameOver>(newState.turnPhase)
            assertEquals(0, (newState.turnPhase as TurnPhase.GameOver).winnerIndex)
        }

        @Test
        fun `player who didn't trigger can win with higher score in final round`() {
            val state = initialState().copy(
                players = listOf(
                    Player("Alice", totalScore = 10050, isOnBoard = true),
                    Player("Bob", totalScore = 10200, isOnBoard = true),
                ),
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 1,
                finalRoundTriggerIndex = 0,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertIs<TurnPhase.GameOver>(newState.turnPhase)
            assertEquals(1, (newState.turnPhase as TurnPhase.GameOver).winnerIndex)
        }
    }

    @Nested
    inner class GameEnd {
        @Test
        fun `player score updates when reaching target`() {
            val state = initialState().copy(
                players = listOf(Player("Alice", totalScore = 9800, isOnBoard = true), Player("Bob")),
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 1, 5, 4, 6, 2),
                    selectedIndices = setOf(0, 1, 2),
                ),
                runningTotal = 0,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertEquals(10050, newState.players[0].totalScore)
        }
    }

    @Nested
    inner class Piggybacking {
        @Test
        fun `offers steal when piggybacking enabled and previous player banked`() {
            val config = GameConfig.DEFAULT.copy(piggybackingEnabled = true)
            val state = GameUiState(
                players = twoPlayers,
                currentPlayerIndex = 0,
                turnPhase = TurnPhase.PassingDevice,
                config = config,
                lastBankedAmount = 400,
                lastBankedDiceRemaining = 3,
            )
            val engine = ScoringEngine(config)
            val r = GameReducer(engine, fakeDice)
            val newState = r.reduce(state, GameEvent.ReadyForTurn)
            assertIs<TurnPhase.OfferSteal>(newState.turnPhase)
            val offer = newState.turnPhase as TurnPhase.OfferSteal
            assertEquals(3, offer.remainingDice)
            assertEquals(400, offer.previousTotal)
        }

        @Test
        fun `StealRoll starts with previous total and remaining dice`() {
            val state = GameUiState(
                players = twoPlayers,
                currentPlayerIndex = 1,
                turnPhase = TurnPhase.OfferSteal(remainingDice = 3, previousTotal = 400),
            )
            val newState = reducer().reduce(state, GameEvent.StealRoll(remainingDice = 3, previousTotal = 400))
            assertIs<TurnPhase.WaitingToRoll>(newState.turnPhase)
            assertEquals(400, newState.runningTotal)
            assertEquals(3, newState.remainingDiceCount)
        }

        @Test
        fun `StartFresh ignores previous total`() {
            val state = GameUiState(
                players = twoPlayers,
                currentPlayerIndex = 1,
                turnPhase = TurnPhase.OfferSteal(remainingDice = 3, previousTotal = 400),
            )
            val newState = reducer().reduce(state, GameEvent.StartFresh)
            assertIs<TurnPhase.WaitingToRoll>(newState.turnPhase)
            assertEquals(0, newState.runningTotal)
            assertEquals(6, newState.remainingDiceCount)
        }

        @Test
        fun `hot dice bank offers 6 dice for steal, not 0`() {
            val config = GameConfig.DEFAULT.copy(piggybackingEnabled = true)
            val engine = ScoringEngine(config)
            val state = GameUiState(
                players = listOf(Player("Alice", totalScore = 500, isOnBoard = true), Player("Bob")),
                currentPlayerIndex = 0,
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 300,
                remainingDiceCount = 2,
                config = config,
            )
            val r = GameReducer(engine, fakeDice)
            val newState = r.reduce(state, GameEvent.Bank)
            assertEquals(6, newState.lastBankedDiceRemaining)
        }

        @Test
        fun `farkle clears lastBankedAmount so stale steal is not offered`() {
            val config = GameConfig.DEFAULT.copy(piggybackingEnabled = true)
            val state = GameUiState(
                players = twoPlayers,
                currentPlayerIndex = 0,
                turnPhase = TurnPhase.Farkled(lostPoints = 200),
                config = config,
                lastBankedAmount = 400,
                lastBankedDiceRemaining = 3,
            )
            val engine = ScoringEngine(config)
            val r = GameReducer(engine, fakeDice)
            val afterFarkle = r.reduce(state, GameEvent.AcknowledgeFarkle)
            assertEquals(0, afterFarkle.lastBankedAmount)
            val afterReady = r.reduce(afterFarkle, GameEvent.ReadyForTurn)
            assertIs<TurnPhase.WaitingToRoll>(afterReady.turnPhase)
        }
    }
}
