package com.orangezest.farkle.engine

import java.security.SecureRandom

fun interface DiceRoller {
    fun roll(count: Int): List<Int>
}

class SecureRandomDiceRoller : DiceRoller {
    private val random = SecureRandom()

    override fun roll(count: Int): List<Int> =
        List(count) { random.nextInt(6) + 1 }
}

class FakeDiceRoller(private val values: List<Int>) : DiceRoller {
    private var index = 0

    override fun roll(count: Int): List<Int> =
        List(count) {
            values[index % values.size].also { index++ }
        }
}
