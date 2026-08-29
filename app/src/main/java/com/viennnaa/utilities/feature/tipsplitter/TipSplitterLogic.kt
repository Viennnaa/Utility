package com.viennnaa.utilities.feature.tipsplitter

/**
 * Bill maths, done entirely in whole cents.
 *
 * Money never touches a Double here: 0.1 + 0.2 is not 0.3 in binary floating
 * point, and a bill split is exactly where that shows up. Everything is a Long
 * count of cents, so the per-person shares always add back to the total.
 */

/** Tip percentages offered as one-tap chips. */
val TipPresets: List<Int> = listOf(0, 10, 15, 18, 20)

/** How many ways a bill can be split. */
const val MIN_PEOPLE = 1
const val MAX_PEOPLE = 20

/** Largest bill accepted, in cents, so the arithmetic stays far from overflow. */
const val MAX_BILL_CENTS = 100_000_000L

/** Widest tip accepted. */
const val MAX_TIP_PERCENT = 100

/** A finished split. All amounts are in whole cents. */
data class BillSplit(
    val billCents: Long,
    val tipCents: Long,
    val totalCents: Long,
    /** One entry per person, largest shares first. Always sums to [totalCents]. */
    val perPerson: List<Long>,
)

/** Clamps a head count coming from a stepper or restored state. */
fun clampPeople(people: Int): Int = people.coerceIn(MIN_PEOPLE, MAX_PEOPLE)

/**
 * Reads a typed amount like "12", "12.5" or "12.34" into whole cents, or null if
 * it is not a usable amount.
 *
 * More than two decimal places is rejected rather than silently truncated: it
 * usually means a typo, and quietly dropping a digit would change the bill.
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
    if (wholeValue > MAX_BILL_CENTS / 100) return null
    return wholeValue * 100 + cents
}

/** Renders whole cents back as a plain decimal string, always with two places. */
fun formatCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = if (cents < 0) -cents else cents
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

/**
 * Works out the tip, total, and each person's share.
 *
 * The tip is rounded to the nearest cent, half up. The total is then divided as
 * evenly as it can be: the leftover cents are handed out one each, so a 10.00
 * bill across three people gives 3.34, 3.33, 3.33 rather than three amounts that
 * quietly lose a cent.
 */
fun splitBill(billCents: Long, tipPercent: Int, people: Int): BillSplit {
    val bill = billCents.coerceIn(0L, MAX_BILL_CENTS)
    val percent = tipPercent.coerceIn(0, MAX_TIP_PERCENT)
    val heads = clampPeople(people)

    // Half-up rounding on bill * percent / 100, without leaving integer maths.
    val tip = (bill * percent + 50) / 100
    val total = bill + tip

    val base = total / heads
    val leftover = (total % heads).toInt()
    val perPerson = List(heads) { index -> if (index < leftover) base + 1 else base }

    return BillSplit(billCents = bill, tipCents = tip, totalCents = total, perPerson = perPerson)
}
