package com.orangezest.farkle.viewmodel

import app.cash.turbine.test
import com.orangezest.farkle.data.FakeGameStateDataStore
import com.orangezest.farkle.data.GameStateRepository
import com.orangezest.farkle.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelSaveResumeTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hasSavedGame emits true after bank triggers save`() = runTest {
        val repo = GameStateRepository(FakeGameStateDataStore())
        val roller = FakeDiceRoller(listOf(1, 1, 1, 1, 5, 2))
        val engine = ScoringEngine(GameConfig.DEFAULT)
        val reducer = GameReducer(engine, roller)
        val vm = GameViewModel(reducer, repo, engine)

        vm.startGame(listOf("Alice", "Bob"))
        vm.onEvent(GameEvent.RollDice)
        vm.onEvent(GameEvent.ToggleDie(0))
        vm.onEvent(GameEvent.ToggleDie(1))
        vm.onEvent(GameEvent.ToggleDie(2))
        vm.onEvent(GameEvent.ToggleDie(3))
        vm.onEvent(GameEvent.Bank)

        assertTrue(vm.hasSavedGame.first())
    }

    @Test
    fun `resumeGame loads saved state`() = runTest {
        val repo = GameStateRepository(FakeGameStateDataStore())
        val roller = FakeDiceRoller(listOf(1, 1, 1, 5, 5, 2))
        val engine = ScoringEngine(GameConfig.DEFAULT)
        val reducer = GameReducer(engine, roller)
        val vm = GameViewModel(reducer, repo, engine)

        val savedState = GameUiState(
            players = listOf(Player("Alice", totalScore = 500, isOnBoard = true), Player("Bob")),
            currentPlayerIndex = 1,
        )
        repo.saveGame(savedState)

        vm.resumeGame()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(500, state.players[0].totalScore)
            assertEquals(1, state.currentPlayerIndex)
        }
    }
}
