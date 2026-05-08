package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.ScoringEngine
import com.orangezest.farkle.engine.TurnPhase
import com.orangezest.farkle.ui.components.Die
import com.orangezest.farkle.ui.components.DieState

@Composable
fun DiceArea(
    phase: TurnPhase.SelectingDice,
    scoringEngine: ScoringEngine,
    onToggleDie: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoringOptions = scoringEngine.findScoringOptions(phase.rollResult)
    val scorableFaces = scoringOptions.flatMap { it.diceUsed }.toSet()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val rows = phase.rollResult.chunked(3)
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEachIndexed { colIndex, value ->
                    val index = rowIndex * 3 + colIndex
                    val isSelected = index in phase.selectedIndices
                    val canScore = value in scorableFaces

                    val state = when {
                        isSelected -> DieState.SELECTED
                        !canScore -> DieState.DISABLED
                        else -> DieState.DEFAULT
                    }

                    Die(
                        value = value,
                        state = state,
                        onClick = { onToggleDie(index) },
                    )
                }
            }
        }
    }
}
