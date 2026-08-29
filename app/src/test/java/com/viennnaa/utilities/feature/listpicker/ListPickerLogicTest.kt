package com.viennnaa.utilities.feature.listpicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ListPickerLogicTest {

    @Test
    fun `pickIndex returns null for an empty list`() {
        assertNull(pickIndex(emptyList(), Random(seed = 1)))
    }

    @Test
    fun `pickIndex stays in bounds`() {
        val options = listOf("a", "b", "c", "d")
        val random = Random(seed = 5)
        repeat(500) {
            val index = pickIndex(options, random)!!
            assertTrue("$index outside the list", index in options.indices)
        }
    }

    @Test
    fun `pickIndex can return every option`() {
        val options = listOf("a", "b", "c")
        val random = Random(seed = 9)
        val seen = (1..500).mapNotNull { pickIndex(options, random) }.toSet()
        assertEquals(options.indices.toSet(), seen)
    }

    @Test
    fun `pickIndex on a single option always returns it`() {
        assertEquals(0, pickIndex(listOf("only"), Random(seed = 3)))
    }

    @Test
    fun `recordPick puts the newest pick first and caps history`() {
        var history = emptyList<String>()
        repeat(HISTORY_SIZE + 4) { history = recordPick(history, "pick $it") }
        assertEquals(HISTORY_SIZE, history.size)
        assertEquals("pick ${HISTORY_SIZE + 3}", history.first())
    }
}
