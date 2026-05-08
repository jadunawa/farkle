package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.TurnPhase

@Composable
fun PassDeviceScreen(
    nextPlayerName: String,
    onReady: () -> Unit,
    offerSteal: TurnPhase.OfferSteal? = null,
    onSteal: () -> Unit = {},
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
                    .height(56.dp)
                    .padding(bottom = 12.dp),
            ) {
                Text("Steal the roll (${offerSteal.previousTotal} pts, ${offerSteal.remainingDice} dice)")
            }

            Button(
                onClick = onReady,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
            ) {
                Text("Start Fresh")
            }
        } else {
            Button(
                onClick = onReady,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
            ) {
                Text("Ready")
            }
        }
    }
}
