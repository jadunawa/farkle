package com.orangezest.farkle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.GameConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: GameConfig,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    themeMode: String,
    onUpdateTargetScore: (Int) -> Unit,
    onUpdateMinimumToBoard: (Int) -> Unit,
    onUpdateHotDice: (Boolean) -> Unit,
    onUpdatePiggybacking: (Boolean) -> Unit,
    onUpdateSoundEnabled: (Boolean) -> Unit,
    onUpdateHapticEnabled: (Boolean) -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Game Rules", style = MaterialTheme.typography.titleMedium)

            NumberField("Target Score", config.targetScore, onUpdateTargetScore)
            NumberField("Minimum to Get on Board", config.minimumToBoard, onUpdateMinimumToBoard)

            SwitchRow("Hot Dice", config.hotDiceEnabled, onUpdateHotDice)
            SwitchRow("Piggybacking", config.piggybackingEnabled, onUpdatePiggybacking)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Feedback", style = MaterialTheme.typography.titleMedium)

            SwitchRow("Haptic Feedback", hapticEnabled, onUpdateHapticEnabled)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium)

            ThemeSelector(themeMode, onUpdateThemeMode)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumberField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            newText.toIntOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeSelector(current: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
            FilterChip(
                selected = current == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}
