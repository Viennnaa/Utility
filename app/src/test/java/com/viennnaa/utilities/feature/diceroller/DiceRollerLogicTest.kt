package com.viennnaa.utilities.feature.diceroller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DiceRollerLogicTest {

    @Test
    fun `rollDice returns one value per die`() {
        assertEquals(3, rollDice(count = 3, sides = 6, random = Random(seed = 1)).size)
    }

    @Test
    fun `rollDice keeps every value on the die`() {
        val random = Random(seed = 42)
        for (sides in DiceTypes) {
            repeat(200) {
                val rolls = rollDice(count = 5, sides = sides, random = random)
                rolls.forEach { value ->
                    assertTrue("$value not on a d$sides", value in 1..sides)
                }
            }
        }
    }

    @Test
    fun `rollDice can produce both ends of a die`() {
        val random = Random(seed = 8)
        val seen = (1..2_000).flatMap { rollDice(count = 1, sides = 6, random = random) }.toSet()
        assertEquals((1..6).toSet(), seen)
    }

    @Test
    fun `rollDice clamps a count below the minimum`() {
        assertEquals(MIN_DICE, rollDice(count = 0, sides = 6, random = Random(seed = 1)).size)
        assertEquals(MIN_DICE, rollDice(count = -5, sides = 6, random = Random(seed = 1)).size)
    }

    @Test
    fun `rollDice clamps a count above the maximum`() {
        assertEquals(MAX_DICE, rollDice(count = 99, sides = 6, random = Random(seed = 1)).size)
    }

    @Test
    fun `rollDice survives a nonsense side count`() {
        val rolls = rollDice(count = 2, sides = 0, random = Random(seed = 1))
        assertEquals(2, rolls.size)
        rolls.forEach { assertTrue("$it not on the fallback die", it in 1..2) }
    }

    @Test
    fun `total sums the roll`() {
        assertEquals(10, total(listOf(1, 4, 5)))
        assertEquals(0, total(emptyList()))
    }

    @Test
    fun `clampDiceCount holds the stepper inside its range`() {
        assertEquals(MIN_DICE, clampDiceCount(MIN_DICE - 1))
        assertEquals(MAX_DICE, clampDiceCount(MAX_DICE + 1))
        assertEquals(3, clampDiceCount(3))
    }

    @Test
    fun `recordTotal puts the newest total first and caps history`() {
        var history = emptyList<Int>()
        repeat(HISTORY_SIZE + 3) { history = recordTotal(history, it) }
        assertEquals(HISTORY_SIZE, history.size)
        assertEquals(HISTORY_SIZE + 2, history.first())
    }
}
