package com.viennnaa.utilities.feature.listpicker

import kotlin.random.Random

/** Upper bound on the option list, so the chip area stays usable. */
const val MAX_OPTIONS = 50

/** Longest a single option can be, to keep it readable in a chip. */
const val MAX_OPTION_LENGTH = 60

/** How many past picks the mini app keeps on screen. */
const val HISTORY_SIZE = 12

/** Why an option could not be added. */
enum class AddRejection {
    /** Nothing but whitespace was typed. */
    Blank,

    /** An option matching this one, ignoring case, is already listed. */
    Duplicate,

    /** The list already holds [MAX_OPTIONS] entries. */
    Full,
}

/**
 * Trims [raw], collapses runs of whitespace, and caps the length. Applied before
 * anything else so that "  Pizza  " and "Pizza" are the same option.
 */
fun normalizeOption(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ").take(MAX_OPTION_LENGTH)

/**
 * Checks whether [raw] can join [options], returning the reason if not.
 * Returns null when the option is fine to add.
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
 * Appends [raw] to [options]. Returns the list unchanged if the option is blank, a
 * duplicate, or the list is already full — check with [rejectionFor] first if you
 * need to tell the user why.
 */
fun addOption(options: List<String>, raw: String): List<String> =
    if (rejectionFor(options, raw) != null) options else options + normalizeOption(raw)

/** Drops the option at [index], or returns the list unchanged if there is none. */
fun removeOption(options: List<String>, index: Int): List<String> =
    if (index !in options.indices) options else options.filterIndexed { i, _ -> i != index }

/**
 * Picks one option's index, every option equally likely. Null when there is
 * nothing to pick from.
 */
fun pickIndex(options: List<String>, random: Random = Random): Int? =
    if (options.isEmpty()) null else random.nextInt(options.size)

/**
 * Adds [pick] to the front of [history], keeping it at [HISTORY_SIZE] entries.
 */
fun recordPick(history: List<String>, pick: String): List<String> =
    (listOf(pick) + history).take(HISTORY_SIZE)
