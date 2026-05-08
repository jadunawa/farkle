package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ActionBar(
    canRoll: Boolean,
    canBank: Boolean,
    bankAmount: Int,
    onRoll: () -> Unit,
    onBank: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (canUndo) {
            OutlinedButton(
                onClick = onUndo,
                modifier = Modifier.height(56.dp),
            ) {
                Text("Undo")
            }
        }

        Button(
            onClick = onRoll,
            enabled = canRoll,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F),
            ),
            modifier = Modifier.weight(1f).height(56.dp),
        ) {
            Text("Roll", color = Color.White)
        }

        Button(
            onClick = onBank,
            enabled = canBank,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF388E3C),
            ),
            modifier = Modifier.weight(1f).height(56.dp),
        ) {
            Text("Bank (+$bankAmount)", color = Color.White)
        }
    }
}
