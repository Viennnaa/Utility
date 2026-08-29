package com.viennnaa.utilities.feature.tipsplitter

import com.viennnaa.utilities.core.money.MAX_AMOUNT_CENTS
import com.viennnaa.utilities.core.money.applyRate

/**
 * Bill maths. Parsing, formatting and rounding live in `core.money`, shared with
 * the other mini apps that handle prices; what is left here is the split itself.
 *
 * Everything is a Long count of whole cents, so the per-person shares always add
 * back to the total.
 */

/** Tip percentages offered as one-tap chips. */
val TipPresets: List<Int> = listOf(0, 10, 15, 18, 20)

/** How many ways a bill can be split. */
const val MIN_PEOPLE = 1
const val MAX_PEOPLE = 20

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
 * Works out the tip, total, and each person's share.
 *
 * The tip is rounded to the nearest cent, half up. The total is then divided as
 * evenly as it can be: the leftover cents are handed out one each, so a 10.00
 * bill across three people gives 3.34, 3.33, 3.33 rather than three amounts that
 * quietly lose a cent.
 */
fun splitBill(billCents: Long, tipPercent: Int, people: Int): BillSplit {
    val bill = billCents.coerceIn(0L, MAX_AMOUNT_CENTS)
    val percent = tipPercent.coerceIn(0, MAX_TIP_PERCENT)
    val heads = clampPeople(people)

    // Rounded through the shared helper so every mini app that touches money
    // rounds the same way.
    val tip = applyRate(bill, percent * 100)
    val total = bill + tip

    val base = total / heads
    val leftover = (total % heads).toInt()
    val perPerson = List(heads) { index -> if (index < leftover) base + 1 else base }

    return BillSplit(billCents = bill, tipCents = tip, totalCents = total, perPerson = perPerson)
}
