package com.orangezest.farkle.viewmodel

import com.orangezest.farkle.data.InMemorySettingsStore
import com.orangezest.farkle.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `updating target score emits new config`() = runTest {
        val store = InMemorySettingsStore()
        val repo = SettingsRepository(store)
        val vm = SettingsViewModel(repo)
        vm.updateTargetScore(5000)
        assertEquals(5000, vm.gameConfig.first().targetScore)
    }

    @Test
    fun `updating piggybacking emits new config`() = runTest {
        val store = InMemorySettingsStore()
        val repo = SettingsRepository(store)
        val vm = SettingsViewModel(repo)
        vm.updatePiggybacking(true)
        assertEquals(true, vm.gameConfig.first().piggybackingEnabled)
    }

    @Test
    fun `updating haptic enabled emits new value`() = runTest {
        val store = InMemorySettingsStore()
        val repo = SettingsRepository(store)
        val vm = SettingsViewModel(repo)
        vm.updateHapticEnabled(false)
        assertEquals(false, vm.hapticEnabled.first())
    }

    @Test
    fun `dice stay in place defaults to true`() = runTest {
        val store = InMemorySettingsStore()
        val repo = SettingsRepository(store)
        val vm = SettingsViewModel(repo)
        assertEquals(true, vm.diceStayInPlace.first())
    }

    @Test
    fun `updating dice stay in place emits new value`() = runTest {
        val store = InMemorySettingsStore()
        val repo = SettingsRepository(store)
        val vm = SettingsViewModel(repo)
        vm.updateDiceStayInPlace(false)
        assertEquals(false, vm.diceStayInPlace.first())
    }
}
