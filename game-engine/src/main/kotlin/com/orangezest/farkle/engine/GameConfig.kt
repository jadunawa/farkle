package com.orangezest.farkle.engine

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val targetScore: Int = 10_000,
    val minimumToBoard: Int = 500,
    val hotDiceEnabled: Boolean = true,
    val piggybackingEnabled: Boolean = false,
    val single1Points: Int = 100,
    val single5Points: Int = 50,
    val three1sPoints: Int = 300,
    val three2sPoints: Int = 200,
    val three3sPoints: Int = 300,
    val three4sPoints: Int = 400,
    val three5sPoints: Int = 500,
    val three6sPoints: Int = 600,
    val fourOfAKindPoints: Int = 1_000,
    val fiveOfAKindPoints: Int = 2_000,
    val sixOfAKindPoints: Int = 3_000,
    val straightPoints: Int = 1_500,
    val threePairsPoints: Int = 1_500,
    val fourPlusAPairPoints: Int = 1_500,
    val twoTripletsPoints: Int = 2_500,
) {
    companion object {
        val DEFAULT = GameConfig()
    }
}
