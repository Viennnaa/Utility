package com.viennnaa.utilities.core.storage

/**
 * Encodes a list of strings as one string, for stores that hold flat values.
 *
 * DataStore Preferences can hold a `Set<String>`, but a set loses the order the
 * user put their options in, so the list is joined into a single value instead.
 *
 * Entries can contain anything, so the separator is escaped rather than assumed
 * to be absent: a backslash becomes `\\` and a newline `\n`, which makes the
 * newline unambiguous as a separator.
 */

private const val SEPARATOR = '\n'
private const val ESCAPE = '\\'

/** Encodes [values] into a single string that [decodeList] reverses exactly. */
fun encodeList(values: List<String>): String =
    values.joinToString(separator = SEPARATOR.toString()) { escape(it) }

/**
 * Reverses [encodeList]. An empty string decodes to an empty list, so a missing
 * stored value and an empty list are the same thing.
 */
fun decodeList(encoded: String): List<String> =
    if (encoded.isEmpty()) emptyList() else encoded.split(SEPARATOR).map { unescape(it) }

private fun escape(value: String): String {
    val out = StringBuilder(value.length)
    for (char in value) {
        when (char) {
            ESCAPE -> out.append(ESCAPE).append(ESCAPE)
            SEPARATOR -> out.append(ESCAPE).append('n')
            else -> out.append(char)
        }
    }
    return out.toString()
}

private fun unescape(value: String): String {
    val out = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char == ESCAPE && index + 1 < value.length) {
            when (val next = value[index + 1]) {
                ESCAPE -> out.append(ESCAPE)
                'n' -> out.append(SEPARATOR)
                // Not a sequence we wrote; keep it as it stands rather than
                // dropping characters out of somebody's saved list.
                else -> out.append(char).append(next)
            }
            index += 2
        } else {
            out.append(char)
            index++
        }
    }
    return out.toString()
}
