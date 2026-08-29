package com.viennnaa.utilities.feature.timezone

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Time zone conversion, built on java.time so the tz database handles the parts
 * that are genuinely hard: daylight saving, half-hour and quarter-hour offsets,
 * and the fact that a zone's offset depends on the date rather than being fixed.
 *
 * Everything is computed from an [Instant] — one moment, rendered in several
 * places — rather than by adding and subtracting offsets, which is what makes
 * DST go wrong.
 */

/** A zone offered on screen, with the city people actually recognise it by. */
data class ZoneEntry(val id: String, val label: String)

/**
 * A spread of major zones. Deliberately identified by region rather than by a
 * fixed offset: "UTC+1" is not a place, and it changes twice a year.
 */
val CommonZones: List<ZoneEntry> = listOf(
    ZoneEntry("Pacific/Auckland", "Auckland"),
    ZoneEntry("Australia/Sydney", "Sydney"),
    ZoneEntry("Asia/Tokyo", "Tokyo"),
    ZoneEntry("Asia/Shanghai", "Shanghai"),
    ZoneEntry("Asia/Singapore", "Singapore"),
    ZoneEntry("Asia/Kolkata", "Kolkata"),
    ZoneEntry("Asia/Dubai", "Dubai"),
    ZoneEntry("Europe/Moscow", "Moscow"),
    ZoneEntry("Africa/Johannesburg", "Johannesburg"),
    ZoneEntry("Europe/Berlin", "Berlin"),
    ZoneEntry("Europe/London", "London"),
    ZoneEntry("UTC", "UTC"),
    ZoneEntry("America/Sao_Paulo", "Sao Paulo"),
    ZoneEntry("America/New_York", "New York"),
    ZoneEntry("America/Chicago", "Chicago"),
    ZoneEntry("America/Denver", "Denver"),
    ZoneEntry("America/Los_Angeles", "Los Angeles"),
    ZoneEntry("Pacific/Honolulu", "Honolulu"),
)

private val CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** Looks up a zone, or null if the id is not one this device knows. */
fun zoneOf(id: String): ZoneId? = try {
    ZoneId.of(id)
} catch (e: Exception) {
    // Covers both a malformed id and one missing from this device's tz data.
    null
}

/**
 * The moment at which it is [time] on [date] in [zone].
 *
 * A local time can be ambiguous or missing entirely around a DST change — the
 * hour that repeats in autumn, the hour that never happens in spring.
 * `atZone` resolves both the way a person would expect: the earlier offset for
 * a repeated time, and pushing forward for a skipped one.
 */
fun instantAt(date: LocalDate, time: LocalTime, zone: ZoneId): Instant =
    date.atTime(time).atZone(zone).toInstant()

fun timeIn(instant: Instant, zone: ZoneId): ZonedDateTime = instant.atZone(zone)

/** The clock reading in [zone], as HH:mm. */
fun formatClock(instant: Instant, zone: ZoneId): String =
    timeIn(instant, zone).format(CLOCK_FORMAT)

/**
 * The zone's offset at that moment, as "+05:30" or "UTC" for zero. Computed at
 * the instant, since a zone's offset is not a constant.
 */
fun offsetLabel(instant: Instant, zone: ZoneId): String {
    val offset = timeIn(instant, zone).offset.id
    // ZoneOffset renders zero as "Z", which reads as a typo next to "+01:00".
    return if (offset == "Z") "UTC" else offset
}

/**
 * Whole days [target] is ahead of [reference] at that moment: -1, 0 or +1, which
 * is what "yesterday" and "tomorrow" on screen mean.
 */
fun dayShift(instant: Instant, reference: ZoneId, target: ZoneId): Int {
    val here = timeIn(instant, reference).toLocalDate()
    val there = timeIn(instant, target).toLocalDate()
    return there.toEpochDay().compareTo(here.toEpochDay())
}

/** Reads a typed clock time like "9:05" or "21:30", or null if it is not one. */
fun parseClock(text: String): LocalTime? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return try {
        LocalTime.parse(if (trimmed.length == 4 && trimmed[1] == ':') "0$trimmed" else trimmed)
    } catch (e: DateTimeParseException) {
        null
    }
}

/** Zones sorted by their offset at that moment, east to west. */
fun zonesByOffset(instant: Instant, zones: List<ZoneEntry> = CommonZones): List<ZoneEntry> =
    zones.sortedByDescending { entry ->
        zoneOf(entry.id)?.let { timeIn(instant, it).offset.totalSeconds } ?: Int.MIN_VALUE
    }
