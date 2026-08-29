package com.viennnaa.utilities.feature.tipsplitter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TipSplitterLogicTest {

    @Test
    fun `tip is rounded half up`() {
        // 10.01 at 15% is 1.5015 -> 1.50
        assertEquals(150L, splitBill(1001, 15, 1).tipCents)
        // 3.30 at 15% is 0.495 -> 0.50
        assertEquals(50L, splitBill(330, 15, 1).tipCents)
    }

    @Test
    fun `zero percent leaves the bill alone`() {
        val split = splitBill(2500, 0, 2)
        assertEquals(0L, split.tipCents)
        assertEquals(2500L, split.totalCents)
    }

    @Test
    fun `an even split gives everyone the same`() {
        val split = splitBill(3000, 0, 3)
        assertEquals(listOf(1000L, 1000L, 1000L), split.perPerson)
    }

    @Test
    fun `leftover cents are handed out one each`() {
        val split = splitBill(1000, 0, 3)
        assertEquals(listOf(334L, 333L, 333L), split.perPerson)
    }

    @Test
    fun `shares always add back to the total`() {
        for (bill in listOf(0L, 1L, 999L, 1000L, 1234L, 98_765L, 1_000_000L)) {
            for (percent in TipPresets + listOf(7, 33, 100)) {
                for (people in MIN_PEOPLE..MAX_PEOPLE) {
                    val split = splitBill(bill, percent, people)
                    assertEquals(
                        "bill=$bill percent=$percent people=$people",
                        split.totalCents,
                        split.perPerson.sum(),
                    )
                    assertEquals(people, split.perPerson.size)
                }
            }
        }
    }

    @Test
    fun `shares never differ by more than a cent`() {
        val split = splitBill(1000, 15, 7)
        assertTrue(split.perPerson.max() - split.perPerson.min() <= 1)
    }

    @Test
    fun `a single person owes the whole total`() {
        val split = splitBill(2000, 20, 1)
        assertEquals(2400L, split.totalCents)
        assertEquals(listOf(2400L), split.perPerson)
    }

    @Test
    fun `out of range input is clamped rather than crashing`() {
        val split = splitBill(-500, -10, 0)
        assertEquals(0L, split.billCents)
        assertEquals(0L, split.tipCents)
        assertEquals(MIN_PEOPLE, split.perPerson.size)

        val wide = splitBill(1000, 500, 99)
        assertEquals(MAX_TIP_PERCENT, (wide.tipCents * 100 / wide.billCents).toInt())
        assertEquals(MAX_PEOPLE, wide.perPerson.size)
    }

    @Test
    fun `clampPeople holds the stepper inside its range`() {
        assertEquals(MIN_PEOPLE, clampPeople(0))
        assertEquals(MAX_PEOPLE, clampPeople(MAX_PEOPLE + 1))
        assertEquals(4, clampPeople(4))
    }
}
