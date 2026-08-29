package com.viennnaa.utilities.feature.ruler

import kotlin.math.roundToInt

/**
 * Screen measurement.
 *
 * A phone reports its physical pixel density, but the reported figure is often
 * a rounded or plainly wrong value from the manufacturer, so a real ruler needs
 * a calibration factor the user can nudge until the on-screen scale matches a
 * known object.
 */

const val MIN_CALIBRATION = 0.5f
const val MAX_CALIBRATION = 2.0f
const val CALIBRATION_STEP = 0.01f

const val MILLIMETRES_PER_INCH = 25.4f

/** Which scale the ruler draws. */
enum class RulerUnit { MILLIMETRES, INCHES }

fun clampCalibration(value: Float): Float = value.coerceIn(MIN_CALIBRATION, MAX_CALIBRATION)

/**
 * Physical pixels in one millimetre, given the screen's [dpi] and the user's
 * [calibration]. Returns null for a nonsense dpi rather than dividing by zero.
 */
fun pixelsPerMillimetre(dpi: Float, calibration: Float = 1f): Float? {
    if (dpi <= 0f || !dpi.isFinite()) return null
    return dpi / MILLIMETRES_PER_INCH * clampCalibration(calibration)
}

/** Physical pixels in one inch, after calibration. */
fun pixelsPerInch(dpi: Float, calibration: Float = 1f): Float? {
    if (dpi <= 0f || !dpi.isFinite()) return null
    return dpi * clampCalibration(calibration)
}

/**
 * How long a tick at [index] should be, as a fraction of the full tick length.
 *
 * Millimetres get a long mark every 10 and a medium every 5; inches get long at
 * the whole inch, medium at the half, and short at the eighths.
 */
fun tickFraction(index: Int, unit: RulerUnit): Float = when (unit) {
    RulerUnit.MILLIMETRES -> when {
        index % 10 == 0 -> 1f
        index % 5 == 0 -> 0.6f
        else -> 0.35f
    }

    RulerUnit.INCHES -> when {
        index % 8 == 0 -> 1f
        index % 4 == 0 -> 0.6f
        index % 2 == 0 -> 0.45f
        else -> 0.3f
    }
}

/** True when a tick should carry a printed number. */
fun tickIsLabelled(index: Int, unit: RulerUnit): Boolean = when (unit) {
    RulerUnit.MILLIMETRES -> index % 10 == 0
    RulerUnit.INCHES -> index % 8 == 0
}

/** The number printed at [index], in whole units. */
fun tickLabel(index: Int, unit: RulerUnit): Int = when (unit) {
    RulerUnit.MILLIMETRES -> index / 10
    RulerUnit.INCHES -> index / 8
}

/** How many ticks fit in [lengthPixels]. */
fun tickCount(lengthPixels: Float, pixelsPerTick: Float): Int {
    if (pixelsPerTick <= 0f || !pixelsPerTick.isFinite() || lengthPixels <= 0f) return 0
    return (lengthPixels / pixelsPerTick).toInt() + 1
}

/** Calibration as the percentage shown next to the nudge buttons. */
fun calibrationPercent(calibration: Float): Int = (clampCalibration(calibration) * 100).roundToInt()
