package com.viennnaa.utilities.feature.teamsplitter

import kotlin.random.Random

/**
 * Name editing lives in `core.options`, shared with List Picker. What is left
 * here is the shuffling and the dealing into teams.
 */

/** How many teams can be made at once. */
const val MIN_TEAMS = 1
const val MAX_TEAMS = 10

/** Clamps a team count coming from a stepper or restored state. */
fun clampTeamCount(count: Int): Int = count.coerceIn(MIN_TEAMS, MAX_TEAMS)

/**
 * Splits [names] into [teamCount] teams of as close to equal size as possible.
 *
 * The names are shuffled and then dealt round robin, which keeps team sizes
 * within one of each other and gives every name the same chance of landing on
 * any team. Asking for one team is therefore just a shuffle of the whole list.
 *
 * Empty teams are dropped, so asking for more teams than there are names gives
 * one name per team rather than a tail of blanks. An empty [names] gives an
 * empty result.
 */
fun splitIntoTeams(
    names: List<String>,
    teamCount: Int,
    random: Random = Random,
): List<List<String>> {
    if (names.isEmpty()) return emptyList()
    val teams = clampTeamCount(teamCount)
    val dealt = List(teams) { mutableListOf<String>() }
    names.shuffled(random).forEachIndexed { index, name ->
        dealt[index % teams].add(name)
    }
    return dealt.filter { it.isNotEmpty() }
}
