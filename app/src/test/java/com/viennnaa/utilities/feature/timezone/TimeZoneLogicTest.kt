package com.viennnaa.utilities.feature.timezone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TimeZoneLogicTest {

    private val london = ZoneId.of("Europe/London")
    private val newYork = ZoneId.of("America/New_York")
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val auckland = ZoneId.of("Pacific/Auckland")
    private val utc = ZoneId.of("UTC")

    /** Midday UTC on a fixed winter day. */
    private val winter: Instant = Instant.parse("2026-01-15T12:00:00Z")

    /** Midday UTC on a fixed summer day. */
    private val summer: Instant = Instant.parse("2026-07-15T12:00:00Z")

    @Test
    fun `the same instant reads differently in each zone`() {
        assertEquals("12:00", formatClock(winter, utc))
        assertEquals("12:00", formatClock(winter, london))
        assertEquals("07:00", formatClock(winter, newYork))
        assertEquals("17:30", formatClock(winter, kolkata))
    }

    @Test
    fun `daylight saving moves the clock, and the tz database knows when`() {
        // London is UTC in January and an hour ahead in July.
        assertEquals("12:00", formatClock(winter, london))
        assertEquals("13:00", formatClock(summer, london))
        assertEquals("UTC", offsetLabel(winter, london))
        assertEquals("+01:00", offsetLabel(summer, london))
    }

    @Test
    fun `a zone without daylight saving does not move`() {
        assertEquals("+05:30", offsetLabel(winter, kolkata))
        assertEquals("+05:30", offsetLabel(summer, kolkata))
    }

    @Test
    fun `half hour offsets survive`() {
        assertEquals("17:30", formatClock(winter, kolkata))
        assertEquals("17:30", formatClock(summer, kolkata))
    }

    @Test
    fun `southern hemisphere daylight saving runs the other way`() {
        // Auckland is on summer time in January, standard time in July.
        assertEquals("+13:00", offsetLabel(winter, auckland))
        assertEquals("+12:00", offsetLabel(summer, auckland))
    }

    @Test
    fun `zero offset reads as UTC rather than Z`() {
        assertEquals("UTC", offsetLabel(winter, utc))
    }

    @Test
    fun `a day boundary is reported as a shift`() {
        // Midday in London is the next day in Auckland.
        assertEquals(1, dayShift(winter, london, auckland))
        assertEquals(-1, dayShift(winter, auckland, london))
        assertEquals(0, dayShift(winter, london, newYork))
        assertEquals(0, dayShift(winter, london, london))
    }

    @Test
    fun `a late evening instant pushes New York back a day`() {
        val lateInLondon = Instant.parse("2026-01-15T02:00:00Z")
        assertEquals(-1, dayShift(lateInLondon, london, newYork))
    }

    @Test
    fun `a local time converts to the right instant`() {
        val instant = instantAt(LocalDate.parse("2026-07-15"), LocalTime.of(13, 0), london)
        // 13:00 BST is 12:00 UTC.
        assertEquals(summer, instant)
    }

    @Test
    fun `a local time in winter converts without a daylight saving offset`() {
        val instant = instantAt(LocalDate.parse("2026-01-15"), LocalTime.of(12, 0), london)
        assertEquals(winter, instant)
    }

    @Test
    fun `a local time that daylight saving skips still resolves`() {
        // Clocks go forward at 01:00 on 29 March 2026 in London, so 01:30 does
        // not exist. It must resolve to something rather than throwing.
        val instant = instantAt(LocalDate.parse("2026-03-29"), LocalTime.of(1, 30), london)
        assertNotNull(instant)
        assertEquals("+01:00", offsetLabel(instant, london))
    }

    @Test
    fun `a local time that daylight saving repeats still resolves`() {
        // Clocks go back at 02:00 on 25 October 2026, so 01:30 happens twice.
        val instant = instantAt(LocalDate.parse("2026-10-25"), LocalTime.of(1, 30), london)
        assertNotNull(instant)
        // atZone takes the earlier of the two, which is still on summer time.
        assertEquals("+01:00", offsetLabel(instant, london))
    }

    @Test
    fun `every offered zone exists on this platform`() {
        for (entry in CommonZones) {
            assertNotNull("${entry.id} is not a known zone", zoneOf(entry.id))
        }
    }

    @Test
    fun `zone ids and labels are unique`() {
        assertEquals(CommonZones.size, CommonZones.map { it.id }.toSet().size)
        assertEquals(CommonZones.size, CommonZones.map { it.label }.toSet().size)
    }

    @Test
    fun `an unknown zone id has no zone rather than throwing`() {
        assertNull(zoneOf("Mars/Olympus_Mons"))
        assertNull(zoneOf(""))
        assertNull(zoneOf("not a zone"))
    }

    @Test
    fun `zones sort east to west by their offset at that moment`() {
        val sorted = zonesByOffset(winter)
        val offsets = sorted.mapNotNull { zoneOf(it.id) }
            .map { timeIn(winter, it).offset.totalSeconds }
        assertEquals(offsets.sortedDescending(), offsets)
        assertEquals(CommonZones.size, sorted.size)
    }

    @Test
    fun `the sort follows daylight saving rather than being fixed`() {
        // Auckland leads in January; in July it has dropped an hour while
        // London has gained one, so the gap between them narrows.
        val winterGap = timeIn(winter, auckland).offset.totalSeconds -
            timeIn(winter, london).offset.totalSeconds
        val summerGap = timeIn(summer, auckland).offset.totalSeconds -
            timeIn(summer, london).offset.totalSeconds
        assertTrue("winter=$winterGap summer=$summerGap", winterGap > summerGap)
    }

    @Test
    fun `parseClock reads the forms people type`() {
        assertEquals(LocalTime.of(9, 5), parseClock("09:05"))
        assertEquals(LocalTime.of(9, 5), parseClock("9:05"))
        assertEquals(LocalTime.of(21, 30), parseClock("21:30"))
        assertEquals(LocalTime.of(0, 0), parseClock("00:00"))
        assertEquals(LocalTime.of(9, 5), parseClock("  9:05  "))
    }

    @Test
    fun `parseClock rejects junk and impossible times`() {
        assertNull(parseClock(""))
        assertNull(parseClock("25:00"))
        assertNull(parseClock("12:60"))
        assertNull(parseClock("noon"))
        assertNull(parseClock("12"))
    }
}
