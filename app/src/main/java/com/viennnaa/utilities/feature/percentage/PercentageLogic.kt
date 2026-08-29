package com.viennnaa.utilities.feature.percentage

import java.util.Locale

/**
 * The three percentage questions people actually ask, kept apart because they
 * take their inputs in different orders and it is easy to answer the wrong one.
 */
enum class PercentageMode { PERCENT_OF, WHAT_PERCENT, CHANGE }

/** [percent]% of [value]. */
fun percentOf(percent: Double, value: Double): Double = value * percent / 100.0

/**
 * [part] is what percent of [whole]. Null when [whole] is zero: everything is an
 * undefined share of nothing, and showing infinity would be worse than nothing.
 */
fun whatPercent(part: Double, whole: Double): Double? =
    if (whole == 0.0) null else part / whole * 100.0

/**
 * Percentage change from [from] to [to]. Null when [from] is zero, since there
 * is no meaningful percentage increase from nothing.
 */
fun percentChange(from: Double, to: Double): Double? =
    if (from == 0.0) null else (to - from) / from * 100.0

/** Reads a typed number, allowing a comma as the decimal mark. */
fun parseNumber(text: String): Double? {
    val trimmed = text.trim().replace(',', '.')
    if (trimmed.isEmpty() || trimmed == "-" || trimmed == ".") return null
    return trimmed.toDoubleOrNull()?.takeIf { it.isFinite() }
}

/** Trims trailing zeros so 25.000000 reads as 25. */
fun formatNumber(value: Double): String {
    if (!value.isFinite()) return "—"
    return String.format("%.4f", value).trimEnd('0').trimEnd('.').ifEmpty { "0" }
}
