package com.viennnaa.utilities.core.options

/**
 * Editing rules for a user-built list of short text entries — the options in List
 * Picker, the names in Team Splitter. Shared so every mini app that collects a
 * list treats blanks, duplicates and length the same way.
 */

/** Upper bound on a list, so the chip area stays usable. */
const val MAX_OPTIONS = 50

/** Longest a single entry can be, to keep it readable in a chip. */
const val MAX_OPTION_LENGTH = 60

/** Why an entry could not be added. */
enum class AddRejection {
    /** Nothing but whitespace was typed. */
    Blank,

    /** An entry matching this one, ignoring case, is already listed. */
    Duplicate,

    /** The list already holds [MAX_OPTIONS] entries. */
    Full,
}

/**
 * Trims [raw], collapses runs of whitespace, and caps the length. Applied before
 * anything else so that "  Pizza  " and "Pizza" are the same entry.
 */
fun normalizeOption(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ").take(MAX_OPTION_LENGTH)

/**
 * Checks whether [raw] can join [options], returning the reason if not, or null
 * when the entry is fine to add.
 */
fun rejectionFor(options: List<String>, raw: String): AddRejection? {
    val normalized = normalizeOption(raw)
    return when {
        normalized.isEmpty() -> AddRejection.Blank
        options.size >= MAX_OPTIONS -> AddRejection.Full
        options.any { it.equals(normalized, ignoreCase = true) } -> AddRejection.Duplicate
        else -> null
    }
}

/**
 * Appends [raw] to [options]. Returns the list unchanged if the entry is blank, a
 * duplicate, or the list is already full — check with [rejectionFor] first if you
 * need to tell the user why.
 */
fun addOption(options: List<String>, raw: String): List<String> =
    if (rejectionFor(options, raw) != null) options else options + normalizeOption(raw)

/** Drops the entry at [index], or returns the list unchanged if there is none. */
fun removeOption(options: List<String>, index: Int): List<String> =
    if (index !in options.indices) options else options.filterIndexed { i, _ -> i != index }
