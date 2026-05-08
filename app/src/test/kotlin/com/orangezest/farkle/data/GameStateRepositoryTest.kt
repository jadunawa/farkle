package com.orangezest.farkle.data

import com.orangezest.farkle.engine.GameUiState
import com.orangezest.farkle.engine.Player
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameStateRepositoryTest {

    @Test
    fun `empty datastore returns null`() = runTest {
        val repo = GameStateRepository(FakeGameStateDataStore())
        assertNull(repo.savedGame.first())
    }

    @Test
    fun `save and read round-trips correctly`() = runTest {
        val repo = GameStateRepository(FakeGameStateDataStore())
        val state = GameUiState(players = listOf(Player("Alice")))
        repo.saveGame(state)
        assertEquals(state, repo.savedGame.first())
    }

    @Test
    fun `clearSavedGame returns to null`() = runTest {
        val repo = GameStateRepository(FakeGameStateDataStore())
        val state = GameUiState(players = listOf(Player("Alice")))
        repo.saveGame(state)
        repo.clearSavedGame()
        assertNull(repo.savedGame.first())
    }
}
