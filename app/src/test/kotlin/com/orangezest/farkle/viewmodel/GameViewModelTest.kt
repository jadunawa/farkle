package com.orangezest.farkle.viewmodel

import app.cash.turbine.test
import com.orangezest.farkle.data.GameStateRepository
import com.orangezest.farkle.engine.*
import com.orangezest.farkle.data.FakeGameStateDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        diceRoller: DiceRoller = FakeDiceRoller(listOf(1, 5, 3, 4, 6, 2)),
        config: GameConfig = GameConfig.DEFAULT,
    ): GameViewModel {
        val engine = ScoringEngine(config)
        val reducer = GameReducer(engine, diceRoller)
        val repo = GameStateRepository(FakeGameStateDataStore())
        return GameViewModel(reducer, repo, engine)
    }

    @Test
    fun `initial state is WaitingToRoll after starting game`() = runTest {
        val vm = createViewModel()
        vm.startGame(listOf("Alice", "Bob"))

        vm.uiState.test {
            val state = awaitItem()
            assertIs<TurnPhase.WaitingToRoll>(state.turnPhase)
            assertEquals(2, state.players.size)
            assertEquals("Alice", state.players[0].name)
        }
    }

    @Test
    fun `onEvent RollDice transitions to SelectingDice`() = runTest {
        val vm = createViewModel()
        vm.startGame(listOf("Alice", "Bob"))

        vm.uiState.test {
            awaitItem()
            vm.onEvent(GameEvent.RollDice)
            val state = awaitItem()
            assertIs<TurnPhase.SelectingDice>(state.turnPhase)
        }
    }

    @Test
    fun `full turn sequence - roll select bank`() = runTest {
        val roller = FakeDiceRoller(listOf(1, 1, 1, 1, 5, 2))
        val vm = createViewModel(diceRoller = roller)
        vm.startGame(listOf("Alice", "Bob"))

        vm.uiState.test {
            awaitItem()
            vm.onEvent(GameEvent.RollDice)
            awaitItem()

            vm.onEvent(GameEvent.ToggleDie(0))
            vm.onEvent(GameEvent.ToggleDie(1))
            vm.onEvent(GameEvent.ToggleDie(2))
            vm.onEvent(GameEvent.ToggleDie(3))
            cancelAndIgnoreRemainingEvents()
        }

        vm.onEvent(GameEvent.Bank)
        vm.uiState.test {
            val state = awaitItem()
            assertIs<TurnPhase.PassingDevice>(state.turnPhase)
            assertEquals(1000, state.players[0].totalScore)
        }
    }

    @Test
    fun `startGame with empty list does not crash`() = runTest {
        val vm = createViewModel()
        vm.startGame(emptyList())
        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.players.isEmpty())
        }
    }

    @Test
    fun `starting new game resets previous game state`() = runTest {
        val vm = createViewModel()
        vm.startGame(listOf("Alice", "Bob"))
        vm.onEvent(GameEvent.RollDice)

        vm.startGame(listOf("Charlie", "Dave"))
        vm.uiState.test {
            val state = awaitItem()
            assertIs<TurnPhase.WaitingToRoll>(state.turnPhase)
            assertEquals("Charlie", state.players[0].name)
            assertEquals(0, state.players[0].totalScore)
        }
    }
}
