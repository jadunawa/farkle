package com.orangezest.farkle.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class DieState {
    DEFAULT,
    SELECTED,
    DISABLED,
    LOCKED,
}

@Composable
fun Die(
    value: Int,
    state: DieState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            DieState.DEFAULT -> MaterialTheme.colorScheme.surface
            DieState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
            DieState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
            DieState.LOCKED -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "die_bg",
    )

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            DieState.SELECTED -> MaterialTheme.colorScheme.primary
            DieState.LOCKED -> MaterialTheme.colorScheme.outlineVariant
            else -> MaterialTheme.colorScheme.outline
        },
        label = "die_border",
    )

    val pipColor = when (state) {
        DieState.SELECTED -> MaterialTheme.colorScheme.onPrimaryContainer
        DieState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
        DieState.LOCKED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    val alpha = when (state) {
        DieState.DISABLED -> 0.4f
        DieState.LOCKED -> 0.6f
        else -> 1f
    }

    Box(
        modifier = modifier
            .size(72.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .then(
                if (state == DieState.DEFAULT || state == DieState.SELECTED) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        DieFace(value = value, pipColor = pipColor)
    }
}

@Composable
private fun DieFace(value: Int, pipColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize(0.7f)) {
        val pipRadius = size.minDimension * 0.09f
        val positions = pipPositions(value, size.width, size.height)
        positions.forEach { offset ->
            drawCircle(color = pipColor, radius = pipRadius, center = offset)
        }
    }
}

private fun pipPositions(value: Int, w: Float, h: Float): List<Offset> {
    val cx = w / 2f
    val cy = h / 2f
    val left = w * 0.25f
    val right = w * 0.75f
    val top = h * 0.25f
    val bottom = h * 0.75f

    return when (value) {
        1 -> listOf(Offset(cx, cy))
        2 -> listOf(Offset(left, top), Offset(right, bottom))
        3 -> listOf(Offset(left, top), Offset(cx, cy), Offset(right, bottom))
        4 -> listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom))
        5 -> listOf(Offset(left, top), Offset(right, top), Offset(cx, cy), Offset(left, bottom), Offset(right, bottom))
        6 -> listOf(
            Offset(left, top), Offset(right, top),
            Offset(left, cy), Offset(right, cy),
            Offset(left, bottom), Offset(right, bottom),
        )
        else -> emptyList()
    }
}
