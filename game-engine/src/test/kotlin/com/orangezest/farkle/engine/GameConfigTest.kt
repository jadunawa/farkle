package com.orangezest.farkle.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GameConfigTest {

    @Test
    fun `default config matches official rules`() {
        val config = GameConfig.DEFAULT

        assertEquals(100, config.single1Points)
        assertEquals(50, config.single5Points)
        assertEquals(300, config.three1sPoints)
        assertEquals(200, config.three2sPoints)
        assertEquals(300, config.three3sPoints)
        assertEquals(400, config.three4sPoints)
        assertEquals(500, config.three5sPoints)
        assertEquals(600, config.three6sPoints)
        assertEquals(1000, config.fourOfAKindPoints)
        assertEquals(2000, config.fiveOfAKindPoints)
        assertEquals(3000, config.sixOfAKindPoints)
        assertEquals(1500, config.straightPoints)
        assertEquals(1500, config.threePairsPoints)
        assertEquals(1500, config.fourPlusAPairPoints)
        assertEquals(2500, config.twoTripletsPoints)
    }

    @Test
    fun `default target score is 10000`() {
        assertEquals(10_000, GameConfig.DEFAULT.targetScore)
    }

    @Test
    fun `default minimum to get on board is 500`() {
        assertEquals(500, GameConfig.DEFAULT.minimumToBoard)
    }

    @Test
    fun `default hot dice is on`() {
        assertEquals(true, GameConfig.DEFAULT.hotDiceEnabled)
    }

    @Test
    fun `default piggybacking is off`() {
        assertEquals(false, GameConfig.DEFAULT.piggybackingEnabled)
    }

    @Test
    fun `custom config overrides specific values`() {
        val custom = GameConfig.DEFAULT.copy(
            targetScore = 5000,
            single1Points = 200,
            piggybackingEnabled = true
        )
        assertEquals(5000, custom.targetScore)
        assertEquals(200, custom.single1Points)
        assertEquals(true, custom.piggybackingEnabled)
        assertEquals(50, custom.single5Points)
    }
}
