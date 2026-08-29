package com.viennnaa.utilities.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class ListEncodingTest {

    private fun assertRoundTrips(values: List<String>) {
        assertEquals(values, decodeList(encodeList(values)))
    }

    @Test
    fun `an empty list round trips`() {
        assertRoundTrips(emptyList())
        assertEquals("", encodeList(emptyList()))
    }

    @Test
    fun `ordinary entries round trip in order`() {
        assertRoundTrips(listOf("Pizza", "Sushi", "Thai food"))
    }

    @Test
    fun `order is preserved rather than sorted`() {
        val values = listOf("Zoe", "Ana", "Mia")
        assertEquals(values, decodeList(encodeList(values)))
    }

    @Test
    fun `a single entry round trips`() {
        assertRoundTrips(listOf("only one"))
    }

    @Test
    fun `an empty entry round trips`() {
        assertRoundTrips(listOf("", "after an empty one"))
        assertRoundTrips(listOf("before an empty one", ""))
    }

    @Test
    fun `entries containing the separator round trip`() {
        assertRoundTrips(listOf("two\nlines", "normal"))
    }

    @Test
    fun `entries containing the escape character round trip`() {
        assertRoundTrips(listOf("back\\slash", "normal"))
        assertRoundTrips(listOf("trailing\\", "normal"))
        assertRoundTrips(listOf("\\", "\\\\"))
    }

    @Test
    fun `an escape immediately before a separator round trips`() {
        // The nasty case: a naive split would tear this entry in half.
        assertRoundTrips(listOf("ends with escape\\", "next"))
        assertRoundTrips(listOf("escape then newline\\\n", "next"))
    }

    @Test
    fun `emoji and other multi character text round trips`() {
        assertRoundTrips(listOf("🎲 dice", "café", "中文"))
    }

    @Test
    fun `unknown escape sequences are left alone rather than dropped`() {
        // Not something encodeList produces, but decoding must not lose text.
        assertEquals(listOf("\\q"), decodeList("\\q"))
    }

    @Test
    fun `a long list round trips`() {
        assertRoundTrips((1..50).map { "entry $it" })
    }
}
