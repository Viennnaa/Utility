package com.viennnaa.utilities.feature.listpicker

import kotlin.random.Random

/**
 * Option editing lives in `core.options`, shared with the other mini apps that
 * collect a list. What is left here is the picking itself.
 */

/** How many past picks the mini app keeps on screen. */
const val HISTORY_SIZE = 12

/**
 * Picks one option's index, every option equally likely. Null when there is
 * nothing to pick from.
 */
fun pickIndex(options: List<String>, random: Random = Random): Int? =
    if (options.isEmpty()) null else random.nextInt(options.size)

/**
 * Adds [pick] to the front of [history], keeping it at [HISTORY_SIZE] entries.
 */
fun recordPick(history: List<String>, pick: String): List<String> =
    (listOf(pick) + history).take(HISTORY_SIZE)
