package com.orangezest.farkle.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DieState {
    DEFAULT,
    SELECTED,
    DISABLED,
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
        },
        label = "die_bg",
    )

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            DieState.SELECTED -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        label = "die_border",
    )

    val alpha = if (state == DieState.DISABLED) 0.4f else 1f

    Box(
        modifier = modifier
            .size(56.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .then(
                if (state != DieState.DISABLED) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = when (state) {
                DieState.SELECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                DieState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
