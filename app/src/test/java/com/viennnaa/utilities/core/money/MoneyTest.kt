package com.viennnaa.utilities.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `parseAmountCents reads whole and decimal amounts`() {
        assertEquals(1200L, parseAmountCents("12"))
        assertEquals(1250L, parseAmountCents("12.5"))
        assertEquals(1234L, parseAmountCents("12.34"))
        assertEquals(34L, parseAmountCents(".34"))
        assertEquals(1200L, parseAmountCents("  12  "))
    }

    @Test
    fun `parseAmountCents accepts a comma as the decimal mark`() {
        assertEquals(1234L, parseAmountCents("12,34"))
    }

    @Test
    fun `parseAmountCents rejects junk`() {
        assertNull(parseAmountCents(""))
        assertNull(parseAmountCents("   "))
        assertNull(parseAmountCents("abc"))
        assertNull(parseAmountCents("12.3.4"))
        assertNull(parseAmountCents("12-34"))
        assertNull(parseAmountCents("-12"))
    }

    @Test
    fun `parseAmountCents rejects more than two decimal places`() {
        // Truncating silently would change the amount, so this is an error.
        assertNull(parseAmountCents("12.345"))
    }

    @Test
    fun `parseAmountCents rejects an amount past the cap`() {
        assertNull(parseAmountCents("2000000"))
    }

    @Test
    fun `formatCents always shows two decimal places`() {
        assertEquals("12.34", formatCents(1234))
        assertEquals("12.00", formatCents(1200))
        assertEquals("12.05", formatCents(1205))
        assertEquals("0.07", formatCents(7))
        assertEquals("0.00", formatCents(0))
        assertEquals("-1.50", formatCents(-150))
    }

    @Test
    fun `parsePercentBasisPoints reads whole and one-decimal rates`() {
        assertEquals(2000, parsePercentBasisPoints("20"))
        assertEquals(1750, parsePercentBasisPoints("17.5"))
        assertEquals(1750, parsePercentBasisPoints("17,5"))
        assertEquals(0, parsePercentBasisPoints("0"))
        assertEquals(50, parsePercentBasisPoints("0.5"))
    }

    @Test
    fun `parsePercentBasisPoints rejects more than one decimal place`() {
        // Rates are not quoted finer than a tenth of a percent in practice.
        assertNull(parsePercentBasisPoints("17.55"))
    }

    @Test
    fun `parsePercentBasisPoints rejects junk and out of range rates`() {
        assertNull(parsePercentBasisPoints(""))
        assertNull(parsePercentBasisPoints("abc"))
        assertNull(parsePercentBasisPoints("-5"))
        assertNull(parsePercentBasisPoints("101"))
        assertNull(parsePercentBasisPoints("100.1"))
    }

    @Test
    fun `parsePercentBasisPoints honours a wider cap when given one`() {
        assertEquals(15000, parsePercentBasisPoints("150", maxPercent = 200))
        assertNull(parsePercentBasisPoints("150"))
    }

    @Test
    fun `formatBasisPoints drops a trailing zero tenth`() {
        assertEquals("20", formatBasisPoints(2000))
        assertEquals("17.5", formatBasisPoints(1750))
        assertEquals("0", formatBasisPoints(0))
        assertEquals("0.5", formatBasisPoints(50))
    }

    @Test
    fun `applyRate rounds half up`() {
        // 10.01 at 15% is 1.5015 -> 1.50
        assertEquals(150L, applyRate(1001, 1500))
        // 3.30 at 15% is 0.495 -> 0.50
        assertEquals(50L, applyRate(330, 1500))
        assertEquals(0L, applyRate(100, 0))
        assertEquals(100L, applyRate(100, BASIS_POINTS_WHOLE))
    }

    @Test
    fun `applyRate handles a fractional rate`() {
        // 100.00 at 17.5% is 17.50
        assertEquals(1750L, applyRate(10_000, 1750))
    }

    @Test
    fun `applyRate stays exact at the top of the range`() {
        // Large amounts must not overflow or drift.
        assertEquals(MAX_AMOUNT_CENTS, applyRate(MAX_AMOUNT_CENTS, BASIS_POINTS_WHOLE))
        assertEquals(MAX_AMOUNT_CENTS / 2, applyRate(MAX_AMOUNT_CENTS, BASIS_POINTS_WHOLE / 2))
    }
}
