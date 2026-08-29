package com.viennnaa.utilities.feature.rps

import kotlin.random.Random

/** The three moves. */
enum class Move { ROCK, PAPER, SCISSORS }

/** The result from the player's point of view. */
enum class Outcome { WIN, LOSE, DRAW }

/** Picks the opponent's move, each equally likely. */
fun randomMove(random: Random = Random): Move =
    Move.entries[random.nextInt(Move.entries.size)]

/** Who won, from [player]'s point of view. */
fun judge(player: Move, opponent: Move): Outcome = when {
    player == opponent -> Outcome.DRAW
    beats(player, opponent) -> Outcome.WIN
    else -> Outcome.LOSE
}

/** True when [a] beats [b]. */
fun beats(a: Move, b: Move): Boolean = when (a) {
    Move.ROCK -> b == Move.SCISSORS
    Move.PAPER -> b == Move.ROCK
    Move.SCISSORS -> b == Move.PAPER
}

/** Running score across a session. */
data class Score(val wins: Int = 0, val losses: Int = 0, val draws: Int = 0) {
    val played: Int get() = wins + losses + draws
}

fun Score.record(outcome: Outcome): Score = when (outcome) {
    Outcome.WIN -> copy(wins = wins + 1)
    Outcome.LOSE -> copy(losses = losses + 1)
    Outcome.DRAW -> copy(draws = draws + 1)
}
