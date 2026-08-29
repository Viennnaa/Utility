package com.viennnaa.utilities.feature.datecalc

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Date arithmetic on [LocalDate], which handles leap years and month lengths so
 * this file does not have to.
 *
 * Every function takes the dates it needs rather than reading the clock, so the
 * results are reproducible and the tests do not drift over time.
 */

/** What the mini app can work out. */
enum class DateMode { BETWEEN, ADD, AGE }

/**
 * Whole days from [from] to [to]. Negative when [to] is earlier, so the sign
 * tells you the direction rather than the caller having to order the arguments.
 */
fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

/** Calendar difference, which is what people mean by "2 years and 3 months". */
fun periodBetween(from: LocalDate, to: LocalDate): Period =
    if (to < from) Period.between(to, from) else Period.between(from, to)

/** [date] shifted by [days], which may be negative. */
fun addDays(date: LocalDate, days: Long): LocalDate = date.plusDays(days)

/**
 * Age on [on], as years, months and days. Null when [birth] is in the future,
 * since a negative age is a mistyped date rather than an answer.
 */
fun ageOn(birth: LocalDate, on: LocalDate): Period? =
    if (birth > on) null else Period.between(birth, on)

/**
 * Reads an ISO date (yyyy-MM-dd), or null if it is not one. Returns null rather
 * than throwing so a half-typed date is just "no answer yet" on screen.
 */
fun parseDate(text: String): LocalDate? = try {
    LocalDate.parse(text.trim())
} catch (e: DateTimeParseException) {
    null
}

/** Whole weeks and leftover days in [days], for a friendlier readout. */
fun weeksAndDays(days: Long): Pair<Long, Long> {
    val magnitude = if (days < 0) -days else days
    return magnitude / 7 to magnitude % 7
}
