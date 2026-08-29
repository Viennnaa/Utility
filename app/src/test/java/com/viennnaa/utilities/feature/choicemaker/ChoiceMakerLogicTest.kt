package com.viennnaa.utilities.feature.choicemaker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChoiceMakerLogicTest {

    @Test
    fun `decide returns both answers`() {
        val random = Random(seed = 11)
        val seen = (1..200).map { decide(random) }.toSet()
        assertEquals(setOf(Choice.Yes, Choice.No), seen)
    }

    @Test
    fun `decide is close to an even split over many draws`() {
        val random = Random(seed = 2024)
        val draws = 10_000
        val yes = (1..draws).count { decide(random) == Choice.Yes }
        // A fair coin lands this far from half only very rarely.
        assertTrue("$yes yes out of $draws is lopsided", yes in 4_700..5_300)
    }

    @Test
    fun `recordChoice puts the newest answer first`() {
        val history = recordChoice(recordChoice(emptyList(), Choice.No), Choice.Yes)
        assertEquals(listOf(Choice.Yes, Choice.No), history)
    }

    @Test
    fun `recordChoice keeps history capped`() {
        var history = emptyList<Choice>()
        repeat(HISTORY_SIZE + 7) { history = recordChoice(history, Choice.Yes) }
        assertEquals(HISTORY_SIZE, history.size)
    }

    @Test
    fun `tally counts each answer`() {
        val history = listOf(Choice.Yes, Choice.No, Choice.Yes, Choice.Yes)
        assertEquals(Tally(yes = 3, no = 1), tally(history))
        assertEquals(4, tally(history).total)
    }

    @Test
    fun `tally of an empty history is zero`() {
        assertEquals(Tally(yes = 0, no = 0), tally(emptyList()))
        assertEquals(0, tally(emptyList()).total)
    }
}
