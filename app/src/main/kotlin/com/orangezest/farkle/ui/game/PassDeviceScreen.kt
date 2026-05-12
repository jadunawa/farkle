package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.TurnPhase

@Composable
fun PassDeviceScreen(
    nextPlayerName: String,
    onReady: () -> Unit,
    offerSteal: TurnPhase.OfferSteal? = null,
    onSteal: () -> Unit = {},
    buttonHeight: Dp = 72.dp,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$nextPlayerName's Turn",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (offerSteal != null) {
            OutlinedButton(
                onClick = onSteal,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(buttonHeight),
            ) {
                Text(
                    "Steal the roll (${offerSteal.previousTotal} pts, ${offerSteal.remainingDice} dice)",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onReady,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(buttonHeight),
            ) {
                Text("Start Fresh", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = onReady,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(buttonHeight),
            ) {
                Text("Ready", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
