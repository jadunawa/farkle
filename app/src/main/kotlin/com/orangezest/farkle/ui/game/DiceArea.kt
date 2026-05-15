package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.orangezest.farkle.engine.ScoringEngine
import com.orangezest.farkle.engine.TurnPhase
import com.orangezest.farkle.ui.components.Die
import com.orangezest.farkle.ui.components.DieState

@Composable
fun DiceArea(
    phase: TurnPhase.SelectingDice,
    scoringEngine: ScoringEngine,
    onToggleDie: (Int) -> Unit,
    diceKept: List<Int> = emptyList(),
    diceStayInPlace: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scoringOptions = scoringEngine.findScoringOptions(phase.rollResult)
    val scorableFaces = scoringOptions.flatMap { it.diceUsed }.toSet()

    if (diceStayInPlace) {
        DiceGridStayInPlace(
            rollResult = phase.rollResult,
            selectedIndices = phase.selectedIndices,
            scorableFaces = scorableFaces,
            diceKept = diceKept,
            onToggleDie = onToggleDie,
            modifier = modifier,
        )
    } else {
        DiceGridSeparateKept(
            rollResult = phase.rollResult,
            selectedIndices = phase.selectedIndices,
            scorableFaces = scorableFaces,
            diceKept = diceKept,
            onToggleDie = onToggleDie,
            modifier = modifier,
        )
    }
}

@Composable
private fun DiceGridStayInPlace(
    rollResult: List<Int>,
    selectedIndices: Set<Int>,
    scorableFaces: Set<Int>,
    diceKept: List<Int>,
    onToggleDie: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalSlots = diceKept.size + rollResult.size
    val rows = (0 until totalSlots).chunked(2)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = 12.dp
        val dieSize = min((maxWidth - spacing) / 2, 160.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            rows.forEach { slotIndices ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    slotIndices.forEach { slotIndex ->
                        if (slotIndex < diceKept.size) {
                            Die(
                                value = diceKept[slotIndex],
                                state = DieState.LOCKED,
                                onClick = {},
                                modifier = Modifier.size(dieSize),
                            )
                        } else {
                            val rollIndex = slotIndex - diceKept.size
                            val value = rollResult[rollIndex]
                            val isSelected = rollIndex in selectedIndices
                            val canScore = value in scorableFaces

                            Die(
                                value = value,
                                state = if (isSelected) DieState.SELECTED else DieState.DEFAULT,
                                onClick = { if (canScore || isSelected) onToggleDie(rollIndex) },
                                modifier = Modifier.size(dieSize),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiceGridSeparateKept(
    rollResult: List<Int>,
    selectedIndices: Set<Int>,
    scorableFaces: Set<Int>,
    diceKept: List<Int>,
    onToggleDie: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = 12.dp
        val dieSize = min((maxWidth - spacing) / 2, 160.dp)
        val keptDieSize = min(dieSize * 0.67f, 64.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            if (diceKept.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    diceKept.forEach { value ->
                        Die(
                            value = value,
                            state = DieState.LOCKED,
                            onClick = {},
                            modifier = Modifier.size(keptDieSize),
                        )
                    }
                }
            }

            val rows = rollResult.chunked(2)
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEachIndexed { colIndex, value ->
                        val index = rowIndex * 2 + colIndex
                        val isSelected = index in selectedIndices
                        val canScore = value in scorableFaces

                        Die(
                            value = value,
                            state = if (isSelected) DieState.SELECTED else DieState.DEFAULT,
                            onClick = { if (canScore || isSelected) onToggleDie(index) },
                            modifier = Modifier.size(dieSize),
                        )
                    }
                }
            }
        }
    }
}
