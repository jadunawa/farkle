package com.orangezest.farkle.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoringEngineTest {

    private val engine = ScoringEngine(GameConfig.DEFAULT)

    @Test
    fun `finds single 1 as scoring option`() {
        val options = engine.findScoringOptions(listOf(1, 2, 3, 4, 6, 6))
        assertTrue(options.any { it.name == "Single 1" && it.points == 100 })
    }

    @Test
    fun `finds single 5 as scoring option`() {
        val options = engine.findScoringOptions(listOf(5, 2, 3, 4, 6, 6))
        assertTrue(options.any { it.name == "Single 5" && it.points == 50 })
    }

    @Test
    fun `finds three of a kind`() {
        val options = engine.findScoringOptions(listOf(3, 3, 3, 2, 4, 6))
        assertTrue(options.any { it.name == "3 of a Kind" && it.points == 300 })
    }

    @Test
    fun `finds straight`() {
        val options = engine.findScoringOptions(listOf(1, 2, 3, 4, 5, 6))
        assertTrue(options.any { it.name == "Straight" && it.points == 1500 })
    }

    @Test
    fun `empty scoring options for non-scoring roll`() {
        val options = engine.findScoringOptions(listOf(2, 3, 4, 6, 6, 4))
        assertTrue(options.isEmpty())
    }

    @Test
    fun `isFarkle returns true for non-scoring roll`() {
        assertTrue(engine.isFarkle(listOf(2, 3, 4, 6, 6, 4)))
    }

    @Test
    fun `isFarkle returns false when scoring options exist`() {
        assertEquals(false, engine.isFarkle(listOf(1, 2, 3, 4, 6, 6)))
    }

    @Test
    fun `validates correct dice selection`() {
        val roll = listOf(1, 1, 3, 4, 5, 6)
        val selection = listOf(1, 1)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(200, result)
    }

    @Test
    fun `validates single die selection from multiple scoring dice`() {
        val roll = listOf(1, 1, 5, 4, 3, 6)
        val selection = listOf(5)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(50, result)
    }

    @Test
    fun `rejects invalid dice selection`() {
        val roll = listOf(2, 3, 4, 6, 6, 4)
        val selection = listOf(2)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(0, result)
    }

    @Test
    fun `scores three of a kind selection`() {
        val roll = listOf(4, 4, 4, 2, 3, 6)
        val selection = listOf(4, 4, 4)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(400, result)
    }

    @Test
    fun `scores mixed selection of singles and three of a kind`() {
        val roll = listOf(1, 4, 4, 4, 5, 6)
        val selection = listOf(1, 4, 4, 4, 5)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(550, result)
    }

    @Test
    fun `uses custom config point values`() {
        val custom = GameConfig.DEFAULT.copy(single1Points = 200)
        val customEngine = ScoringEngine(custom)
        val options = customEngine.findScoringOptions(listOf(1, 2, 3, 4, 6, 6))
        val single1 = options.first { it.name == "Single 1" }
        assertEquals(200, single1.points)
    }

    @Test
    fun `four-plus-a-pair takes priority over four-of-a-kind plus singles`() {
        val roll = listOf(1, 1, 1, 1, 5, 5)
        val score = engine.scoreSelection(roll, roll)
        assertEquals(1500, score)
    }

    @Test
    fun `two-triplets takes priority over individual three-of-a-kinds`() {
        val roll = listOf(1, 1, 1, 5, 5, 5)
        val score = engine.scoreSelection(roll, roll)
        assertEquals(2500, score)
    }
}
