package com.viennnaa.utilities.feature.unitconverter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class UnitConverterLogicTest {

    private fun unit(id: String) = unitById(id)!!

    @Test
    fun `length conversions match the published factors`() {
        assertEquals(1000.0, convert(1.0, unit("km"), unit("m"))!!, 1e-9)
        assertEquals(2.54, convert(1.0, unit("in"), unit("cm"))!!, 1e-9)
        assertEquals(1.609344, convert(1.0, unit("mi"), unit("km"))!!, 1e-9)
        assertEquals(3.0, convert(1.0, unit("yd"), unit("ft"))!!, 1e-9)
    }

    @Test
    fun `mass conversions match the published factors`() {
        assertEquals(1000.0, convert(1.0, unit("kg"), unit("g"))!!, 1e-9)
        assertEquals(16.0, convert(1.0, unit("lb"), unit("oz"))!!, 1e-9)
        assertEquals(14.0, convert(1.0, unit("st"), unit("lb"))!!, 1e-6)
    }

    @Test
    fun `temperature handles the offset scales`() {
        assertEquals(32.0, convert(0.0, unit("c"), unit("f"))!!, 1e-9)
        assertEquals(212.0, convert(100.0, unit("c"), unit("f"))!!, 1e-9)
        assertEquals(100.0, convert(212.0, unit("f"), unit("c"))!!, 1e-9)
        assertEquals(273.15, convert(0.0, unit("c"), unit("k"))!!, 1e-9)
        assertEquals(0.0, convert(273.15, unit("k"), unit("c"))!!, 1e-9)
        // The scales cross at -40.
        assertEquals(-40.0, convert(-40.0, unit("c"), unit("f"))!!, 1e-9)
    }

    @Test
    fun `volume conversions match the published factors`() {
        assertEquals(1000.0, convert(1.0, unit("l"), unit("ml"))!!, 1e-9)
        assertEquals(8.0, convert(1.0, unit("gal"), unit("pt"))!!, 1e-6)
    }

    @Test
    fun `converting to the same unit changes nothing`() {
        for (u in Units) {
            assertEquals("${u.id} was not identity", 42.5, convert(42.5, u, u)!!, 1e-9)
        }
    }

    @Test
    fun `every conversion round trips`() {
        for (from in Units) {
            for (to in unitsIn(from.category)) {
                val there = convert(7.25, from, to)!!
                val back = convert(there, to, from)!!
                assertEquals("${from.id} to ${to.id}", 7.25, back, 1e-6)
            }
        }
    }

    @Test
    fun `mixing categories is refused rather than answered`() {
        assertNull(convert(1.0, unit("kg"), unit("mi")))
        assertNull(convert(1.0, unit("c"), unit("l")))
    }

    @Test
    fun `every unit is reachable by id and grouped in one category`() {
        for (u in Units) {
            assertNotNull(unitById(u.id))
            assertTrue(u in unitsIn(u.category))
        }
        assertNull(unitById("nope"))
    }

    @Test
    fun `every category offers at least two units`() {
        for (category in MeasureCategory.entries) {
            assertTrue("$category", unitsIn(category).size >= 2)
        }
    }

    @Test
    fun `unit ids are unique`() {
        assertEquals(Units.size, Units.map { it.id }.toSet().size)
    }

    @Test
    fun `parseValue accepts the forms people type`() {
        assertEquals(12.5, parseValue("12.5")!!, 1e-9)
        assertEquals(12.5, parseValue("12,5")!!, 1e-9)
        assertEquals(-3.0, parseValue(" -3 ")!!, 1e-9)
        assertEquals(0.0, parseValue("0")!!, 1e-9)
    }

    @Test
    fun `parseValue rejects junk and partial input`() {
        assertNull(parseValue(""))
        assertNull(parseValue("   "))
        assertNull(parseValue("-"))
        assertNull(parseValue("."))
        assertNull(parseValue("abc"))
    }

    @Test
    fun `formatValue trims noise without leaving a stray separator`() {
        assertEquals("25", formatValue(25.0))
        assertEquals("2.5", formatValue(2.5))
        assertEquals("0", formatValue(0.0))
        assertEquals("-40", formatValue(-40.0))
        assertTrue(formatValue(1.0 / 3.0).startsWith("0.3333"))
    }

    @Test
    fun `formatValue is unaffected by a comma-decimal default locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("25", formatValue(25.0))
            assertEquals("2.5", formatValue(2.5))
            assertEquals("-40", formatValue(-40.0))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `formatValue falls back to scientific notation at the extremes`() {
        assertTrue(formatValue(1e12).contains("E"))
        assertTrue(formatValue(1e-9).contains("E"))
    }
}
