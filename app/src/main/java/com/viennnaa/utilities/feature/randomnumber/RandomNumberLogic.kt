package com.viennnaa.utilities.feature.randomnumber

import kotlin.random.Random

/**
 * Bounds accepted for a custom range. Keeping both ends inside a billion means
 * `last + 1` in [randomIn] can never overflow, whatever the user types.
 */
const val RANGE_LIMIT = 1_000_000_000

/** How many past results the mini app keeps on screen. */
const val HISTORY_SIZE = 12

/** A range the user can pick with a single tap. */
data class RangePreset(val min: Int, val max: Int) {
    val range: IntRange get() = min..max
}

val DefaultPresets: List<RangePreset> = listOf(
    RangePreset(1, 6),
    RangePreset(1, 10),
    RangePreset(1, 20),
    RangePreset(1, 100),
)

/** Outcome of turning the two custom-range text fields into a usable range. */
sealed interface RangeValidation {
    data class Valid(val range: IntRange) : RangeValidation

    /** Why a typed range cannot be used. The UI maps each case to a message. */
    enum class Problem : RangeValidation {
        /** One of the fields is empty or not a whole number. */
        NotANumber,

        /** A bound is beyond ±[RANGE_LIMIT]. */
        OutOfBounds,

        /** The low bound is above the high bound. */
        MinAboveMax,
    }
}

/**
 * Validates the custom range typed on screen.
 *
 * A single-number range (`5` to `5`) is allowed — it always returns that number,
 * which is degenerate but not an error.
 */
fun validateRange(minText: String, maxText: String): RangeValidation {
    // Parsed as Long so that a bound too big for an Int reports as out of bounds
    // rather than as "not a number".
    val min = minText.trim().toLongOrNull() ?: return RangeValidation.Problem.NotANumber
    val max = maxText.trim().toLongOrNull() ?: return RangeValidation.Problem.NotANumber
    if (min < -RANGE_LIMIT || min > RANGE_LIMIT) return RangeValidation.Problem.OutOfBounds
    if (max < -RANGE_LIMIT || max > RANGE_LIMIT) return RangeValidation.Problem.OutOfBounds
    if (min > max) return RangeValidation.Problem.MinAboveMax
    return RangeValidation.Valid(min.toInt()..max.toInt())
}

/**
 * Draws a number from [range], both ends included, with every value equally likely.
 */
fun randomIn(range: IntRange, random: Random = Random): Int =
    random.nextInt(range.first, range.last + 1)

/**
 * Adds [value] to the front of [history], keeping it at [HISTORY_SIZE] entries.
 */
fun recordResult(history: List<Int>, value: Int): List<Int> =
    (listOf(value) + history).take(HISTORY_SIZE)

/**
 * Strips anything that cannot be part of a whole number as the user types, so the
 * field never holds text the keyboard could otherwise sneak in (a decimal point, a
 * pasted word). A leading `-` is kept so negative bounds stay typeable.
 */
fun sanitizeBoundInput(input: String): String {
    val negative = input.startsWith('-')
    val digits = input.filter { it.isDigit() }.take(10)
    return if (negative) "-$digits" else digits
}
