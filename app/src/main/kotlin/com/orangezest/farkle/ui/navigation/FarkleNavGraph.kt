package com.orangezest.farkle.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.orangezest.farkle.engine.GameEvent
import com.orangezest.farkle.engine.GameUiState
import com.orangezest.farkle.engine.ScoringEngine
import com.orangezest.farkle.ui.game.AdaptiveGameScreen
import com.orangezest.farkle.ui.settings.SettingsScreen
import com.orangezest.farkle.ui.setup.SetupScreen
import com.orangezest.farkle.viewmodel.SettingsViewModel

object Routes {
    const val SETUP = "setup"
    const val GAME = "game"
    const val SETTINGS = "settings"
}

@Composable
fun FarkleNavGraph(
    navController: NavHostController,
    onStartGame: (List<String>) -> Unit,
    gameState: GameUiState,
    scoringEngine: ScoringEngine,
    onEvent: (GameEvent) -> Unit,
    hasSavedGame: Boolean = false,
    onResumeGame: () -> Unit = {},
    windowWidthClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
) {
    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            SetupScreen(
                onStartGame = { names ->
                    onStartGame(names)
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                hasSavedGame = hasSavedGame,
                onResumeGame = {
                    onResumeGame()
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.GAME) {
            AdaptiveGameScreen(
                state = gameState,
                scoringEngine = scoringEngine,
                onEvent = onEvent,
                onNewGame = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
                windowWidthClass = windowWidthClass,
            )
        }
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val config by settingsViewModel.gameConfig.collectAsStateWithLifecycle()
            val soundEnabled by settingsViewModel.soundEnabled.collectAsStateWithLifecycle()
            val hapticEnabled by settingsViewModel.hapticEnabled.collectAsStateWithLifecycle()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val diceStayInPlace by settingsViewModel.diceStayInPlace.collectAsStateWithLifecycle()

            SettingsScreen(
                config = config,
                soundEnabled = soundEnabled,
                hapticEnabled = hapticEnabled,
                themeMode = themeMode,
                onUpdateTargetScore = settingsViewModel::updateTargetScore,
                onUpdateMinimumToBoard = settingsViewModel::updateMinimumToBoard,
                onUpdateHotDice = settingsViewModel::updateHotDice,
                onUpdatePiggybacking = settingsViewModel::updatePiggybacking,
                onUpdateSoundEnabled = settingsViewModel::updateSoundEnabled,
                onUpdateHapticEnabled = settingsViewModel::updateHapticEnabled,
                onUpdateThemeMode = settingsViewModel::updateThemeMode,
                diceStayInPlace = diceStayInPlace,
                onUpdateDiceStayInPlace = settingsViewModel::updateDiceStayInPlace,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
