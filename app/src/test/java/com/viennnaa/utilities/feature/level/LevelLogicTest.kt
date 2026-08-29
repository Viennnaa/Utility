package com.viennnaa.utilities.feature.level

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelLogicTest {

    private val g = 9.81f

    @Test
    fun `flat on its back reads level`() {
        val tilt = tiltFrom(0f, 0f, g)
        assertEquals(0.0, tilt.roll, 1e-6)
        assertEquals(0.0, tilt.pitch, 1e-6)
        assertTrue(tilt.isLevel)
    }

    @Test
    fun `standing on its bottom edge is ninety degrees of pitch`() {
        val tilt = tiltFrom(0f, g, 0f)
        assertEquals(90.0, tilt.pitch, 1e-6)
        assertFalse(tilt.isLevel)
    }

    @Test
    fun `on its side is ninety degrees of roll`() {
        val tilt = tiltFrom(g, 0f, 0f)
        assertEquals(-90.0, tilt.roll, 1e-6)
    }

    @Test
    fun `roll sign follows which edge is raised`() {
        assertTrue(tiltFrom(-g / 2, 0f, g).roll > 0)
        assertTrue(tiltFrom(g / 2, 0f, g).roll < 0)
    }

    @Test
    fun `a forty five degree lean reads as forty five`() {
        val component = (g / Math.sqrt(2.0)).toFloat()
        assertEquals(45.0, tiltFrom(0f, component, component).pitch, 1e-4)
    }

    @Test
    fun `a zero vector reads flat rather than NaN`() {
        val tilt = tiltFrom(0f, 0f, 0f)
        assertFalse(tilt.roll.isNaN())
        assertFalse(tilt.pitch.isNaN())
        assertEquals(0.0, tilt.magnitude, 1e-9)
    }

    @Test
    fun `magnitude combines both axes`() {
        val tilt = Tilt(roll = 3.0, pitch = 4.0)
        assertEquals(5.0, tilt.magnitude, 1e-9)
    }

    @Test
    fun `the tolerance decides what counts as level`() {
        assertTrue(Tilt(roll = 0.3, pitch = 0.3).isLevel)
        assertFalse(Tilt(roll = 2.0, pitch = 0.0).isLevel)
    }

    @Test
    fun `smoothing moves toward the new reading without jumping to it`() {
        val previous = Tilt(roll = 0.0, pitch = 0.0)
        val next = Tilt(roll = 10.0, pitch = 20.0)
        val blended = smooth(previous, next, 0.2)
        assertEquals(2.0, blended.roll, 1e-9)
        assertEquals(4.0, blended.pitch, 1e-9)
    }

    @Test
    fun `the first reading is taken as is`() {
        val next = Tilt(roll = 10.0, pitch = 20.0)
        assertEquals(next, smooth(null, next))
    }

    @Test
    fun `smoothing converges on the target`() {
        var tilt = Tilt(0.0, 0.0)
        val target = Tilt(roll = 30.0, pitch = -15.0)
        repeat(200) { tilt = smooth(tilt, target) }
        assertEquals(target.roll, tilt.roll, 1e-3)
        assertEquals(target.pitch, tilt.pitch, 1e-3)
    }

    @Test
    fun `an out of range smoothing factor is clamped`() {
        val previous = Tilt(0.0, 0.0)
        val next = Tilt(10.0, 10.0)
        assertEquals(next, smooth(previous, next, 5.0))
        assertEquals(previous, smooth(previous, next, -1.0))
    }

    @Test
    fun `degrees format to one place without a negative zero`() {
        assertEquals("0.0", formatDegrees(0.0))
        assertEquals("0.0", formatDegrees(-0.02))
        assertEquals("12.3", formatDegrees(12.34))
        assertEquals("-4.6", formatDegrees(-4.56))
        // roundToInt breaks ties toward positive infinity, so -4.55 rounds up
        // to -4.5 rather than away from zero to -4.6.
        assertEquals("-4.5", formatDegrees(-4.55))
        assertEquals("4.6", formatDegrees(4.55))
    }
}
