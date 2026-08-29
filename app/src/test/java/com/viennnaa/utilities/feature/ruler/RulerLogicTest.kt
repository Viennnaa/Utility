package com.viennnaa.utilities.feature.ruler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulerLogicTest {

    @Test
    fun `pixels per millimetre follows the dpi`() {
        // 25.4 dpi is exactly one pixel per millimetre.
        assertEquals(1f, pixelsPerMillimetre(25.4f)!!, 1e-4f)
        assertEquals(160f / 25.4f, pixelsPerMillimetre(160f)!!, 1e-4f)
    }

    @Test
    fun `calibration scales the result`() {
        val base = pixelsPerMillimetre(400f)!!
        assertEquals(base * 1.1f, pixelsPerMillimetre(400f, 1.1f)!!, 1e-4f)
        assertEquals(base * 0.9f, pixelsPerMillimetre(400f, 0.9f)!!, 1e-4f)
    }

    @Test
    fun `calibration outside the range is clamped`() {
        assertEquals(MIN_CALIBRATION, clampCalibration(0.1f), 1e-6f)
        assertEquals(MAX_CALIBRATION, clampCalibration(9f), 1e-6f)
        assertEquals(1.25f, clampCalibration(1.25f), 1e-6f)
        // The clamp applies inside the conversion too.
        assertEquals(pixelsPerMillimetre(400f, MAX_CALIBRATION), pixelsPerMillimetre(400f, 99f))
    }

    @Test
    fun `a nonsense dpi has no answer rather than a division by zero`() {
        assertNull(pixelsPerMillimetre(0f))
        assertNull(pixelsPerMillimetre(-160f))
        assertNull(pixelsPerMillimetre(Float.NaN))
        assertNull(pixelsPerInch(0f))
    }

    @Test
    fun `an inch is 25 point 4 millimetres of pixels`() {
        val perMm = pixelsPerMillimetre(400f)!!
        val perInch = pixelsPerInch(400f)!!
        assertEquals(perInch, perMm * MILLIMETRES_PER_INCH, 1e-3f)
    }

    @Test
    fun `millimetre ticks are long every ten and medium every five`() {
        assertEquals(1f, tickFraction(0, RulerUnit.MILLIMETRES), 1e-6f)
        assertEquals(1f, tickFraction(20, RulerUnit.MILLIMETRES), 1e-6f)
        assertEquals(0.6f, tickFraction(5, RulerUnit.MILLIMETRES), 1e-6f)
        assertEquals(0.35f, tickFraction(3, RulerUnit.MILLIMETRES), 1e-6f)
    }

    @Test
    fun `inch ticks step down through halves and eighths`() {
        assertEquals(1f, tickFraction(0, RulerUnit.INCHES), 1e-6f)
        assertEquals(1f, tickFraction(8, RulerUnit.INCHES), 1e-6f)
        assertEquals(0.6f, tickFraction(4, RulerUnit.INCHES), 1e-6f)
        assertEquals(0.45f, tickFraction(2, RulerUnit.INCHES), 1e-6f)
        assertEquals(0.3f, tickFraction(1, RulerUnit.INCHES), 1e-6f)
    }

    @Test
    fun `only whole units carry a label`() {
        assertTrue(tickIsLabelled(0, RulerUnit.MILLIMETRES))
        assertTrue(tickIsLabelled(30, RulerUnit.MILLIMETRES))
        assertTrue(!tickIsLabelled(7, RulerUnit.MILLIMETRES))
        assertTrue(tickIsLabelled(16, RulerUnit.INCHES))
        assertTrue(!tickIsLabelled(3, RulerUnit.INCHES))
    }

    @Test
    fun `labels count whole units`() {
        assertEquals(3, tickLabel(30, RulerUnit.MILLIMETRES))
        assertEquals(2, tickLabel(16, RulerUnit.INCHES))
        assertEquals(0, tickLabel(0, RulerUnit.INCHES))
    }

    @Test
    fun `tick count fills the available length`() {
        assertEquals(11, tickCount(100f, 10f))
        assertEquals(1, tickCount(5f, 10f))
    }

    @Test
    fun `tick count refuses nonsense rather than looping forever`() {
        assertEquals(0, tickCount(100f, 0f))
        assertEquals(0, tickCount(100f, -1f))
        assertEquals(0, tickCount(0f, 10f))
        assertEquals(0, tickCount(100f, Float.NaN))
    }

    @Test
    fun `calibration reads as a percentage`() {
        assertEquals(100, calibrationPercent(1f))
        assertEquals(110, calibrationPercent(1.1f))
        assertEquals(50, calibrationPercent(0.2f))
    }
}
