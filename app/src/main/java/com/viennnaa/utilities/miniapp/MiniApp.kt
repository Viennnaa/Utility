package com.viennnaa.utilities.miniapp

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * One mini app hosted inside Utilities.
 *
 * Everything the super app needs — how the tile looks on the home screen and what
 * to show once it is opened — lives in a single entry, so adding a mini app means
 * adding one item to [MiniAppCatalog] and nothing else.
 *
 * @param id stable identifier, also used as the navigation route argument. Never
 *   change it for a shipped mini app: it is what a deep link or a saved shortcut
 *   points at.
 * @param category which home screen group the tile sits in.
 * @param emoji glyph shown in the home screen tile.
 * @param accent tile color, fixed across themes so each mini app keeps its identity.
 * @param screen the mini app itself; [onBack] returns to the home screen.
 */
data class MiniApp(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val taglineRes: Int,
    val category: MiniAppCategory,
    val emoji: String,
    val accent: Color,
    val screen: @Composable (onBack: () -> Unit) -> Unit,
)
