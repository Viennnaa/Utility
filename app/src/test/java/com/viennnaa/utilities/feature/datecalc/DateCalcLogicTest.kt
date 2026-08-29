package com.viennnaa.utilities.feature.datecalc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateCalcLogicTest {

    private fun date(text: String) = LocalDate.parse(text)

    @Test
    fun `days between counts whole days`() {
        assertEquals(1L, daysBetween(date("2026-01-01"), date("2026-01-02")))
        assertEquals(31L, daysBetween(date("2026-01-01"), date("2026-02-01")))
        assertEquals(0L, daysBetween(date("2026-01-01"), date("2026-01-01")))
    }

    @Test
    fun `days between is signed by direction`() {
        assertEquals(-1L, daysBetween(date("2026-01-02"), date("2026-01-01")))
    }

    @Test
    fun `leap days are counted`() {
        // 2024 is a leap year, so February has 29 days.
        assertEquals(29L, daysBetween(date("2024-02-01"), date("2024-03-01")))
        assertEquals(28L, daysBetween(date("2026-02-01"), date("2026-03-01")))
        assertEquals(366L, daysBetween(date("2024-01-01"), date("2025-01-01")))
        assertEquals(365L, daysBetween(date("2026-01-01"), date("2027-01-01")))
    }

    @Test
    fun `century years follow the leap rule`() {
        // 2000 was a leap year; 1900 was not.
        assertEquals(366L, daysBetween(date("2000-01-01"), date("2001-01-01")))
        assertEquals(365L, daysBetween(date("1900-01-01"), date("1901-01-01")))
    }

    @Test
    fun `adding days crosses months and years`() {
        assertEquals(date("2026-01-02"), addDays(date("2026-01-01"), 1))
        assertEquals(date("2026-02-01"), addDays(date("2026-01-31"), 1))
        assertEquals(date("2027-01-01"), addDays(date("2026-12-31"), 1))
    }

    @Test
    fun `subtracting days works through the same function`() {
        assertEquals(date("2025-12-31"), addDays(date("2026-01-01"), -1))
    }

    @Test
    fun `adding days lands on the leap day`() {
        assertEquals(date("2024-02-29"), addDays(date("2024-02-28"), 1))
        assertEquals(date("2026-03-01"), addDays(date("2026-02-28"), 1))
    }

    @Test
    fun `age is whole years months and days`() {
        val age = ageOn(date("1990-05-15"), date("2026-08-29"))!!
        assertEquals(36, age.years)
        assertEquals(3, age.months)
        assertEquals(14, age.days)
    }

    @Test
    fun `age on a birthday is a whole number of years`() {
        val age = ageOn(date("2000-08-29"), date("2026-08-29"))!!
        assertEquals(26, age.years)
        assertEquals(0, age.months)
        assertEquals(0, age.days)
    }

    @Test
    fun `age the day before a birthday is still the previous year`() {
        val age = ageOn(date("2000-08-29"), date("2026-08-28"))!!
        assertEquals(25, age.years)
    }

    @Test
    fun `a birth date in the future has no age`() {
        assertNull(ageOn(date("2030-01-01"), date("2026-08-29")))
    }

    @Test
    fun `period between is order independent`() {
        val forward = periodBetween(date("2020-01-01"), date("2026-03-15"))
        val backward = periodBetween(date("2026-03-15"), date("2020-01-01"))
        assertEquals(forward, backward)
        assertEquals(6, forward.years)
    }

    @Test
    fun `parseDate reads ISO dates and rejects the rest`() {
        assertEquals(date("2026-08-29"), parseDate("2026-08-29"))
        assertEquals(date("2026-08-29"), parseDate("  2026-08-29 "))
        assertNull(parseDate(""))
        assertNull(parseDate("29/08/2026"))
        assertNull(parseDate("2026-13-01"))
        assertNull(parseDate("2026-02-30"))
        assertNull(parseDate("not a date"))
    }

    @Test
    fun `weeks and days splits a day count`() {
        assertEquals(2L to 3L, weeksAndDays(17))
        assertEquals(0L to 5L, weeksAndDays(5))
        assertEquals(1L to 0L, weeksAndDays(7))
        // The split describes a magnitude, so direction does not flip it.
        assertEquals(2L to 3L, weeksAndDays(-17))
    }
}
