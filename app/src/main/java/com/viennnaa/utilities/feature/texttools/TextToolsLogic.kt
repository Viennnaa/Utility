package com.viennnaa.utilities.feature.texttools

import java.util.Locale

/** The transformations offered, in the order they appear on screen. */
enum class TextTransform {
    UPPERCASE,
    LOWERCASE,
    TITLE_CASE,
    SENTENCE_CASE,
    REVERSE,
    COLLAPSE_SPACES,
    REMOVE_LINE_BREAKS,
}

/** Counts shown under the text as it is typed. */
data class TextStats(
    val characters: Int,
    val charactersNoSpaces: Int,
    val words: Int,
    val lines: Int,
)

private val WHITESPACE = Regex("\\s+")

/**
 * Counts for [text]. An empty string has zero lines rather than one: the box is
 * empty, and reporting "1 line" for nothing typed reads as a bug.
 */
fun textStats(text: String): TextStats = TextStats(
    characters = text.length,
    charactersNoSpaces = text.count { !it.isWhitespace() },
    words = if (text.isBlank()) 0 else text.trim().split(WHITESPACE).size,
    lines = if (text.isEmpty()) 0 else text.count { it == '\n' } + 1,
)

/** Applies [transform] to [text]. */
fun applyTransform(text: String, transform: TextTransform): String = when (transform) {
    TextTransform.UPPERCASE -> text.uppercase(Locale.getDefault())
    TextTransform.LOWERCASE -> text.lowercase(Locale.getDefault())
    TextTransform.TITLE_CASE -> titleCase(text)
    TextTransform.SENTENCE_CASE -> sentenceCase(text)
    // Reversed by code point, so an accented letter or an emoji does not come
    // back as two broken halves the way a plain char reverse would leave it.
    TextTransform.REVERSE -> reverseByCodePoint(text)
    TextTransform.COLLAPSE_SPACES -> text.trim().replace(WHITESPACE, " ")
    TextTransform.REMOVE_LINE_BREAKS -> text.replace(Regex("\\r?\\n"), " ").replace(WHITESPACE, " ").trim()
}

/** Capitalises the first letter of every whitespace-separated run. */
private fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    var atWordStart = true
    for (char in text) {
        if (char.isWhitespace()) {
            atWordStart = true
            out.append(char)
        } else if (atWordStart) {
            atWordStart = false
            out.append(char.uppercaseChar())
        } else {
            out.append(char.lowercaseChar())
        }
    }
    return out.toString()
}

/** Lowercases everything, then capitalises the first letter after . ! or ? */
private fun sentenceCase(text: String): String {
    val lowered = text.lowercase(Locale.getDefault())
    val out = StringBuilder(lowered.length)
    var startOfSentence = true
    for (char in lowered) {
        when {
            startOfSentence && char.isLetter() -> {
                out.append(char.uppercaseChar())
                startOfSentence = false
            }

            char == '.' || char == '!' || char == '?' || char == '\n' -> {
                out.append(char)
                startOfSentence = true
            }

            else -> out.append(char)
        }
    }
    return out.toString()
}

private fun reverseByCodePoint(text: String): String {
    val points = text.codePoints().toArray()
    val out = StringBuilder(text.length)
    for (index in points.indices.reversed()) {
        out.appendCodePoint(points[index])
    }
    return out.toString()
}
