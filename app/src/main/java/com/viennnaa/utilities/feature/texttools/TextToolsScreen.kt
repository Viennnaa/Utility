package com.viennnaa.utilities.feature.texttools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextToolsScreen(onBack: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val stats = textStats(text)

    MiniAppScaffold(
        title = stringResource(R.string.text_tools_title),
        onBack = onBack,
        actions = {
            if (text.isNotEmpty()) {
                TextButton(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                    Text(stringResource(R.string.action_copy))
                }
                TextButton(onClick = { text = "" }) {
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
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.text_tools_input)) },
                placeholder = { Text(stringResource(R.string.text_tools_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            StatsCard(stats = stats, modifier = Modifier.fillMaxWidth())

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.text_tools_transform_heading),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextTransform.entries.forEach { transform ->
                        AssistChip(
                            onClick = { text = applyTransform(text, transform) },
                            enabled = text.isNotEmpty(),
                            label = { Text(stringResource(transformLabel(transform))) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(stats: TextStats, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat(R.string.text_tools_stat_characters, stats.characters)
            Stat(R.string.text_tools_stat_no_spaces, stats.charactersNoSpaces)
            Stat(R.string.text_tools_stat_words, stats.words)
            Stat(R.string.text_tools_stat_lines, stats.lines)
        }
    }
}

@Composable
private fun Stat(labelRes: Int, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun transformLabel(transform: TextTransform): Int = when (transform) {
    TextTransform.UPPERCASE -> R.string.text_tools_uppercase
    TextTransform.LOWERCASE -> R.string.text_tools_lowercase
    TextTransform.TITLE_CASE -> R.string.text_tools_title_case
    TextTransform.SENTENCE_CASE -> R.string.text_tools_sentence_case
    TextTransform.REVERSE -> R.string.text_tools_reverse
    TextTransform.COLLAPSE_SPACES -> R.string.text_tools_collapse
    TextTransform.REMOVE_LINE_BREAKS -> R.string.text_tools_no_breaks
}

@Preview(showBackground = true)
@Composable
private fun TextToolsScreenPreview() {
    UtilitiesTheme { TextToolsScreen(onBack = {}) }
}
