package com.viennnaa.utilities.feature.randomnumber

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomNumberLogicTest {

    @Test
    fun `validateRange accepts a normal range`() {
        val result = validateRange("1", "100")
        assertEquals(RangeValidation.Valid(1..100), result)
    }

    @Test
    fun `validateRange accepts negative bounds and surrounding whitespace`() {
        assertEquals(RangeValidation.Valid(-10..-2), validateRange(" -10 ", " -2 "))
    }

    @Test
    fun `validateRange accepts a single value range`() {
        assertEquals(RangeValidation.Valid(5..5), validateRange("5", "5"))
    }

    @Test
    fun `validateRange rejects text that is not a whole number`() {
        assertEquals(RangeValidation.Problem.NotANumber, validateRange("", "10"))
        assertEquals(RangeValidation.Problem.NotANumber, validateRange("1", ""))
        assertEquals(RangeValidation.Problem.NotANumber, validateRange("two", "10"))
        assertEquals(RangeValidation.Problem.NotANumber, validateRange("1.5", "10"))
    }

    @Test
    fun `validateRange reports bounds beyond the limit as out of bounds`() {
        val tooBig = (RANGE_LIMIT + 1L).toString()
        assertEquals(RangeValidation.Problem.OutOfBounds, validateRange("1", tooBig))
        assertEquals(RangeValidation.Problem.OutOfBounds, validateRange("-$tooBig", "1"))
        // Wider than an Int, which must still read as out of bounds rather than
        // as unparseable text.
        assertEquals(RangeValidation.Problem.OutOfBounds, validateRange("1", "9999999999"))
    }

    @Test
    fun `validateRange rejects an inverted range`() {
        assertEquals(RangeValidation.Problem.MinAboveMax, validateRange("10", "1"))
    }

    @Test
    fun `randomIn stays inside the range`() {
        val random = Random(seed = 42)
        repeat(1_000) {
            val value = randomIn(1..10, random)
            assertTrue("$value outside 1..10", value in 1..10)
        }
    }

    @Test
    fun `randomIn can return both ends of the range`() {
        val random = Random(seed = 7)
        val seen = (1..1_000).map { randomIn(1..6, random) }.toSet()
        assertEquals((1..6).toSet(), seen)
    }

    @Test
    fun `randomIn handles a single value range`() {
        assertEquals(5, randomIn(5..5, Random(seed = 1)))
    }

    @Test
    fun `randomIn handles the widest allowed range without overflowing`() {
        val random = Random(seed = 3)
        repeat(100) {
            val value = randomIn(-RANGE_LIMIT..RANGE_LIMIT, random)
            assertTrue("$value outside the allowed range", value in -RANGE_LIMIT..RANGE_LIMIT)
        }
    }

    @Test
    fun `recordResult puts the newest value first`() {
        val history = recordResult(recordResult(emptyList(), 1), 2)
        assertEquals(listOf(2, 1), history)
    }

    @Test
    fun `recordResult keeps history capped`() {
        var history = emptyList<Int>()
        repeat(HISTORY_SIZE + 5) { history = recordResult(history, it) }
        assertEquals(HISTORY_SIZE, history.size)
        assertEquals(HISTORY_SIZE + 4, history.first())
    }

    @Test
    fun `sanitizeBoundInput keeps only digits`() {
        assertEquals("123", sanitizeBoundInput("1a2b3"))
        assertEquals("15", sanitizeBoundInput("1.5"))
        assertEquals("", sanitizeBoundInput("abc"))
    }

    @Test
    fun `sanitizeBoundInput keeps a leading minus only`() {
        assertEquals("-12", sanitizeBoundInput("-12"))
        assertEquals("12", sanitizeBoundInput("1-2"))
    }

    @Test
    fun `sanitizeBoundInput caps the number of digits`() {
        assertEquals(10, sanitizeBoundInput("1".repeat(30)).length)
    }
}
