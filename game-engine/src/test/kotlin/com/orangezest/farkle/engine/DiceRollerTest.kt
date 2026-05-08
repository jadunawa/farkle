package com.orangezest.farkle.engine

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceRollerTest {

    @Test
    fun `roll returns correct number of dice`() {
        val roller = SecureRandomDiceRoller()
        for (count in 1..6) {
            assertEquals(count, roller.roll(count).size)
        }
    }

    @Test
    fun `all dice values are between 1 and 6`() {
        val roller = SecureRandomDiceRoller()
        val results = (1..1000).flatMap { roller.roll(6) }
        assertTrue(results.all { it in 1..6 })
    }

    @Tag("statistical")
    @Test
    fun `distribution is roughly uniform over many rolls`() {
        val roller = SecureRandomDiceRoller()
        val results = (1..6000).map { roller.roll(1).first() }
        val counts = results.groupingBy { it }.eachCount()

        assertAll(
            (1..6).map { face ->
                { assertTrue(counts[face]!! in 700..1300, "Face $face count ${counts[face]} outside expected range") }
            }
        )
    }

    @Test
    fun `fake dice roller returns predetermined values`() {
        val fake = FakeDiceRoller(listOf(1, 2, 3, 4, 5, 6))
        assertEquals(listOf(1, 2, 3), fake.roll(3))
        assertEquals(listOf(4, 5, 6), fake.roll(3))
    }

    @Test
    fun `fake dice roller wraps around when exhausted`() {
        val fake = FakeDiceRoller(listOf(1, 1, 1))
        fake.roll(3)
        assertEquals(listOf(1, 1, 1), fake.roll(3))
    }
}
