package com.viennnaa.utilities.miniapp

import androidx.annotation.StringRes
import com.viennnaa.utilities.R

/**
 * How mini apps are grouped on the home screen.
 *
 * Declaration order is display order, so a new category appears where it is
 * declared rather than wherever its first mini app happens to be registered.
 */
enum class MiniAppCategory(@StringRes val titleRes: Int) {
    /** Anything that makes a random choice for you. */
    DECIDE(R.string.category_decide),

    /** Anything that works out a number from numbers you give it. */
    CALCULATE(R.string.category_calculate),

    /** Text handling, and the codes that carry text around. */
    TEXT_AND_CODES(R.string.category_text_and_codes),

    /** Mini apps that use the hardware in the phone. */
    DEVICE(R.string.category_device),
}
