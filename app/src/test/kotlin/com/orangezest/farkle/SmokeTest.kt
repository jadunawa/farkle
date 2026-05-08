package com.orangezest.farkle

import com.orangezest.farkle.engine.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SmokeTest {

    @Test
    fun `complete two-player game with final round`() {
        val targetConfig = GameConfig.DEFAULT.copy(targetScore = 1000)
        val targetEngine = ScoringEngine(targetConfig)

        val diceSequence = listOf(
            1, 1, 1, 1, 5, 2,
            5, 5, 5, 5, 5, 5,
        )
        val roller = FakeDiceRoller(diceSequence)
        val reducer = GameReducer(targetEngine, roller)

        var state = GameUiState(
            players = listOf(Player("Alice"), Player("Bob")),
            config = targetConfig,
        )

        state = reducer.reduce(state, GameEvent.RollDice)
        assertIs<TurnPhase.SelectingDice>(state.turnPhase)
        val alicePhase = state.turnPhase as TurnPhase.SelectingDice
        assertEquals(listOf(1, 1, 1, 1, 5, 2), alicePhase.rollResult)

        state = reducer.reduce(state, GameEvent.ToggleDie(0))
        state = reducer.reduce(state, GameEvent.ToggleDie(1))
        state = reducer.reduce(state, GameEvent.ToggleDie(2))
        state = reducer.reduce(state, GameEvent.ToggleDie(3))

        state = reducer.reduce(state, GameEvent.Bank)
        assertIs<TurnPhase.PassingDevice>(state.turnPhase)
        assertEquals(1000, state.players[0].totalScore)
        assertTrue(state.players[0].isOnBoard)
        assertEquals(0, state.finalRoundTriggerIndex)

        state = reducer.reduce(state, GameEvent.ReadyForTurn)
        assertEquals(1, state.currentPlayerIndex)
        assertIs<TurnPhase.WaitingToRoll>(state.turnPhase)

        state = reducer.reduce(state, GameEvent.RollDice)
        assertIs<TurnPhase.SelectingDice>(state.turnPhase)
        val bobPhase = state.turnPhase as TurnPhase.SelectingDice
        assertEquals(listOf(5, 5, 5, 5, 5, 5), bobPhase.rollResult)

        state = reducer.reduce(state, GameEvent.ToggleDie(0))
        state = reducer.reduce(state, GameEvent.ToggleDie(1))
        state = reducer.reduce(state, GameEvent.ToggleDie(2))
        state = reducer.reduce(state, GameEvent.ToggleDie(3))
        state = reducer.reduce(state, GameEvent.ToggleDie(4))
        state = reducer.reduce(state, GameEvent.ToggleDie(5))

        state = reducer.reduce(state, GameEvent.Bank)
        assertIs<TurnPhase.PassingDevice>(state.turnPhase)
        assertEquals(3000, state.players[1].totalScore)

        state = reducer.reduce(state, GameEvent.ReadyForTurn)
        assertIs<TurnPhase.GameOver>(state.turnPhase)

        val gameOver = state.turnPhase as TurnPhase.GameOver
        assertEquals(1, gameOver.winnerIndex)
    }

    @Test
    fun `game engine scoring is consistent with rules`() {
        val engine = ScoringEngine(GameConfig.DEFAULT)

        assertEquals(true, engine.isFarkle(listOf(2, 3, 4, 6, 6, 4)))
        assertEquals(false, engine.isFarkle(listOf(1, 2, 3, 4, 6, 6)))
        assertEquals(100, engine.scoreSelection(listOf(1, 2, 3, 4, 6, 6), listOf(1)))
        assertEquals(1500, engine.scoreSelection(listOf(1, 2, 3, 4, 5, 6), listOf(1, 2, 3, 4, 5, 6)))
    }
}
