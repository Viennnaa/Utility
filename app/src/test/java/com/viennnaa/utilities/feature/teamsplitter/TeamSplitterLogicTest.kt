package com.viennnaa.utilities.feature.teamsplitter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class TeamSplitterLogicTest {

    private val names = listOf("Ana", "Ben", "Cleo", "Dev", "Eli", "Fay", "Gus")

    @Test
    fun `every name lands on exactly one team`() {
        val random = Random(seed = 4)
        for (teams in MIN_TEAMS..MAX_TEAMS) {
            val result = splitIntoTeams(names, teams, random)
            assertEquals(names.sorted(), result.flatten().sorted())
        }
    }

    @Test
    fun `team sizes differ by at most one`() {
        val random = Random(seed = 6)
        for (teams in 1..4) {
            val sizes = splitIntoTeams(names, teams, random).map { it.size }
            assertTrue("sizes were $sizes", abs(sizes.max() - sizes.min()) <= 1)
        }
    }

    @Test
    fun `one team is just a shuffle of the whole list`() {
        val result = splitIntoTeams(names, 1, Random(seed = 2))
        assertEquals(1, result.size)
        assertEquals(names.sorted(), result.first().sorted())
    }

    @Test
    fun `asking for more teams than names drops the empty ones`() {
        val result = splitIntoTeams(listOf("Ana", "Ben"), MAX_TEAMS, Random(seed = 1))
        assertEquals(2, result.size)
        result.forEach { assertEquals(1, it.size) }
    }

    @Test
    fun `an empty roster gives no teams`() {
        assertTrue(splitIntoTeams(emptyList(), 3, Random(seed = 1)).isEmpty())
    }

    @Test
    fun `a team count outside the range is clamped`() {
        assertEquals(MIN_TEAMS, splitIntoTeams(names, 0, Random(seed = 1)).size)
        assertEquals(MAX_TEAMS, clampTeamCount(MAX_TEAMS + 5))
        assertEquals(MIN_TEAMS, clampTeamCount(-3))
    }

    @Test
    fun `the order actually changes across rolls`() {
        val random = Random(seed = 12)
        val orders = (1..40).map { splitIntoTeams(names, 1, random).first() }.toSet()
        assertTrue("shuffling produced only ${orders.size} distinct orders", orders.size > 1)
    }
}
