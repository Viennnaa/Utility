package com.viennnaa.utilities.feature.diceroller

import kotlin.random.Random

/** Dice types offered, in the order they appear on screen. */
val DiceTypes: List<Int> = listOf(4, 6, 8, 10, 12, 20, 100)

/** How many dice can be thrown at once. */
const val MIN_DICE = 1
const val MAX_DICE = 10

/** How many past totals the mini app keeps on screen. */
const val HISTORY_SIZE = 12

/**
 * Throws [count] dice of [sides] sides each, returning one value per die in the
 * order thrown.
 *
 * [count] is clamped to [MIN_DICE]..[MAX_DICE] and [sides] to at least 2, so a
 * bad saved state cannot produce an empty roll or crash the generator.
 */
fun rollDice(count: Int, sides: Int, random: Random = Random): List<Int> {
    val safeCount = count.coerceIn(MIN_DICE, MAX_DICE)
    val safeSides = sides.coerceAtLeast(2)
    return List(safeCount) { random.nextInt(1, safeSides + 1) }
}

/** Sum of a roll. Zero for an empty roll. */
fun total(rolls: List<Int>): Int = rolls.sum()

/** Clamps a dice count coming from a stepper or restored state. */
fun clampDiceCount(count: Int): Int = count.coerceIn(MIN_DICE, MAX_DICE)

/**
 * Adds [rollTotal] to the front of [history], keeping it at [HISTORY_SIZE] entries.
 */
fun recordTotal(history: List<Int>, rollTotal: Int): List<Int> =
    (listOf(rollTotal) + history).take(HISTORY_SIZE)
