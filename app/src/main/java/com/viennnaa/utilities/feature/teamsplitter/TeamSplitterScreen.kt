package com.viennnaa.utilities.feature.teamsplitter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.options.addOption
import com.viennnaa.utilities.core.options.rejectionFor
import com.viennnaa.utilities.core.options.removeOption
import com.viennnaa.utilities.core.storage.rememberMiniAppPreferences
import com.viennnaa.utilities.miniapp.MiniAppIds
import com.viennnaa.utilities.ui.components.CountStepper
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.components.OptionEditor
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

private const val KEY_NAMES = "names"
private const val KEY_TEAM_COUNT = "teamCount"

/** Splitting one name between teams is not a split, so two is the floor. */
private const val MIN_NAMES_TO_SPLIT = 2

@Composable
fun TeamSplitterScreen(onBack: () -> Unit) {
    var names by rememberSaveable { mutableStateOf(listOf<String>()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var teamCount by rememberSaveable { mutableStateOf(2) }
    // Nested lists of strings save as serializable ArrayLists, so a split
    // survives rotation without having to be dealt again.
    var teams by rememberSaveable { mutableStateOf(listOf<List<String>>()) }

    val haptics = LocalHapticFeedback.current
    val rejection = rejectionFor(names, draft)

    // Typing a roster is real work, so it is kept. The dealt teams are not.
    val prefs = rememberMiniAppPreferences(MiniAppIds.TEAM_SPLITTER)
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restored) {
            names = prefs.getStringList(KEY_NAMES)
            teamCount = clampTeamCount(prefs.getInt(KEY_TEAM_COUNT, teamCount))
            restored = true
        }
    }
    LaunchedEffect(restored, names, teamCount) {
        if (restored) {
            prefs.setStringList(KEY_NAMES, names)
            prefs.setInt(KEY_TEAM_COUNT, teamCount)
        }
    }

    fun add() {
        if (rejection == null) {
            names = addOption(names, draft)
            draft = ""
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.team_splitter_title),
        onBack = onBack,
        actions = {
            if (teams.isNotEmpty()) {
                TextButton(onClick = { teams = emptyList() }) {
                    Text(stringResource(R.string.action_clear))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ResultCard(
                teams = teams,
                nameCount = names.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            OptionEditor(
                options = names,
                draft = draft,
                onDraftChange = { draft = it },
                onAdd = { add() },
                onRemove = { index -> names = removeOption(names, index) },
                rejection = rejection,
                labelRes = R.string.team_splitter_add_hint,
            )

            CountStepper(
                labelRes = R.string.team_splitter_teams_heading,
                count = teamCount,
                onCountChange = { teamCount = clampTeamCount(it) },
                range = MIN_TEAMS..MAX_TEAMS,
            )

            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    teams = splitIntoTeams(names, teamCount)
                },
                enabled = names.size >= MIN_NAMES_TO_SPLIT,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (teams.isEmpty()) {
                            R.string.team_splitter_split
                        } else {
                            R.string.team_splitter_split_again
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    teams: List<List<String>>,
    nameCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        if (teams.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (nameCount >= MIN_NAMES_TO_SPLIT) {
                        stringResource(R.string.team_splitter_ready, nameCount)
                    } else {
                        stringResource(R.string.team_splitter_empty_hint)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.team_splitter_shuffle_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(teams) { index, team ->
                    Column {
                        Text(
                            // A single team is a shuffled list, not "Team 1".
                            text = if (teams.size == 1) {
                                stringResource(R.string.team_splitter_shuffled_label)
                            } else {
                                stringResource(R.string.team_splitter_team_label, index + 1)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = team.joinToString(separator = "  ·  "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeamSplitterScreenPreview() {
    UtilitiesTheme { TeamSplitterScreen(onBack = {}) }
}
