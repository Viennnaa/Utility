package com.viennnaa.utilities.feature.discount

import com.viennnaa.utilities.core.money.BASIS_POINTS_PER_PERCENT
import com.viennnaa.utilities.core.money.BASIS_POINTS_WHOLE
import com.viennnaa.utilities.core.money.MAX_AMOUNT_CENTS
import com.viennnaa.utilities.core.money.applyRate

/**
 * Discounts and tax, in whole cents throughout.
 *
 * The three jobs are kept apart because they are not the same sum. Taking 20%
 * off and adding 20% on are inverses only by coincidence of wording: removing
 * tax from a gross price divides rather than multiplies, which is the mistake
 * this mini app exists to avoid making by hand.
 */

/** What the mini app is working out. */
enum class PriceMode { DISCOUNT, ADD_TAX, REMOVE_TAX }

/** Rates offered as one-tap chips, in whole percent. */
val RatePresets: List<Int> = listOf(5, 10, 15, 20, 25)

/** Widest rate accepted, in whole percent. */
const val MAX_RATE_PERCENT = 100

/**
 * @param base the amount the rate applies to: the original price, the net, or
 *   the net recovered from a gross.
 * @param adjustment the money that moved: the saving, or the tax.
 * @param result what the user pays or is quoted.
 */
data class PriceResult(
    val base: Long,
    val adjustment: Long,
    val result: Long,
)

/** Takes [basisPoints] off [priceCents]. */
fun applyDiscount(priceCents: Long, basisPoints: Int): PriceResult {
    val price = priceCents.coerceIn(0L, MAX_AMOUNT_CENTS)
    val rate = clampRate(basisPoints)
    val saving = applyRate(price, rate)
    return PriceResult(base = price, adjustment = saving, result = price - saving)
}

/** Adds [basisPoints] of tax to a net [netCents]. */
fun addTax(netCents: Long, basisPoints: Int): PriceResult {
    val net = netCents.coerceIn(0L, MAX_AMOUNT_CENTS)
    val rate = clampRate(basisPoints)
    val tax = applyRate(net, rate)
    return PriceResult(base = net, adjustment = tax, result = net + tax)
}

/**
 * Recovers the net from a tax-inclusive [grossCents].
 *
 * This is a division, not the reverse multiplication: net = gross / (1 + rate).
 * Taking the rate off the gross instead is the classic error — 20% off a
 * 120.00 gross gives 96.00, not the 100.00 it came from.
 *
 * The tax is then the remainder rather than a second rounded calculation, so
 * net plus tax always equals the gross exactly.
 */
fun removeTax(grossCents: Long, basisPoints: Int): PriceResult {
    val gross = grossCents.coerceIn(0L, MAX_AMOUNT_CENTS)
    val rate = clampRate(basisPoints)
    val divisor = BASIS_POINTS_WHOLE.toLong() + rate
    val net = (gross * BASIS_POINTS_WHOLE + divisor / 2) / divisor
    return PriceResult(base = net, adjustment = gross - net, result = gross)
}

fun clampRate(basisPoints: Int): Int =
    basisPoints.coerceIn(0, MAX_RATE_PERCENT * BASIS_POINTS_PER_PERCENT)

/** Runs whichever calculation [mode] names. */
fun priceFor(mode: PriceMode, amountCents: Long, basisPoints: Int): PriceResult = when (mode) {
    PriceMode.DISCOUNT -> applyDiscount(amountCents, basisPoints)
    PriceMode.ADD_TAX -> addTax(amountCents, basisPoints)
    PriceMode.REMOVE_TAX -> removeTax(amountCents, basisPoints)
}
