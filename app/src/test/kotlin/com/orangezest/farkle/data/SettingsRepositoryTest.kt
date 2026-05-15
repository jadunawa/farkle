package com.orangezest.farkle.data

import com.orangezest.farkle.engine.GameConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsRepositoryTest {

    @Test
    fun `default settings match GameConfig defaults`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        val config = repo.gameConfig.first()
        assertEquals(GameConfig.DEFAULT, config)
    }

    @Test
    fun `updates target score`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updateTargetScore(5000)
        val config = repo.gameConfig.first()
        assertEquals(5000, config.targetScore)
    }

    @Test
    fun `updates piggybacking`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updatePiggybacking(true)
        val config = repo.gameConfig.first()
        assertEquals(true, config.piggybackingEnabled)
    }

    @Test
    fun `updates sound enabled`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updateSoundEnabled(false)
        assertFalse(repo.soundEnabled.first())
    }

    @Test
    fun `updates haptic enabled`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updateHapticEnabled(false)
        assertFalse(repo.hapticEnabled.first())
    }

    @Test
    fun `dice stay in place defaults to true`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        assertEquals(true, repo.diceStayInPlace.first())
    }

    @Test
    fun `updates dice stay in place`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updateDiceStayInPlace(false)
        assertFalse(repo.diceStayInPlace.first())
    }
}
