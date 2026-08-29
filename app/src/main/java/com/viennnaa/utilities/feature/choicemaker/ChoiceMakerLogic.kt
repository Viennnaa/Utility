package com.viennnaa.utilities.feature.choicemaker

import kotlin.random.Random

/** How many past answers the mini app keeps on screen. */
const val HISTORY_SIZE = 20

/** The two answers the choice maker can give. */
enum class Choice { Yes, No }

/** Flips a fair coin: [Choice.Yes] and [Choice.No] are equally likely. */
fun decide(random: Random = Random): Choice =
    if (random.nextBoolean()) Choice.Yes else Choice.No

/**
 * Adds [choice] to the front of [history], keeping it at [HISTORY_SIZE] entries.
 */
fun recordChoice(history: List<Choice>, choice: Choice): List<Choice> =
    (listOf(choice) + history).take(HISTORY_SIZE)

/** Running count of each answer, for the tally under the result. */
data class Tally(val yes: Int, val no: Int) {
    val total: Int get() = yes + no
}

fun tally(history: List<Choice>): Tally =
    Tally(
        yes = history.count { it == Choice.Yes },
        no = history.count { it == Choice.No },
    )
