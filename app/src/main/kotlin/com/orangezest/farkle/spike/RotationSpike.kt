package com.orangezest.farkle.spike

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@Composable
fun RotationSpike() {
    var tapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("Tap count: $tapCount")

        Box(modifier = Modifier.rotate(180f)) {
            Button(
                onClick = { tapCount++ },
                modifier = Modifier.size(width = 200.dp, height = 56.dp),
            ) {
                Text("Rotated Button")
            }
        }

        Button(
            onClick = { tapCount++ },
            modifier = Modifier.size(width = 200.dp, height = 56.dp),
        ) {
            Text("Normal Button")
        }
    }
}
