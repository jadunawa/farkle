package com.orangezest.farkle.engine

class SinglesRule(private val face: Int, private val config: GameConfig) : ScoringRule {
    override val name: String = "Single $face"

    private val pointValue: Int
        get() = when (face) {
            1 -> config.single1Points
            5 -> config.single5Points
            else -> 0
        }

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (face !in dice) return null
        return ScoringResult(
            points = pointValue,
            diceUsed = listOf(face),
            name = name,
        )
    }
}

class NOfAKindRule(private val count: Int, private val config: GameConfig) : ScoringRule {
    override val name: String = "$count of a Kind"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        val face = dice.groupingBy { it }.eachCount()
            .entries.find { it.value == count }?.key
            ?: return null

        val points = when (count) {
            3 -> when (face) {
                1 -> config.three1sPoints
                2 -> config.three2sPoints
                3 -> config.three3sPoints
                4 -> config.three4sPoints
                5 -> config.three5sPoints
                6 -> config.three6sPoints
                else -> 0
            }
            4 -> config.fourOfAKindPoints
            5 -> config.fiveOfAKindPoints
            6 -> config.sixOfAKindPoints
            else -> 0
        }

        return ScoringResult(
            points = points,
            diceUsed = List(count) { face },
            name = name,
        )
    }
}

class StraightRule(private val config: GameConfig) : ScoringRule {
    override val name: String = "Straight"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (dice.size != 6) return null
        if (dice.sorted() != listOf(1, 2, 3, 4, 5, 6)) return null
        return ScoringResult(
            points = config.straightPoints,
            diceUsed = dice.sorted(),
            name = name,
        )
    }
}

class ThreePairsRule(private val config: GameConfig) : ScoringRule {
    override val name: String = "Three Pairs"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (dice.size != 6) return null
        val counts = dice.groupingBy { it }.eachCount()
        if (counts.size != 3 || !counts.values.all { it == 2 }) return null
        return ScoringResult(
            points = config.threePairsPoints,
            diceUsed = dice.sorted(),
            name = name,
        )
    }
}

class FourPlusAPairRule(private val config: GameConfig) : ScoringRule {
    override val name: String = "Four + a Pair"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (dice.size != 6) return null
        val counts = dice.groupingBy { it }.eachCount()
        if (counts.size != 2) return null
        val values = counts.values.sorted()
        if (values != listOf(2, 4)) return null
        return ScoringResult(
            points = config.fourPlusAPairPoints,
            diceUsed = dice.sorted(),
            name = name,
        )
    }
}

class TwoTripletsRule(private val config: GameConfig) : ScoringRule {
    override val name: String = "Two Triplets"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (dice.size != 6) return null
        val counts = dice.groupingBy { it }.eachCount()
        if (counts.size != 2 || !counts.values.all { it == 3 }) return null
        return ScoringResult(
            points = config.twoTripletsPoints,
            diceUsed = dice.sorted(),
            name = name,
        )
    }
}
