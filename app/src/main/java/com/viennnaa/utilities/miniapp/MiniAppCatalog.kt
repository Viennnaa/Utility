package com.viennnaa.utilities.miniapp

import com.viennnaa.utilities.R
import com.viennnaa.utilities.feature.choicemaker.ChoiceMakerScreen
import com.viennnaa.utilities.feature.diceroller.DiceRollerScreen
import com.viennnaa.utilities.feature.datecalc.DateCalcScreen
import com.viennnaa.utilities.feature.listpicker.ListPickerScreen
import com.viennnaa.utilities.feature.passwordgen.PasswordGenScreen
import com.viennnaa.utilities.feature.percentage.PercentageScreen
import com.viennnaa.utilities.feature.randomnumber.RandomNumberScreen
import com.viennnaa.utilities.feature.rps.RpsScreen
import com.viennnaa.utilities.feature.teamsplitter.TeamSplitterScreen
import com.viennnaa.utilities.feature.texttools.TextToolsScreen
import com.viennnaa.utilities.feature.tipsplitter.TipSplitterScreen
import com.viennnaa.utilities.feature.unitconverter.UnitConverterScreen
import com.viennnaa.utilities.ui.theme.AccentChoiceMaker
import com.viennnaa.utilities.ui.theme.AccentDateCalc
import com.viennnaa.utilities.ui.theme.AccentDiceRoller
import com.viennnaa.utilities.ui.theme.AccentListPicker
import com.viennnaa.utilities.ui.theme.AccentPasswordGen
import com.viennnaa.utilities.ui.theme.AccentPercentage
import com.viennnaa.utilities.ui.theme.AccentRandomNumber
import com.viennnaa.utilities.ui.theme.AccentRps
import com.viennnaa.utilities.ui.theme.AccentTeamSplitter
import com.viennnaa.utilities.ui.theme.AccentTextTools
import com.viennnaa.utilities.ui.theme.AccentTipSplitter
import com.viennnaa.utilities.ui.theme.AccentUnitConverter

/**
 * Every mini app Utilities ships, in the order they appear on the home screen.
 *
 * This is the one place a new mini app is registered: add an entry and it shows
 * up on the home screen and gets its own route for free.
 */
val MiniAppCatalog: List<MiniApp> = listOf(
    MiniApp(
        id = MiniAppIds.RANDOM_NUMBER,
        titleRes = R.string.random_number_title,
        taglineRes = R.string.random_number_tagline,
        category = MiniAppCategory.DECIDE,
        emoji = "🔢",
        accent = AccentRandomNumber,
        screen = { onBack -> RandomNumberScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.CHOICE_MAKER,
        titleRes = R.string.choice_maker_title,
        taglineRes = R.string.choice_maker_tagline,
        category = MiniAppCategory.DECIDE,
        emoji = "🤔",
        accent = AccentChoiceMaker,
        screen = { onBack -> ChoiceMakerScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.LIST_PICKER,
        titleRes = R.string.list_picker_title,
        taglineRes = R.string.list_picker_tagline,
        category = MiniAppCategory.DECIDE,
        emoji = "🎯",
        accent = AccentListPicker,
        screen = { onBack -> ListPickerScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.DICE_ROLLER,
        titleRes = R.string.dice_roller_title,
        taglineRes = R.string.dice_roller_tagline,
        category = MiniAppCategory.DECIDE,
        emoji = "🎲",
        accent = AccentDiceRoller,
        screen = { onBack -> DiceRollerScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.TEAM_SPLITTER,
        titleRes = R.string.team_splitter_title,
        taglineRes = R.string.team_splitter_tagline,
        category = MiniAppCategory.DECIDE,
        emoji = "👥",
        accent = AccentTeamSplitter,
        screen = { onBack -> TeamSplitterScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.TIP_SPLITTER,
        titleRes = R.string.tip_splitter_title,
        taglineRes = R.string.tip_splitter_tagline,
        category = MiniAppCategory.CALCULATE,
        emoji = "💸",
        accent = AccentTipSplitter,
        screen = { onBack -> TipSplitterScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.RPS,
        titleRes = R.string.rps_title,
        taglineRes = R.string.rps_tagline,
        category = MiniAppCategory.DECIDE,
        emoji = "\u270A",
        accent = AccentRps,
        screen = { onBack -> RpsScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.UNIT_CONVERTER,
        titleRes = R.string.unit_converter_title,
        taglineRes = R.string.unit_converter_tagline,
        category = MiniAppCategory.CALCULATE,
        emoji = "\U0001F4CF",
        accent = AccentUnitConverter,
        screen = { onBack -> UnitConverterScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.PERCENTAGE,
        titleRes = R.string.percentage_title,
        taglineRes = R.string.percentage_tagline,
        category = MiniAppCategory.CALCULATE,
        emoji = "\U0001F4CA",
        accent = AccentPercentage,
        screen = { onBack -> PercentageScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.DATE_CALC,
        titleRes = R.string.date_calc_title,
        taglineRes = R.string.date_calc_tagline,
        category = MiniAppCategory.CALCULATE,
        emoji = "\U0001F4C5",
        accent = AccentDateCalc,
        screen = { onBack -> DateCalcScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.PASSWORD_GEN,
        titleRes = R.string.password_gen_title,
        taglineRes = R.string.password_gen_tagline,
        category = MiniAppCategory.TEXT_AND_CODES,
        emoji = "\U0001F510",
        accent = AccentPasswordGen,
        screen = { onBack -> PasswordGenScreen(onBack = onBack) },
    ),
    MiniApp(
        id = MiniAppIds.TEXT_TOOLS,
        titleRes = R.string.text_tools_title,
        taglineRes = R.string.text_tools_tagline,
        category = MiniAppCategory.TEXT_AND_CODES,
        emoji = "\u270D\uFE0F",
        accent = AccentTextTools,
        screen = { onBack -> TextToolsScreen(onBack = onBack) },
    ),
)

/** Looks up a mini app by [MiniApp.id], or null if nothing is registered under it. */
fun findMiniApp(id: String?, catalog: List<MiniApp> = MiniAppCatalog): MiniApp? =
    catalog.firstOrNull { it.id == id }
