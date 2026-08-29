package com.viennnaa.utilities.feature.randomnumber

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.storage.rememberMiniAppPreferences
import com.viennnaa.utilities.miniapp.MiniAppIds
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.ResultTextStyle
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val KEY_PRESET = "preset"
private const val KEY_MIN = "min"
private const val KEY_MAX = "max"

/** Marks "no preset selected" — the custom range fields are in charge instead. */
private const val CUSTOM_RANGE = -1

/** Frames flashed before the number settles, and the gap between them. */
private const val ROLL_FRAMES = 9
private const val ROLL_FRAME_MILLIS = 45L

@Composable
fun RandomNumberScreen(onBack: () -> Unit) {
    var selectedPreset by rememberSaveable { mutableStateOf(1) }
    var minText by rememberSaveable { mutableStateOf("1") }
    var maxText by rememberSaveable { mutableStateOf("50") }
    var result by rememberSaveable { mutableStateOf<Int?>(null) }
    var history by rememberSaveable { mutableStateOf(listOf<Int>()) }

    // Only alive while a roll is running, so it is deliberately not saved.
    var preview by remember { mutableStateOf<Int?>(null) }
    var rollJob by remember { mutableStateOf<Job?>(null) }
    val isRolling = preview != null

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // The chosen range is a preference worth keeping; the rolls themselves are not.
    val prefs = rememberMiniAppPreferences(MiniAppIds.RANDOM_NUMBER)
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restored) {
            selectedPreset = prefs.getInt(KEY_PRESET, selectedPreset)
            minText = prefs.getString(KEY_MIN, minText)
            maxText = prefs.getString(KEY_MAX, maxText)
            restored = true
        }
    }
    LaunchedEffect(restored, selectedPreset, minText, maxText) {
        if (restored) {
            prefs.setInt(KEY_PRESET, selectedPreset)
            prefs.setString(KEY_MIN, minText)
            prefs.setString(KEY_MAX, maxText)
        }
    }

    val validation = remember(minText, maxText) { validateRange(minText, maxText) }
    // getOrNull, not [], so a saved index that no longer exists (presets changed
    // in an update) falls back to the custom fields instead of crashing.
    val activeRange: IntRange? = if (selectedPreset == CUSTOM_RANGE) {
        (validation as? RangeValidation.Valid)?.range
    } else {
        DefaultPresets.getOrNull(selectedPreset)?.range
    }

    fun roll(range: IntRange) {
        rollJob?.cancel()
        rollJob = scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(ROLL_FRAMES) {
                preview = randomIn(range)
                delay(ROLL_FRAME_MILLIS)
            }
            val settled = randomIn(range)
            result = settled
            history = recordResult(history, settled)
            preview = null
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.random_number_title),
        onBack = onBack,
        actions = {
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = {
                        history = emptyList()
                        result = null
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ResultCard(
                value = preview ?: result,
                activeRange = activeRange,
                isRolling = isRolling,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            RangePicker(
                selectedPreset = selectedPreset,
                onSelectPreset = { selectedPreset = it },
                minText = minText,
                maxText = maxText,
                onMinChange = { minText = sanitizeBoundInput(it) },
                onMaxChange = { maxText = sanitizeBoundInput(it) },
                problem = validation as? RangeValidation.Problem,
            )

            Button(
                onClick = { activeRange?.let { range -> roll(range) } },
                enabled = activeRange != null && !isRolling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (result == null) {
                            R.string.random_number_generate
                        } else {
                            R.string.random_number_generate_again
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            HistoryStrip(history = history)
        }
    }
}

@Composable
private fun ResultCard(
    value: Int?,
    activeRange: IntRange?,
    isRolling: Boolean,
    modifier: Modifier = Modifier,
) {
    // Dips while the numbers flicker, then springs back as the result lands.
    val scale by animateFloatAsState(
        targetValue = if (isRolling) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "resultScale",
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
            val resultDescription = value?.let {
                stringResource(R.string.cd_random_number_result, it)
            }
            Text(
                text = value?.toString() ?: stringResource(R.string.random_number_empty_result),
                style = ResultTextStyle,
                color = if (value == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .semantics {
                        if (resultDescription != null) contentDescription = resultDescription
                    },
            )
            Text(
                text = if (activeRange != null) {
                    stringResource(
                        R.string.random_number_active_range,
                        activeRange.first,
                        activeRange.last,
                    )
                } else {
                    stringResource(R.string.random_number_empty_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangePicker(
    selectedPreset: Int,
    onSelectPreset: (Int) -> Unit,
    minText: String,
    maxText: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    problem: RangeValidation.Problem?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.random_number_range_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultPresets.forEachIndexed { index, preset ->
                FilterChip(
                    selected = selectedPreset == index,
                    onClick = { onSelectPreset(index) },
                    label = {
                        Text(
                            stringResource(
                                R.string.random_number_preset,
                                preset.min,
                                preset.max,
                            ),
                        )
                    },
                )
            }
            FilterChip(
                selected = selectedPreset == CUSTOM_RANGE,
                onClick = { onSelectPreset(CUSTOM_RANGE) },
                label = { Text(stringResource(R.string.random_number_custom)) },
            )
        }

        if (selectedPreset == CUSTOM_RANGE) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = onMinChange,
                    label = { Text(stringResource(R.string.random_number_min)) },
                    singleLine = true,
                    isError = problem != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = onMaxChange,
                    label = { Text(stringResource(R.string.random_number_max)) },
                    singleLine = true,
                    isError = problem != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            if (problem != null) {
                Text(
                    text = stringResource(problem.messageRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HistoryStrip(history: List<Int>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.random_number_history_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { value ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

private fun RangeValidation.Problem.messageRes(): Int = when (this) {
    RangeValidation.Problem.NotANumber -> R.string.random_number_error_not_a_number
    RangeValidation.Problem.OutOfBounds -> R.string.random_number_error_out_of_bounds
    RangeValidation.Problem.MinAboveMax -> R.string.random_number_error_min_above_max
}

@Preview(showBackground = true)
@Composable
private fun RandomNumberScreenPreview() {
    UtilitiesTheme { RandomNumberScreen(onBack = {}) }
}
