package com.viennnaa.utilities.feature.compass

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Heading maths for the compass.
 *
 * Headings are circular, which is the whole difficulty: 359 degrees and 1 degree
 * are two degrees apart, not 358, and smoothing that ignores the wrap makes the
 * needle spin the long way round.
 */

/** The eight points shown under the heading. */
enum class Cardinal(val label: String) {
    NORTH("N"),
    NORTHEAST("NE"),
    EAST("E"),
    SOUTHEAST("SE"),
    SOUTH("S"),
    SOUTHWEST("SW"),
    WEST("W"),
    NORTHWEST("NW"),
}

/** Brings any angle into 0 until 360. */
fun normalizeDegrees(degrees: Double): Double {
    val wrapped = degrees % 360.0
    return if (wrapped < 0) wrapped + 360.0 else wrapped
}

/**
 * Signed difference from [from] to [to], always the short way round: the result
 * is in -180 until 180.
 */
fun angleDelta(from: Double, to: Double): Double {
    val raw = normalizeDegrees(to) - normalizeDegrees(from)
    return when {
        raw > 180.0 -> raw - 360.0
        raw <= -180.0 -> raw + 360.0
        else -> raw
    }
}

/**
 * Blends a new heading into the previous one, taking the short way round so the
 * needle never spins the long way across north.
 */
fun smoothHeading(previous: Double?, next: Double, factor: Double = 0.15): Double {
    if (previous == null) return normalizeDegrees(next)
    val weight = factor.coerceIn(0.0, 1.0)
    return normalizeDegrees(previous + angleDelta(previous, next) * weight)
}

/** Nearest of the eight points to [degrees]. */
fun cardinalFor(degrees: Double): Cardinal {
    val normalized = normalizeDegrees(degrees)
    // Each point covers 45 degrees, centred on its own bearing.
    val index = ((normalized + 22.5) / 45.0).toInt() % Cardinal.entries.size
    return Cardinal.entries[index]
}

/** Whole degrees, wrapped, for the readout. */
fun formatHeading(degrees: Double): Int = normalizeDegrees(degrees).roundToInt() % 360

/** True when the device is pointing close enough to north to say so. */
fun isFacingNorth(degrees: Double, tolerance: Double = 2.0): Boolean =
    abs(angleDelta(0.0, degrees)) <= tolerance
