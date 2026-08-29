package com.viennnaa.utilities.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * The super app's front door: mini apps grouped by category, with a search field
 * that flattens them into one list of matches while a query is typed.
 */
@Composable
fun HomeScreen(
    miniApps: List<MiniApp>,
    onOpenMiniApp: (MiniApp) -> Unit,
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

    val trimmedQuery = query.trim()
    val searching = trimmedQuery.isNotEmpty()
    val matches = if (searching) entries.filter { it.matches(trimmedQuery) } else entries

    Scaffold(modifier = modifier) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 158.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Header(
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
                    items(matches, key = { it.miniApp.id }) { entry ->
                        MiniAppTile(entry = entry, onClick = { onOpenMiniApp(entry.miniApp) })
                    }
                }
            } else {
                // Declaration order of the enum is the display order.
                for (category in MiniAppCategory.entries) {
                    val inCategory = matches.filter { it.miniApp.category == category }
                    if (inCategory.isEmpty()) continue

                    item(span = { GridItemSpan(maxLineSpan) }, key = "category-${category.name}") {
                        CategoryHeading(category)
                    }
                    items(inCategory, key = { it.miniApp.id }) { entry ->
                        MiniAppTile(entry = entry, onClick = { onOpenMiniApp(entry.miniApp) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.home_search_hint)) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Composable
private fun CategoryHeading(category: MiniAppCategory, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(category.titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
private fun MiniAppTile(
    entry: HomeEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 150.dp)
            .clickable(role = Role.Button, onClickLabel = entry.title, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(entry.miniApp.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = entry.miniApp.emoji, fontSize = 26.sp)
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
        HomeScreen(miniApps = MiniAppCatalog, onOpenMiniApp = {})
    }
}
