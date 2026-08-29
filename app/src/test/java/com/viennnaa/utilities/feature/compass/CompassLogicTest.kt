package com.viennnaa.utilities.feature.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CompassLogicTest {

    @Test
    fun `normalize brings any angle into range`() {
        assertEquals(0.0, normalizeDegrees(0.0), 1e-9)
        assertEquals(10.0, normalizeDegrees(370.0), 1e-9)
        assertEquals(350.0, normalizeDegrees(-10.0), 1e-9)
        assertEquals(180.0, normalizeDegrees(-180.0), 1e-9)
        assertEquals(0.0, normalizeDegrees(720.0), 1e-9)
    }

    @Test
    fun `delta takes the short way round`() {
        assertEquals(2.0, angleDelta(359.0, 1.0), 1e-9)
        assertEquals(-2.0, angleDelta(1.0, 359.0), 1e-9)
        assertEquals(10.0, angleDelta(0.0, 10.0), 1e-9)
        assertEquals(0.0, angleDelta(90.0, 90.0), 1e-9)
    }

    @Test
    fun `delta stays within half a turn`() {
        for (from in 0..359) {
            for (to in 0..359 step 7) {
                val delta = angleDelta(from.toDouble(), to.toDouble())
                assertTrue("$from to $to gave $delta", delta > -180.5 && delta <= 180.5)
            }
        }
    }

    @Test
    fun `smoothing does not spin the long way across north`() {
        // Naive interpolation from 359 toward 1 would sweep back through 180.
        val blended = smoothHeading(359.0, 1.0, 0.5)
        assertTrue("went the long way: $blended", blended >= 359.5 || blended <= 0.5)
    }

    @Test
    fun `the first heading is taken as is`() {
        assertEquals(42.0, smoothHeading(null, 42.0), 1e-9)
        assertEquals(350.0, smoothHeading(null, -10.0), 1e-9)
    }

    @Test
    fun `smoothing converges across the wrap`() {
        var heading = 350.0
        repeat(300) { heading = smoothHeading(heading, 10.0) }
        assertTrue("ended at $heading", abs(angleDelta(heading, 10.0)) < 0.01)
    }

    @Test
    fun `smoothed headings stay in range`() {
        var heading = 0.0
        for (step in 0..720) {
            heading = smoothHeading(heading, step.toDouble())
            assertTrue("$heading out of range", heading >= 0.0 && heading < 360.0)
        }
    }

    @Test
    fun `cardinal points map to their bearings`() {
        assertEquals(Cardinal.NORTH, cardinalFor(0.0))
        assertEquals(Cardinal.NORTHEAST, cardinalFor(45.0))
        assertEquals(Cardinal.EAST, cardinalFor(90.0))
        assertEquals(Cardinal.SOUTHEAST, cardinalFor(135.0))
        assertEquals(Cardinal.SOUTH, cardinalFor(180.0))
        assertEquals(Cardinal.SOUTHWEST, cardinalFor(225.0))
        assertEquals(Cardinal.WEST, cardinalFor(270.0))
        assertEquals(Cardinal.NORTHWEST, cardinalFor(315.0))
    }

    @Test
    fun `north spans the wrap in both directions`() {
        assertEquals(Cardinal.NORTH, cardinalFor(350.0))
        assertEquals(Cardinal.NORTH, cardinalFor(10.0))
        assertEquals(Cardinal.NORTH, cardinalFor(360.0))
    }

    @Test
    fun `every angle has a cardinal`() {
        for (degrees in -720..1080) {
            cardinalFor(degrees.toDouble())
        }
    }

    @Test
    fun `heading formats as whole degrees under a full turn`() {
        assertEquals(0, formatHeading(0.0))
        assertEquals(0, formatHeading(359.7))
        assertEquals(45, formatHeading(44.6))
        assertEquals(350, formatHeading(-10.0))
    }

    @Test
    fun `facing north allows a small wobble either side`() {
        assertTrue(isFacingNorth(0.0))
        assertTrue(isFacingNorth(1.0))
        assertTrue(isFacingNorth(359.0))
        assertFalse(isFacingNorth(20.0))
    }
}
