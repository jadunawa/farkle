package com.orangezest.farkle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.orangezest.farkle.ui.components.UpdateDialog
import com.orangezest.farkle.ui.navigation.FarkleNavGraph
import com.orangezest.farkle.ui.theme.FarkleTheme
import com.orangezest.farkle.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            FarkleTheme {
                UpdateDialog()
                val navController = rememberNavController()
                val gameViewModel: GameViewModel = hiltViewModel()
                val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()
                val hasSavedGame by gameViewModel.hasSavedGame.collectAsStateWithLifecycle()

                FarkleNavGraph(
                    navController = navController,
                    onStartGame = { names -> gameViewModel.startGame(names) },
                    gameState = gameState,
                    scoringEngine = gameViewModel.scoringEngine,
                    onEvent = gameViewModel::onEvent,
                    hasSavedGame = hasSavedGame,
                    onResumeGame = { gameViewModel.resumeGame() },
                    windowWidthClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }
}
