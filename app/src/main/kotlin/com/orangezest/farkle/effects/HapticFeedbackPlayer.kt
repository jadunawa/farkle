package com.orangezest.farkle.effects

import android.view.HapticFeedbackConstants
import android.view.View
import com.orangezest.farkle.engine.TurnPhase

class HapticFeedbackPlayer(
    private val hapticEnabled: () -> Boolean,
) {

    fun onPhaseChange(previousPhase: TurnPhase?, newPhase: TurnPhase, view: View) {
        when {
            newPhase is TurnPhase.SelectingDice && previousPhase is TurnPhase.WaitingToRoll -> {
                performHaptic(view, HapticFeedbackConstants.KEYBOARD_TAP)
            }
            newPhase is TurnPhase.Farkled -> {
                performHaptic(view, HapticFeedbackConstants.LONG_PRESS)
            }
            newPhase is TurnPhase.PassingDevice && previousPhase is TurnPhase.SelectingDice -> {
                performHaptic(view, HapticFeedbackConstants.CONFIRM)
            }
        }
    }

    fun onDieSelected(view: View) {
        performHaptic(view, HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun performHaptic(view: View, feedbackConstant: Int) {
        if (!hapticEnabled()) return
        view.performHapticFeedback(feedbackConstant)
    }
}
