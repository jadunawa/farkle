package com.orangezest.farkle.engine

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScoringRulesTest {

    private val config = GameConfig.DEFAULT

    @Nested
    inner class Single1Rule {
        private val rule = SinglesRule(1, config)

        @Test
        fun `scores 100 for a single 1`() {
            val result = rule.evaluate(listOf(1))
            assertEquals(100, result?.points)
            assertEquals(listOf(1), result?.diceUsed)
        }

        @Test
        fun `returns null for no 1s`() {
            assertNull(rule.evaluate(listOf(2, 3, 4, 6)))
        }

        @Test
        fun `scores only one 1 per evaluation`() {
            val result = rule.evaluate(listOf(1, 1, 3, 4))
            assertEquals(100, result?.points)
            assertEquals(listOf(1), result?.diceUsed)
        }
    }

    @Nested
    inner class Single5Rule {
        private val rule = SinglesRule(5, config)

        @Test
        fun `scores 50 for a single 5`() {
            val result = rule.evaluate(listOf(5))
            assertEquals(50, result?.points)
            assertEquals(listOf(5), result?.diceUsed)
        }

        @Test
        fun `returns null for no 5s`() {
            assertNull(rule.evaluate(listOf(1, 2, 3, 4, 6)))
        }
    }

    @Nested
    inner class ThreeOfAKindRule {
        @Test
        fun `three 1s scores 300`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(1, 1, 1, 3, 4, 6))
            assertEquals(300, result?.points)
            assertEquals(listOf(1, 1, 1), result?.diceUsed)
        }

        @Test
        fun `three 2s scores 200`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(2, 2, 2))
            assertEquals(200, result?.points)
            assertEquals(listOf(2, 2, 2), result?.diceUsed)
        }

        @Test
        fun `three 3s scores 300`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(3, 3, 3, 5, 6, 2))
            assertEquals(300, result?.points)
        }

        @Test
        fun `three 4s scores 400`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(4, 4, 4))
            assertEquals(400, result?.points)
        }

        @Test
        fun `three 5s scores 500`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(5, 5, 5, 2, 3))
            assertEquals(500, result?.points)
        }

        @Test
        fun `three 6s scores 600`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(6, 6, 6))
            assertEquals(600, result?.points)
        }

        @Test
        fun `returns null when no three of a kind exists`() {
            val rule = NOfAKindRule(3, config)
            assertNull(rule.evaluate(listOf(1, 2, 3, 4, 5, 6)))
        }

        @Test
        fun `does not match four of a kind`() {
            val rule = NOfAKindRule(3, config)
            assertNull(rule.evaluate(listOf(2, 2, 2, 2, 3, 4)))
        }
    }

    @Nested
    inner class FourOfAKindRule {
        @Test
        fun `four of any number scores 1000`() {
            val rule = NOfAKindRule(4, config)
            val result = rule.evaluate(listOf(3, 3, 3, 3, 5, 6))
            assertEquals(1000, result?.points)
            assertEquals(listOf(3, 3, 3, 3), result?.diceUsed)
        }

        @Test
        fun `returns null when only three of a kind`() {
            val rule = NOfAKindRule(4, config)
            assertNull(rule.evaluate(listOf(3, 3, 3, 5, 6, 2)))
        }

        @Test
        fun `does not match five of a kind`() {
            val rule = NOfAKindRule(4, config)
            assertNull(rule.evaluate(listOf(3, 3, 3, 3, 3, 6)))
        }
    }

    @Nested
    inner class FiveOfAKindRule {
        @Test
        fun `five of any number scores 2000`() {
            val rule = NOfAKindRule(5, config)
            val result = rule.evaluate(listOf(4, 4, 4, 4, 4, 6))
            assertEquals(2000, result?.points)
            assertEquals(listOf(4, 4, 4, 4, 4), result?.diceUsed)
        }

        @Test
        fun `does not match six of a kind`() {
            val rule = NOfAKindRule(5, config)
            assertNull(rule.evaluate(listOf(4, 4, 4, 4, 4, 4)))
        }
    }

    @Nested
    inner class SixOfAKindRule {
        @Test
        fun `six of any number scores 3000`() {
            val rule = NOfAKindRule(6, config)
            val result = rule.evaluate(listOf(2, 2, 2, 2, 2, 2))
            assertEquals(3000, result?.points)
            assertEquals(listOf(2, 2, 2, 2, 2, 2), result?.diceUsed)
        }

        @Test
        fun `returns null when only five of a kind`() {
            val rule = NOfAKindRule(6, config)
            assertNull(rule.evaluate(listOf(2, 2, 2, 2, 2, 3)))
        }
    }

    @Nested
    inner class StraightRuleTest {
        private val rule = StraightRule(config)

        @Test
        fun `1-2-3-4-5-6 scores 1500`() {
            val result = rule.evaluate(listOf(1, 2, 3, 4, 5, 6))
            assertEquals(1500, result?.points)
            assertEquals(listOf(1, 2, 3, 4, 5, 6).sorted(), result?.diceUsed?.sorted())
        }

        @Test
        fun `order does not matter`() {
            val result = rule.evaluate(listOf(6, 5, 4, 3, 2, 1))
            assertEquals(1500, result?.points)
        }

        @Test
        fun `returns null for partial straight`() {
            assertNull(rule.evaluate(listOf(1, 2, 3, 4, 5, 5)))
        }

        @Test
        fun `requires exactly 6 dice`() {
            assertNull(rule.evaluate(listOf(1, 2, 3, 4, 5)))
        }
    }

    @Nested
    inner class ThreePairsRuleTest {
        private val rule = ThreePairsRule(config)

        @Test
        fun `three pairs scores 1500`() {
            val result = rule.evaluate(listOf(2, 2, 3, 3, 6, 6))
            assertEquals(1500, result?.points)
            assertEquals(6, result?.diceUsed?.size)
        }

        @Test
        fun `order does not matter`() {
            val result = rule.evaluate(listOf(1, 5, 1, 5, 3, 3))
            assertEquals(1500, result?.points)
        }

        @Test
        fun `returns null for two pairs`() {
            assertNull(rule.evaluate(listOf(2, 2, 3, 3, 4, 5)))
        }

        @Test
        fun `requires exactly 6 dice`() {
            assertNull(rule.evaluate(listOf(2, 2, 3, 3)))
        }

        @Test
        fun `three of a kind counts as a pair plus extra - not three pairs`() {
            assertNull(rule.evaluate(listOf(2, 2, 2, 3, 3, 4)))
        }
    }

    @Nested
    inner class FourPlusAPairRuleTest {
        private val rule = FourPlusAPairRule(config)

        @Test
        fun `four plus a pair scores 1500`() {
            val result = rule.evaluate(listOf(3, 3, 3, 3, 5, 5))
            assertEquals(1500, result?.points)
            assertEquals(6, result?.diceUsed?.size)
        }

        @Test
        fun `returns null for four without a pair`() {
            assertNull(rule.evaluate(listOf(3, 3, 3, 3, 5, 6)))
        }

        @Test
        fun `requires exactly 6 dice`() {
            assertNull(rule.evaluate(listOf(3, 3, 3, 3)))
        }
    }

    @Nested
    inner class TwoTripletsRuleTest {
        private val rule = TwoTripletsRule(config)

        @Test
        fun `two triplets scores 2500`() {
            val result = rule.evaluate(listOf(2, 2, 2, 5, 5, 5))
            assertEquals(2500, result?.points)
            assertEquals(6, result?.diceUsed?.size)
        }

        @Test
        fun `returns null for one triplet`() {
            assertNull(rule.evaluate(listOf(2, 2, 2, 3, 4, 5)))
        }

        @Test
        fun `requires exactly 6 dice`() {
            assertNull(rule.evaluate(listOf(2, 2, 2)))
        }
    }
}
