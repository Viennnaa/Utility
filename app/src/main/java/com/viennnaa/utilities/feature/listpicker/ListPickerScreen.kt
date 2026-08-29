package com.viennnaa.utilities.feature.listpicker

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.options.addOption
import com.viennnaa.utilities.core.options.rejectionFor
import com.viennnaa.utilities.core.options.removeOption
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.components.OptionEditor
import com.viennnaa.utilities.ui.theme.ResultTextStyle
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Options flashed before the pick settles, and the gap between them. */
private const val PICK_FRAMES = 9
private const val PICK_FRAME_MILLIS = 45L

/** Picking from a single option is not a choice, so two is the floor. */
private const val MIN_OPTIONS_TO_PICK = 2

@Composable
fun ListPickerScreen(onBack: () -> Unit) {
    var options by rememberSaveable { mutableStateOf(listOf<String>()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    var history by rememberSaveable { mutableStateOf(listOf<String>()) }
    var removeAfterPick by rememberSaveable { mutableStateOf(false) }

    // Only alive while a pick is running, so it is deliberately not saved.
    var preview by remember { mutableStateOf<String?>(null) }
    var pickJob by remember { mutableStateOf<Job?>(null) }
    val isPicking = preview != null

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val rejection = rejectionFor(options, draft)

    fun add() {
        if (rejection == null) {
            options = addOption(options, draft)
            draft = ""
        }
    }

    fun pick() {
        pickJob?.cancel()
        pickJob = scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(PICK_FRAMES) {
                // Reads the list each frame: it cannot shrink mid-pick, since the
                // chips are disabled while this runs.
                preview = pickIndex(options)?.let { options[it] }
                delay(PICK_FRAME_MILLIS)
            }
            val index = pickIndex(options)
            if (index != null) {
                val choice = options[index]
                picked = choice
                history = recordPick(history, choice)
                if (removeAfterPick) options = removeOption(options, index)
            }
            preview = null
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.list_picker_title),
        onBack = onBack,
        actions = {
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = {
                        history = emptyList()
                        picked = null
                    },
                ) {
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
                pick = preview ?: picked,
                optionCount = options.size,
                isPicking = isPicking,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            OptionEditor(
                options = options,
                draft = draft,
                onDraftChange = { draft = it },
                onAdd = { add() },
                onRemove = { index -> options = removeOption(options, index) },
                rejection = rejection,
                labelRes = R.string.list_picker_add_hint,
                enabled = !isPicking,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.list_picker_remove_after_pick),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = removeAfterPick,
                    onCheckedChange = { removeAfterPick = it },
                    enabled = !isPicking,
                )
            }

            Button(
                onClick = { pick() },
                enabled = options.size >= MIN_OPTIONS_TO_PICK && !isPicking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (picked == null) {
                            R.string.list_picker_pick
                        } else {
                            R.string.list_picker_pick_again
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            HistoryStrip(history = history)
        }
    }
}

/**
 * Long options have to shrink to fit; a short one should still fill the card.
 */
private fun resultFontSize(text: String): TextUnit = when {
    text.length <= 6 -> 64.sp
    text.length <= 12 -> 46.sp
    text.length <= 24 -> 32.sp
    else -> 24.sp
}

@Composable
private fun ResultCard(
    pick: String?,
    optionCount: Int,
    isPicking: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPicking) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "pickScale",
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val text = pick ?: stringResource(R.string.list_picker_empty_result)
            val size = resultFontSize(text)
            Text(
                text = text,
                style = ResultTextStyle.copy(fontSize = size, lineHeight = size * 1.2f),
                color = if (pick == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            )
            Text(
                text = when {
                    isPicking -> stringResource(R.string.list_picker_thinking)
                    optionCount >= MIN_OPTIONS_TO_PICK ->
                        stringResource(R.string.list_picker_ready, optionCount)

                    else -> stringResource(R.string.list_picker_empty_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun HistoryStrip(history: List<String>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.list_picker_history_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { pick ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = pick,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListPickerScreenPreview() {
    UtilitiesTheme { ListPickerScreen(onBack = {}) }
}
