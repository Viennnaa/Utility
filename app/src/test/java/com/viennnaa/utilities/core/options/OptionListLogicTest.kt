package com.viennnaa.utilities.core.options

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OptionListLogicTest {

    @Test
    fun `normalizeOption trims and collapses whitespace`() {
        assertEquals("Thai food", normalizeOption("   Thai    food  "))
        assertEquals("Pizza", normalizeOption("\tPizza\n"))
    }

    @Test
    fun `normalizeOption caps the length`() {
        assertEquals(MAX_OPTION_LENGTH, normalizeOption("x".repeat(200)).length)
    }

    @Test
    fun `addOption appends a normalized option`() {
        assertEquals(listOf("Pizza"), addOption(emptyList(), "  Pizza  "))
    }

    @Test
    fun `addOption ignores blank input`() {
        val options = listOf("Pizza")
        assertEquals(options, addOption(options, "   "))
        assertEquals(AddRejection.Blank, rejectionFor(options, "   "))
    }

    @Test
    fun `addOption ignores duplicates regardless of case`() {
        val options = listOf("Pizza")
        assertEquals(options, addOption(options, "pizza"))
        assertEquals(options, addOption(options, "  PIZZA "))
        assertEquals(AddRejection.Duplicate, rejectionFor(options, "pizza"))
    }

    @Test
    fun `addOption refuses to grow past the cap`() {
        val full = (1..MAX_OPTIONS).map { "option $it" }
        assertEquals(full, addOption(full, "one more"))
        assertEquals(AddRejection.Full, rejectionFor(full, "one more"))
    }

    @Test
    fun `rejectionFor accepts a fresh option`() {
        assertNull(rejectionFor(listOf("Pizza"), "Sushi"))
    }

    @Test
    fun `a full list reports Full before Duplicate`() {
        // Blank still wins over Full, since there is nothing to add either way.
        val full = (1..MAX_OPTIONS).map { "option $it" }
        assertEquals(AddRejection.Blank, rejectionFor(full, " "))
        assertEquals(AddRejection.Full, rejectionFor(full, "option 1"))
    }

    @Test
    fun `removeOption drops the right entry`() {
        val options = listOf("a", "b", "c")
        assertEquals(listOf("a", "c"), removeOption(options, 1))
    }

    @Test
    fun `removeOption ignores an out of range index`() {
        val options = listOf("a", "b")
        assertEquals(options, removeOption(options, -1))
        assertEquals(options, removeOption(options, 2))
    }
}
