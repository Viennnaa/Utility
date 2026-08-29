package com.viennnaa.utilities.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.settings.sanitizeFavourites
import com.viennnaa.utilities.miniapp.MiniApp
import com.viennnaa.utilities.miniapp.MiniAppCatalog
import com.viennnaa.utilities.miniapp.MiniAppCategory
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

/**
 * A mini app with its labels already resolved, so searching can match the words
 * the user actually sees rather than resource ids.
 */
private data class HomeEntry(
    val miniApp: MiniApp,
    val title: String,
    val tagline: String,
) {
    fun matches(query: String): Boolean =
        title.contains(query, ignoreCase = true) || tagline.contains(query, ignoreCase = true)
}

/**
 * The super app's front door: pinned mini apps first, then the rest grouped by
 * category, with a search field that flattens everything into one list while a
 * query is being typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    miniApps: List<MiniApp>,
    favouriteIds: List<String>,
    onOpenMiniApp: (MiniApp) -> Unit,
    onToggleFavourite: (MiniApp) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    val entries = mutableListOf<HomeEntry>()
    for (miniApp in miniApps) {
        entries.add(
            HomeEntry(
                miniApp = miniApp,
                title = stringResource(miniApp.titleRes),
                tagline = stringResource(miniApp.taglineRes),
            ),
        )
    }

    // Stored favourites outlive the catalog, so ids for mini apps that no longer
    // exist are dropped before anything is drawn.
    val pinnedIds = sanitizeFavourites(favouriteIds, miniApps.map { it.id }.toSet())
    val trimmedQuery = query.trim()
    val searching = trimmedQuery.isNotEmpty()
    val matches = if (searching) entries.filter { it.matches(trimmedQuery) } else entries
    val pinned = pinnedIds.mapNotNull { id -> entries.firstOrNull { it.miniApp.id == id } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 158.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "search") {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (searching) {
                // Categories would be noise here: a search is already a filter.
                if (matches.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                        Text(
                            text = stringResource(R.string.home_no_results, trimmedQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                } else {
                    tiles(matches, pinnedIds, onOpenMiniApp, onToggleFavourite)
                }
            } else {
                if (pinned.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "heading-favourites") {
                        SectionHeading(stringResource(R.string.home_favourites))
                    }
                    tiles(pinned, pinnedIds, onOpenMiniApp, onToggleFavourite, keyPrefix = "fav-")
                }

                // Declaration order of the enum is the display order.
                for (category in MiniAppCategory.entries) {
                    val inCategory = matches.filter { it.miniApp.category == category }
                    if (inCategory.isEmpty()) continue

                    item(span = { GridItemSpan(maxLineSpan) }, key = "heading-${category.name}") {
                        SectionHeading(stringResource(category.titleRes))
                    }
                    tiles(inCategory, pinnedIds, onOpenMiniApp, onToggleFavourite)
                }

                item(span = { GridItemSpan(maxLineSpan) }, key = "pin-hint") {
                    Text(
                        text = stringResource(R.string.home_pin_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

/** The tiles themselves, shared by the search results and every section. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.tiles(
    entries: List<HomeEntry>,
    pinnedIds: List<String>,
    onOpenMiniApp: (MiniApp) -> Unit,
    onToggleFavourite: (MiniApp) -> Unit,
    keyPrefix: String = "",
) {
    items(entries, key = { keyPrefix + it.miniApp.id }) { entry ->
        MiniAppTile(
            entry = entry,
            pinned = entry.miniApp.id in pinnedIds,
            onClick = { onOpenMiniApp(entry.miniApp) },
            onLongClick = { onToggleFavourite(entry.miniApp) },
        )
    }
}

@Composable
private fun SectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.home_search_hint)) },
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_home_clear_search),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiniAppTile(
    entry: HomeEntry,
    pinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // A small dip under the finger, so a tap feels like it landed on something.
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "tileScale")

    Card(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 150.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = entry.title,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                onClick = onClick,
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(entry.miniApp.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = entry.miniApp.emoji, fontSize = 26.sp)
                }
                if (pinned) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(R.string.cd_home_pinned),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = entry.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    UtilitiesTheme {
        HomeScreen(
            miniApps = MiniAppCatalog,
            favouriteIds = listOf(MiniAppCatalog.first().id),
            onOpenMiniApp = {},
            onToggleFavourite = {},
            onOpenSettings = {},
        )
    }
}
