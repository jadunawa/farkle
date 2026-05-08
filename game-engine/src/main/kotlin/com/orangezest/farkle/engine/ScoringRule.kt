package com.orangezest.farkle.engine

data class ScoringResult(
    val points: Int,
    val diceUsed: List<Int>,
    val name: String,
)

sealed interface ScoringRule {
    val name: String
    fun evaluate(dice: List<Int>): ScoringResult?
}
