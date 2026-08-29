package com.viennnaa.utilities.feature.level

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Turning a gravity reading into the two angles a spirit level shows.
 *
 * The sensor gives an acceleration vector; with the phone still, that vector
 * points down, so its direction relative to the screen is the tilt.
 */

/** How far off level still counts as level, in degrees. */
const val LEVEL_TOLERANCE_DEGREES = 0.5

/** Tilt of the device, in degrees. */
data class Tilt(
    /** Rotation about the long axis: positive when the right edge is raised. */
    val roll: Double,
    /** Rotation about the short axis: positive when the top edge is raised. */
    val pitch: Double,
) {
    /** Total tilt away from flat, whichever direction it leans. */
    val magnitude: Double get() = hypot(roll, pitch)

    val isLevel: Boolean get() = magnitude <= LEVEL_TOLERANCE_DEGREES
}

/**
 * Converts a gravity vector into [Tilt].
 *
 * A zero vector — which a sensor should never report, but a broken one might —
 * gives a flat reading rather than a NaN, since atan2(0, 0) is defined as 0.
 */
fun tiltFrom(x: Float, y: Float, z: Float): Tilt {
    val roll = Math.toDegrees(atan2(x.toDouble(), hypot(y.toDouble(), z.toDouble())))
    val pitch = Math.toDegrees(atan2(y.toDouble(), hypot(x.toDouble(), z.toDouble())))
    return Tilt(roll = -roll, pitch = pitch)
}

/**
 * Blends a new reading into the previous one so the bubble settles instead of
 * jittering. [factor] is how much of the new reading to take, 0 to 1.
 */
fun smooth(previous: Tilt?, next: Tilt, factor: Double = 0.2): Tilt {
    if (previous == null) return next
    val weight = factor.coerceIn(0.0, 1.0)
    return Tilt(
        roll = previous.roll + (next.roll - previous.roll) * weight,
        pitch = previous.pitch + (next.pitch - previous.pitch) * weight,
    )
}

/** One decimal place, which is as much as a phone sensor can honestly claim. */
fun formatDegrees(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    // Round -0.04 to "0.0" rather than "-0.0".
    val cleaned = if (abs(rounded) < 0.05) 0.0 else rounded
    return String.format(java.util.Locale.US, "%.1f", cleaned)
}
