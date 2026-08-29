package com.viennnaa.utilities.miniapp

import com.viennnaa.utilities.R
import com.viennnaa.utilities.feature.choicemaker.ChoiceMakerScreen
import com.viennnaa.utilities.feature.randomnumber.RandomNumberScreen
import com.viennnaa.utilities.ui.theme.AccentChoiceMaker
import com.viennnaa.utilities.ui.theme.AccentRandomNumber

/**
 * Every mini app Utilities ships, in the order they appear on the home screen.
 *
 * This is the one place a new mini app is registered: add an entry and it shows
 * up on the home screen and gets its own route for free.
 */
val MiniAppCatalog: List<MiniApp> = listOf(
    MiniApp(
        id = "random-number",
        titleRes = R.string.random_number_title,
        taglineRes = R.string.random_number_tagline,
        emoji = "🎲",
        accent = AccentRandomNumber,
        screen = { onBack -> RandomNumberScreen(onBack = onBack) },
    ),
    MiniApp(
        id = "choice-maker",
        titleRes = R.string.choice_maker_title,
        taglineRes = R.string.choice_maker_tagline,
        emoji = "🤔",
        accent = AccentChoiceMaker,
        screen = { onBack -> ChoiceMakerScreen(onBack = onBack) },
    ),
)

/** Looks up a mini app by [MiniApp.id], or null if nothing is registered under it. */
fun findMiniApp(id: String?, catalog: List<MiniApp> = MiniAppCatalog): MiniApp? =
    catalog.firstOrNull { it.id == id }
