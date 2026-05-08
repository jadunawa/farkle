package com.orangezest.farkle.engine

sealed interface GameEvent {
    data object RollDice : GameEvent
    data class ToggleDie(val index: Int) : GameEvent
    data object Bank : GameEvent
    data object Undo : GameEvent
    data object AcknowledgeFarkle : GameEvent
    data object ReadyForTurn : GameEvent
    data object StartFresh : GameEvent
    data class StealRoll(val remainingDice: Int, val previousTotal: Int) : GameEvent
}
