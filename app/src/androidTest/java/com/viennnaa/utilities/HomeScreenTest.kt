package com.viennnaa.utilities

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.viennnaa.utilities.miniapp.MiniAppCatalog
import com.viennnaa.utilities.ui.home.HomeScreen
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showHome(onOpen: (String) -> Unit = {}) {
        compose.setContent {
            UtilitiesTheme {
                HomeScreen(miniApps = MiniAppCatalog, onOpenMiniApp = { onOpen(it.id) })
            }
        }
    }

    /**
     * Targets the field by its text-entry action rather than its placeholder:
     * the placeholder is a separate node and disappears once anything is typed.
     */
    private fun search(query: String) {
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput(query)
    }

    @Test
    fun showsCategoryHeadingsAndTiles() {
        showHome()
        compose.onNodeWithText("Decide").assertIsDisplayed()
        compose.onNodeWithText("Random Number").assertIsDisplayed()
    }

    @Test
    fun searchNarrowsToMatchingMiniApps() {
        showHome()
        search("dice")
        compose.onNodeWithText("Dice Roller").assertIsDisplayed()
        // Gone from the tree entirely, not merely scrolled out of view.
        compose.onNodeWithText("Tip Splitter").assertDoesNotExist()
    }

    @Test
    fun searchMatchesTaglinesNotJustTitles() {
        showHome()
        // "coin flip" appears only in Choice Maker's tagline.
        search("coin flip")
        compose.onNodeWithText("Choice Maker").assertIsDisplayed()
    }

    @Test
    fun searchIgnoresCase() {
        showHome()
        search("DICE")
        compose.onNodeWithText("Dice Roller").assertIsDisplayed()
    }

    @Test
    fun aSearchWithNoMatchesSaysSo() {
        showHome()
        search("zzzzz")
        compose.onNodeWithText("No mini app matches “zzzzz”").assertIsDisplayed()
    }

    @Test
    fun clearingTheSearchBringsTheCategoriesBack() {
        showHome()
        search("dice")
        compose.onNodeWithText("Decide").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithText("Decide").assertIsDisplayed()
    }

    @Test
    fun tappingATileOpensThatMiniApp() {
        var opened: String? = null
        showHome { opened = it }
        search("dice")
        compose.onNodeWithText("Dice Roller").performClick()
        assertEquals("dice-roller", opened)
    }
}
