package com.viennnaa.utilities

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.viennnaa.utilities.core.settings.AppSettings
import com.viennnaa.utilities.core.storage.AppPreferences
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniAppNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            UtilitiesTheme {
                // Fixed settings rather than the live flow: these tests are about
                // navigation, and a store that writes back would make them
                // depend on each other's leftovers.
                UtilitiesApp(settings = AppSettings(), preferences = AppPreferences(context))
            }
        }
    }

    /**
     * Opens a mini app by searching for it first. With more than twenty tiles
     * most are scrolled out of the grid, and a node that is not composed cannot
     * be clicked, so search is the reliable way to reach one.
     *
     * The query is lowercased so it does not collide with the tile: text
     * matching is case-sensitive and covers editable text, so typing the title
     * exactly would match the search field as well and leave two candidates.
     */
    private fun openMiniApp(title: String) {
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput(title.lowercase())
        compose.onNodeWithText(title).performClick()
    }

    @Test
    fun openingAMiniAppAndComingBackReturnsToTheGrid() {
        showApp()
        openMiniApp("Choice Maker")
        // Text only this mini app shows, so it cannot be confused with the tile.
        compose.onNodeWithText("Ask a question, then tap Decide").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Utilities").assertIsDisplayed()
    }

    @Test
    fun settingsOpensFromTheHomeScreenAndComesBack() {
        showApp()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        compose.onNodeWithText("Theme").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Utilities").assertIsDisplayed()
    }

    @Test
    fun theThemeChoicesAreAllOffered() {
        showApp()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("System").assertIsDisplayed()
        compose.onNodeWithText("Light").assertIsDisplayed()
        compose.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun mostMiniAppsOpenAndCloseWithoutCrashing() {
        showApp()
        // A spread across the categories. The hardware ones are left out: they
        // depend on sensors an emulator may not report.
        val titles = listOf(
            "Random Number",
            "List Picker",
            "Dice Roller",
            "Team Splitter",
            "Tip Splitter",
            "Unit Converter",
            "Percentage",
            "Date Calculator",
            "Discount and VAT",
            "Time Zones",
            "Password Generator",
            "Text Tools",
            "WiFi QR",
            "Rock Paper Scissors",
        )
        for (title in titles) {
            openMiniApp(title)
            compose.onNodeWithContentDescription("Back").performClick()
        }
        compose.onNodeWithText("Utilities").assertIsDisplayed()
    }
}
