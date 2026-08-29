package com.viennnaa.utilities.feature.texttools

import org.junit.Assert.assertEquals
import org.junit.Test

class TextToolsLogicTest {

    private fun transform(text: String, t: TextTransform) = applyTransform(text, t)

    @Test
    fun `upper and lower case`() {
        assertEquals("HELLO THERE", transform("Hello There", TextTransform.UPPERCASE))
        assertEquals("hello there", transform("Hello There", TextTransform.LOWERCASE))
    }

    @Test
    fun `title case capitalises each word`() {
        assertEquals("Hello There World", transform("hello there world", TextTransform.TITLE_CASE))
        assertEquals("Hello There", transform("HELLO THERE", TextTransform.TITLE_CASE))
    }

    @Test
    fun `title case keeps the original spacing`() {
        assertEquals("A  B\nC", transform("a  b\nc", TextTransform.TITLE_CASE))
    }

    @Test
    fun `sentence case capitalises after terminators`() {
        assertEquals(
            "Hello there. How are you? Fine! Good.",
            transform("hello there. how are you? fine! good.", TextTransform.SENTENCE_CASE),
        )
    }

    @Test
    fun `sentence case starts a new sentence on a new line`() {
        assertEquals("One\nTwo", transform("one\ntwo", TextTransform.SENTENCE_CASE))
    }

    @Test
    fun `reverse turns the text around`() {
        assertEquals("cba", transform("abc", TextTransform.REVERSE))
    }

    @Test
    fun `reverse keeps multi code point characters whole`() {
        // A naive char reverse would split the surrogate pair into two broken halves.
        assertEquals("🎲b", transform("b🎲", TextTransform.REVERSE))
    }

    @Test
    fun `collapse spaces squeezes runs of whitespace`() {
        assertEquals("a b c", transform("  a   b \t c  ", TextTransform.COLLAPSE_SPACES))
    }

    @Test
    fun `remove line breaks joins lines with a single space`() {
        assertEquals("one two three", transform("one\ntwo\r\nthree", TextTransform.REMOVE_LINE_BREAKS))
    }

    @Test
    fun `every transform handles empty text`() {
        for (t in TextTransform.entries) {
            assertEquals("$t", "", transform("", t))
        }
    }

    @Test
    fun `stats count characters words and lines`() {
        val stats = textStats("hello there\nsecond line")
        assertEquals(23, stats.characters)
        assertEquals(20, stats.charactersNoSpaces)
        assertEquals(4, stats.words)
        assertEquals(2, stats.lines)
    }

    @Test
    fun `empty text counts as nothing rather than one line`() {
        val stats = textStats("")
        assertEquals(0, stats.characters)
        assertEquals(0, stats.words)
        assertEquals(0, stats.lines)
    }

    @Test
    fun `blank text has no words`() {
        assertEquals(0, textStats("    ").words)
        assertEquals(4, textStats("    ").characters)
    }

    @Test
    fun `words are separated by any run of whitespace`() {
        assertEquals(3, textStats("  one   two \t three  ").words)
    }
}
