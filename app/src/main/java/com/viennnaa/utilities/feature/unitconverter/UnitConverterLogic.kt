package com.viennnaa.utilities.feature.unitconverter

import java.util.Locale
import kotlin.math.abs

/**
 * Unit conversion, done by mapping every unit onto one base unit per category.
 *
 * Each unit is linear against its base: `base = value * factor + offset`. The
 * offset exists for temperature, where the scales do not share a zero, and is
 * zero everywhere else. Handling it in the general form avoids a special case
 * that only temperature would take.
 */

enum class MeasureCategory { LENGTH, MASS, TEMPERATURE, VOLUME }

/**
 * @param symbol shown on the chip. Unit symbols are conventional rather than
 *   translated, so they live here rather than in strings.xml.
 */
data class MeasureUnit(
    val id: String,
    val symbol: String,
    val category: MeasureCategory,
    val factor: Double,
    val offset: Double = 0.0,
)

/** Base units: metre, kilogram, Celsius, litre. */
val Units: List<MeasureUnit> = listOf(
    MeasureUnit("mm", "mm", MeasureCategory.LENGTH, 0.001),
    MeasureUnit("cm", "cm", MeasureCategory.LENGTH, 0.01),
    MeasureUnit("m", "m", MeasureCategory.LENGTH, 1.0),
    MeasureUnit("km", "km", MeasureCategory.LENGTH, 1000.0),
    MeasureUnit("in", "in", MeasureCategory.LENGTH, 0.0254),
    MeasureUnit("ft", "ft", MeasureCategory.LENGTH, 0.3048),
    MeasureUnit("yd", "yd", MeasureCategory.LENGTH, 0.9144),
    MeasureUnit("mi", "mi", MeasureCategory.LENGTH, 1609.344),

    MeasureUnit("g", "g", MeasureCategory.MASS, 0.001),
    MeasureUnit("kg", "kg", MeasureCategory.MASS, 1.0),
    MeasureUnit("t", "t", MeasureCategory.MASS, 1000.0),
    MeasureUnit("oz", "oz", MeasureCategory.MASS, 0.028349523125),
    MeasureUnit("lb", "lb", MeasureCategory.MASS, 0.45359237),
    MeasureUnit("st", "st", MeasureCategory.MASS, 6.35029318),

    MeasureUnit("c", "°C", MeasureCategory.TEMPERATURE, 1.0),
    // base = (F - 32) * 5/9, which rearranges to F * 5/9 - 160/9.
    MeasureUnit("f", "°F", MeasureCategory.TEMPERATURE, 5.0 / 9.0, -160.0 / 9.0),
    MeasureUnit("k", "K", MeasureCategory.TEMPERATURE, 1.0, -273.15),

    MeasureUnit("ml", "ml", MeasureCategory.VOLUME, 0.001),
    MeasureUnit("l", "l", MeasureCategory.VOLUME, 1.0),
    MeasureUnit("floz", "fl oz", MeasureCategory.VOLUME, 0.0295735295625),
    MeasureUnit("pt", "pt", MeasureCategory.VOLUME, 0.473176473),
    MeasureUnit("gal", "gal", MeasureCategory.VOLUME, 3.785411784),
)

fun unitsIn(category: MeasureCategory): List<MeasureUnit> = Units.filter { it.category == category }

fun unitById(id: String): MeasureUnit? = Units.firstOrNull { it.id == id }

/**
 * Converts [value] from one unit to another, or null if the units measure
 * different things — converting kilograms to miles is a bug, not a number.
 */
fun convert(value: Double, from: MeasureUnit, to: MeasureUnit): Double? {
    if (from.category != to.category) return null
    val base = value * from.factor + from.offset
    return (base - to.offset) / to.factor
}

/** Reads a typed amount, allowing a comma as the decimal mark. */
fun parseValue(text: String): Double? {
    val trimmed = text.trim().replace(',', '.')
    if (trimmed.isEmpty() || trimmed == "-" || trimmed == ".") return null
    return trimmed.toDoubleOrNull()?.takeIf { it.isFinite() }
}

/**
 * Formats a result without the noise of full double precision: trailing zeros
 * go, and a whole number loses its decimal point entirely.
 *
 * Formatting is pinned to [Locale.US] on purpose. The trimming below strips a
 * trailing '.', so a locale that formats with ',' would leave "25," on screen.
 * [parseValue] accepts either mark, so input stays forgiving either way.
 */
fun formatValue(value: Double): String {
    if (!value.isFinite()) return "\u2014"
    val magnitude = abs(value)
    if (magnitude >= 1e9 || (magnitude < 1e-4 && value != 0.0)) {
        return String.format(Locale.US, "%.4E", value)
    }
    return String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.').ifEmpty { "0" }
}
