package com.viennnaa.utilities.feature.percentage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class PercentageLogicTest {

    @Test
    fun `percent of a value`() {
        assertEquals(25.0, percentOf(25.0, 100.0), 1e-9)
        assertEquals(15.0, percentOf(50.0, 30.0), 1e-9)
        assertEquals(0.0, percentOf(0.0, 30.0), 1e-9)
        assertEquals(-5.0, percentOf(10.0, -50.0), 1e-9)
    }

    @Test
    fun `what percent one number is of another`() {
        assertEquals(50.0, whatPercent(5.0, 10.0)!!, 1e-9)
        assertEquals(200.0, whatPercent(20.0, 10.0)!!, 1e-9)
        assertEquals(0.0, whatPercent(0.0, 10.0)!!, 1e-9)
    }

    @Test
    fun `a share of nothing has no answer`() {
        assertNull(whatPercent(5.0, 0.0))
    }

    @Test
    fun `percentage change in both directions`() {
        assertEquals(100.0, percentChange(10.0, 20.0)!!, 1e-9)
        assertEquals(-50.0, percentChange(20.0, 10.0)!!, 1e-9)
        assertEquals(0.0, percentChange(10.0, 10.0)!!, 1e-9)
    }

    @Test
    fun `there is no percentage increase from zero`() {
        assertNull(percentChange(0.0, 10.0))
    }

    @Test
    fun `parseNumber accepts the forms people type`() {
        assertEquals(12.5, parseNumber("12.5")!!, 1e-9)
        assertEquals(12.5, parseNumber("12,5")!!, 1e-9)
        assertEquals(-7.0, parseNumber(" -7 ")!!, 1e-9)
    }

    @Test
    fun `parseNumber rejects junk and partial input`() {
        assertNull(parseNumber(""))
        assertNull(parseNumber("-"))
        assertNull(parseNumber("."))
        assertNull(parseNumber("twelve"))
    }

    @Test
    fun `formatNumber trims noise without leaving a stray separator`() {
        assertEquals("25", formatNumber(25.0))
        assertEquals("2.5", formatNumber(2.5))
        assertEquals("0", formatNumber(0.0))
        assertEquals("-12.75", formatNumber(-12.75))
    }

    @Test
    fun `formatNumber is unaffected by a comma-decimal default locale`() {
        // The trimming strips a trailing '.', so a locale that formats with ','
        // would leave "25," behind. This test fails without the explicit locale,
        // and passes by luck on an en-US machine, which is why it sets one.
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("25", formatNumber(25.0))
            assertEquals("2.5", formatNumber(2.5))
            assertEquals("-12.75", formatNumber(-12.75))
        } finally {
            Locale.setDefault(original)
        }
    }
}
