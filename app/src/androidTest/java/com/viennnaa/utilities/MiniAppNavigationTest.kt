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
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniAppNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showApp() {
        compose.setContent { UtilitiesTheme { UtilitiesApp() } }
    }

    /**
     * Opens a mini app by searching for it first. With nineteen tiles most are
     * scrolled out of the grid, and a node that is not composed cannot be
     * clicked, so search is the reliable way to reach one.
     */
    private fun openMiniApp(title: String) {
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput(title)
        compose.onNodeWithText(title).performClick()
    }

    @Test
    fun openingAMiniAppAndComingBackReturnsToTheGrid() {
        showApp()
        openMiniApp("Choice Maker")
        // Text only this mini app shows, so it cannot be confused with the tile.
        compose.onNodeWithText("Ask a question, then tap Decide").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Small tools, one app").assertIsDisplayed()
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
            "Password Generator",
            "Text Tools",
            "Rock Paper Scissors",
        )
        for (title in titles) {
            openMiniApp(title)
            compose.onNodeWithContentDescription("Back").performClick()
        }
        compose.onNodeWithText("Small tools, one app").assertIsDisplayed()
    }
}
