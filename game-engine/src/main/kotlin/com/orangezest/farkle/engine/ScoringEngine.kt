package com.orangezest.farkle.engine

class ScoringEngine(private val config: GameConfig) {

    private val rules: List<ScoringRule> by lazy {
        listOf(
            NOfAKindRule(6, config),
            NOfAKindRule(5, config),
            FourPlusAPairRule(config),
            NOfAKindRule(4, config),
            TwoTripletsRule(config),
            ThreePairsRule(config),
            StraightRule(config),
            NOfAKindRule(3, config),
            SinglesRule(1, config),
            SinglesRule(5, config),
        )
    }

    fun findScoringOptions(dice: List<Int>): List<ScoringResult> =
        rules.mapNotNull { it.evaluate(dice) }

    fun isFarkle(dice: List<Int>): Boolean =
        findScoringOptions(dice).isEmpty()

    fun scoreSelection(roll: List<Int>, selection: List<Int>): Int {
        if (selection.isEmpty()) return 0

        val remaining = roll.toMutableList()
        for (die in selection) {
            if (!remaining.remove(die)) return 0
        }

        return calculateScore(selection)
    }

    private fun calculateScore(dice: List<Int>): Int {
        val mutable = dice.toMutableList()
        var total = 0

        val allDiceRules = listOf(
            StraightRule(config),
            TwoTripletsRule(config),
            ThreePairsRule(config),
            FourPlusAPairRule(config),
        )
        for (rule in allDiceRules) {
            rule.evaluate(mutable)?.let { return it.points }
        }

        for (n in listOf(6, 5, 4, 3)) {
            val nOfAKind = NOfAKindRule(n, config)
            nOfAKind.evaluate(mutable)?.let { result ->
                total += result.points
                result.diceUsed.forEach { mutable.remove(it) }
            }
        }

        val ones = mutable.count { it == 1 }
        total += ones * config.single1Points
        repeat(ones) { mutable.remove(1) }

        val fives = mutable.count { it == 5 }
        total += fives * config.single5Points
        repeat(fives) { mutable.remove(5) }

        if (mutable.isNotEmpty()) return 0

        return total
    }
}
