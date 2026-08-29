package com.viennnaa.utilities.feature.discount

import com.viennnaa.utilities.core.money.MAX_AMOUNT_CENTS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DiscountLogicTest {

    @Test
    fun `a discount takes the rate off the price`() {
        val result = applyDiscount(10_000, 2000)
        assertEquals(2_000L, result.adjustment)
        assertEquals(8_000L, result.result)
    }

    @Test
    fun `a discount of nothing leaves the price alone`() {
        val result = applyDiscount(10_000, 0)
        assertEquals(0L, result.adjustment)
        assertEquals(10_000L, result.result)
    }

    @Test
    fun `a full discount leaves nothing to pay`() {
        val result = applyDiscount(10_000, 100 * 100)
        assertEquals(10_000L, result.adjustment)
        assertEquals(0L, result.result)
    }

    @Test
    fun `the saving and the price paid always add back to the original`() {
        for (price in listOf(0L, 1L, 99L, 999L, 12_345L, 1_000_000L)) {
            for (rate in listOf(0, 500, 1750, 2000, 3333, 10_000)) {
                val result = applyDiscount(price, rate)
                assertEquals(
                    "price=$price rate=$rate",
                    price,
                    result.result + result.adjustment,
                )
            }
        }
    }

    @Test
    fun `adding tax puts the rate on top`() {
        val result = addTax(10_000, 2000)
        assertEquals(2_000L, result.adjustment)
        assertEquals(12_000L, result.result)
    }

    @Test
    fun `removing tax divides rather than subtracting`() {
        // The whole point: 20% off a 120.00 gross gives 96.00, not the 100.00
        // it came from.
        val result = removeTax(12_000, 2000)
        assertEquals(10_000L, result.base)
        assertEquals(2_000L, result.adjustment)
        assertEquals(12_000L, result.result)
    }

    @Test
    fun `net plus tax always equals the gross exactly`() {
        for (gross in listOf(0L, 1L, 99L, 100L, 999L, 12_345L, 999_999L)) {
            for (rate in listOf(0, 500, 1750, 2000, 2500)) {
                val result = removeTax(gross, rate)
                assertEquals(
                    "gross=$gross rate=$rate",
                    gross,
                    result.base + result.adjustment,
                )
            }
        }
    }

    @Test
    fun `adding then removing tax comes back to the same net`() {
        for (net in listOf(100L, 999L, 10_000L, 45_678L)) {
            for (rate in listOf(500, 1750, 2000, 2500)) {
                val gross = addTax(net, rate).result
                val back = removeTax(gross, rate).base
                // Rounding can move a single cent; anything more is a bug.
                assertTrue("net=$net rate=$rate came back as $back", abs(back - net) <= 1)
            }
        }
    }

    @Test
    fun `a fractional rate works`() {
        // 17.5% of 100.00 is 17.50.
        assertEquals(1_750L, addTax(10_000, 1750).adjustment)
    }

    @Test
    fun `removing a zero rate changes nothing`() {
        val result = removeTax(12_345, 0)
        assertEquals(12_345L, result.base)
        assertEquals(0L, result.adjustment)
    }

    @Test
    fun `out of range input is clamped rather than crashing`() {
        assertEquals(0L, applyDiscount(-500, 2000).base)
        assertEquals(MAX_AMOUNT_CENTS, applyDiscount(MAX_AMOUNT_CENTS * 10, 0).base)
        assertEquals(0, clampRate(-100))
        assertEquals(MAX_RATE_PERCENT * 100, clampRate(999_999))
    }

    @Test
    fun `priceFor dispatches to the right calculation`() {
        assertEquals(applyDiscount(10_000, 2000), priceFor(PriceMode.DISCOUNT, 10_000, 2000))
        assertEquals(addTax(10_000, 2000), priceFor(PriceMode.ADD_TAX, 10_000, 2000))
        assertEquals(removeTax(10_000, 2000), priceFor(PriceMode.REMOVE_TAX, 10_000, 2000))
    }

    @Test
    fun `every mode handles a zero amount`() {
        for (mode in PriceMode.entries) {
            val result = priceFor(mode, 0, 2000)
            assertEquals("$mode", 0L, result.result)
            assertEquals("$mode", 0L, result.adjustment)
        }
    }
}
