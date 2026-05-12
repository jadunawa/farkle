package com.orangezest.farkle.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onStartGame: (List<String>) -> Unit,
    onOpenSettings: () -> Unit,
    hasSavedGame: Boolean = false,
    onResumeGame: () -> Unit = {},
) {
    var playerCount by remember { mutableIntStateOf(2) }
    var playerNames by remember { mutableStateOf(List(4) { "" }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Farkle") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            val buttonHeight = (maxHeight * 0.09f).coerceIn(48.dp, 96.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Players", style = MaterialTheme.typography.titleLarge)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (2..4).forEach { count ->
                        FilterChip(
                            selected = playerCount == count,
                            onClick = { playerCount = count },
                            label = { Text("$count") },
                        )
                    }
                }

                (0 until playerCount).forEach { index ->
                    OutlinedTextField(
                        value = playerNames[index],
                        onValueChange = { name ->
                            playerNames = playerNames.toMutableList().apply { set(index, name) }
                        },
                        label = { Text("Player ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (hasSavedGame) {
                    OutlinedButton(
                        onClick = onResumeGame,
                        modifier = Modifier.fillMaxWidth().height(buttonHeight),
                    ) {
                        Text("Resume Game", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Button(
                    onClick = {
                        val names = (0 until playerCount).map { i ->
                            playerNames[i].ifBlank { "Player ${i + 1}" }
                        }
                        onStartGame(names)
                    },
                    modifier = Modifier.fillMaxWidth().height(buttonHeight),
                ) {
                    Text("Start Game", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
