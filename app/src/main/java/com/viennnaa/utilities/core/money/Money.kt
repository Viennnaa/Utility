package com.viennnaa.utilities.core.money

import java.util.Locale

/**
 * Money handling shared by the mini apps that deal in prices.
 *
 * Money never touches a Double: 0.1 + 0.2 is not 0.3 in binary floating point,
 * and splitting a bill or taking a percentage off a price is exactly where that
 * surfaces. Amounts are a Long count of whole cents throughout, and percentages
 * are basis points, so every calculation stays in integer arithmetic.
 */

/** Largest amount accepted, in cents, so the arithmetic stays far from overflow. */
const val MAX_AMOUNT_CENTS = 100_000_000L

/** One percent, in basis points. A rate of 17.5% is 1750. */
const val BASIS_POINTS_PER_PERCENT = 100

/** Basis points in a whole, used as the divisor when applying a rate. */
const val BASIS_POINTS_WHOLE = 10_000

/**
 * Reads a typed amount like "12", "12.5" or "12.34" into whole cents, or null if
 * it is not a usable amount.
 *
 * More than two decimal places is rejected rather than silently truncated: it
 * usually means a typo, and quietly dropping a digit would change the amount.
 */
fun parseAmountCents(text: String): Long? {
    val trimmed = text.trim().replace(',', '.')
    if (trimmed.isEmpty() || trimmed.count { it == '.' } > 1) return null
    val (whole, fraction) = when (val dot = trimmed.indexOf('.')) {
        -1 -> trimmed to ""
        else -> trimmed.substring(0, dot) to trimmed.substring(dot + 1)
    }
    if (fraction.length > 2) return null
    if (whole.isEmpty() && fraction.isEmpty()) return null
    if (!whole.all { it.isDigit() } || !fraction.all { it.isDigit() }) return null

    val wholeValue = if (whole.isEmpty()) 0L else whole.toLongOrNull() ?: return null
    val cents = fraction.padEnd(2, '0').toLongOrNull() ?: return null
    if (wholeValue > MAX_AMOUNT_CENTS / 100) return null
    return wholeValue * 100 + cents
}

/**
 * Reads a typed rate like "20" or "17.5" into basis points, or null if it is not
 * a usable rate. At most one decimal place, which is as fine as tax and discount
 * rates get in practice.
 */
fun parsePercentBasisPoints(text: String, maxPercent: Int = 100): Int? {
    val trimmed = text.trim().replace(',', '.')
    if (trimmed.isEmpty() || trimmed.count { it == '.' } > 1) return null
    val (whole, fraction) = when (val dot = trimmed.indexOf('.')) {
        -1 -> trimmed to ""
        else -> trimmed.substring(0, dot) to trimmed.substring(dot + 1)
    }
    if (fraction.length > 1) return null
    if (whole.isEmpty() && fraction.isEmpty()) return null
    if (!whole.all { it.isDigit() } || !fraction.all { it.isDigit() }) return null

    val wholeValue = if (whole.isEmpty()) 0 else whole.toIntOrNull() ?: return null
    if (wholeValue > maxPercent) return null
    val tenths = if (fraction.isEmpty()) 0 else fraction.toIntOrNull() ?: return null
    val basisPoints = wholeValue * BASIS_POINTS_PER_PERCENT + tenths * 10
    return if (basisPoints > maxPercent * BASIS_POINTS_PER_PERCENT) null else basisPoints
}

/** Renders whole cents back as a plain decimal string, always with two places. */
fun formatCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = if (cents < 0) -cents else cents
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

/**
 * Renders basis points as a percentage, dropping a trailing ".0" so a whole rate
 * reads as "20%" rather than "20.0%".
 */
fun formatBasisPoints(basisPoints: Int): String {
    val whole = basisPoints / BASIS_POINTS_PER_PERCENT
    val tenths = (basisPoints % BASIS_POINTS_PER_PERCENT) / 10
    return if (tenths == 0) "$whole" else String.format(Locale.US, "%d.%d", whole, tenths)
}

/**
 * [amount] multiplied by a rate in basis points, rounded half up.
 *
 * Kept as one function so every mini app rounds money the same way; two places
 * rounding differently would produce totals that disagree by a cent.
 */
fun applyRate(amount: Long, basisPoints: Int): Long =
    (amount * basisPoints + BASIS_POINTS_WHOLE / 2) / BASIS_POINTS_WHOLE
