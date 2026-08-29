package com.viennnaa.utilities.feature.rps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RpsLogicTest {

    @Test
    fun `the full truth table`() {
        assertEquals(Outcome.DRAW, judge(Move.ROCK, Move.ROCK))
        assertEquals(Outcome.LOSE, judge(Move.ROCK, Move.PAPER))
        assertEquals(Outcome.WIN, judge(Move.ROCK, Move.SCISSORS))

        assertEquals(Outcome.WIN, judge(Move.PAPER, Move.ROCK))
        assertEquals(Outcome.DRAW, judge(Move.PAPER, Move.PAPER))
        assertEquals(Outcome.LOSE, judge(Move.PAPER, Move.SCISSORS))

        assertEquals(Outcome.LOSE, judge(Move.SCISSORS, Move.ROCK))
        assertEquals(Outcome.WIN, judge(Move.SCISSORS, Move.PAPER))
        assertEquals(Outcome.DRAW, judge(Move.SCISSORS, Move.SCISSORS))
    }

    @Test
    fun `judging is symmetric`() {
        for (player in Move.entries) {
            for (opponent in Move.entries) {
                val mine = judge(player, opponent)
                val theirs = judge(opponent, player)
                val expected = when (mine) {
                    Outcome.WIN -> Outcome.LOSE
                    Outcome.LOSE -> Outcome.WIN
                    Outcome.DRAW -> Outcome.DRAW
                }
                assertEquals("$player vs $opponent", expected, theirs)
            }
        }
    }

    @Test
    fun `every move beats exactly one other move`() {
        for (move in Move.entries) {
            assertEquals(1, Move.entries.count { beats(move, it) })
        }
    }

    @Test
    fun `randomMove can return every move`() {
        val random = Random(seed = 5)
        val seen = (1..300).map { randomMove(random) }.toSet()
        assertEquals(Move.entries.toSet(), seen)
    }

    @Test
    fun `score records each outcome`() {
        var score = Score()
        score = score.record(Outcome.WIN).record(Outcome.WIN).record(Outcome.LOSE).record(Outcome.DRAW)
        assertEquals(Score(wins = 2, losses = 1, draws = 1), score)
        assertEquals(4, score.played)
    }

    @Test
    fun `a fresh score is empty`() {
        assertEquals(0, Score().played)
    }

    @Test
    fun `an opponent picking at random wins about a third`() {
        val random = Random(seed = 77)
        val rounds = 9_000
        val wins = (1..rounds).count { judge(Move.ROCK, randomMove(random)) == Outcome.WIN }
        assertTrue("won $wins of $rounds", wins in 2_700..3_300)
    }
}
