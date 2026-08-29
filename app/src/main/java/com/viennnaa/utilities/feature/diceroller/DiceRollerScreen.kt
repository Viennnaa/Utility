package com.viennnaa.utilities.feature.diceroller

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.storage.rememberMiniAppPreferences
import com.viennnaa.utilities.miniapp.MiniAppIds
import com.viennnaa.utilities.ui.components.CountStepper
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.ResultTextStyle
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val KEY_SIDES = "sides"
private const val KEY_COUNT = "count"

/** Throws flashed before the dice settle, and the gap between them. */
private const val ROLL_FRAMES = 9
private const val ROLL_FRAME_MILLIS = 45L

@Composable
fun DiceRollerScreen(onBack: () -> Unit) {
    var sides by rememberSaveable { mutableStateOf(6) }
    var count by rememberSaveable { mutableStateOf(1) }
    var rolls by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var history by rememberSaveable { mutableStateOf(listOf<Int>()) }

    // Only alive while the dice are in the air, so it is deliberately not saved.
    var preview by remember { mutableStateOf<List<Int>?>(null) }
    var rollJob by remember { mutableStateOf<Job?>(null) }
    val isRolling = preview != null

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Which dice you reach for is a preference; what they landed on is not.
    val prefs = rememberMiniAppPreferences(MiniAppIds.DICE_ROLLER)
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restored) {
            sides = prefs.getInt(KEY_SIDES, sides)
            count = clampDiceCount(prefs.getInt(KEY_COUNT, count))
            restored = true
        }
    }
    LaunchedEffect(restored, sides, count) {
        if (restored) {
            prefs.setInt(KEY_SIDES, sides)
            prefs.setInt(KEY_COUNT, count)
        }
    }

    fun roll() {
        rollJob?.cancel()
        rollJob = scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(ROLL_FRAMES) {
                preview = rollDice(count, sides)
                delay(ROLL_FRAME_MILLIS)
            }
            val settled = rollDice(count, sides)
            rolls = settled
            history = recordTotal(history, total(settled))
            preview = null
        }
    }

    val shown = preview ?: rolls

    MiniAppScaffold(
        title = stringResource(R.string.dice_roller_title),
        onBack = onBack,
        actions = {
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = {
                        history = emptyList()
                        rolls = emptyList()
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
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ResultCard(
                rolls = shown,
                count = count,
                sides = sides,
                isRolling = isRolling,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            DiceTypePicker(
                selectedSides = sides,
                onSelectSides = { sides = it },
                enabled = !isRolling,
            )

            CountStepper(
                labelRes = R.string.dice_roller_count_heading,
                count = count,
                onCountChange = { count = clampDiceCount(it) },
                range = MIN_DICE..MAX_DICE,
                enabled = !isRolling,
            )

            Button(
                onClick = { roll() },
                enabled = !isRolling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (rolls.isEmpty()) {
                            R.string.dice_roller_roll
                        } else {
                            R.string.dice_roller_roll_again
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            HistoryStrip(history = history)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(
    rolls: List<Int>,
    count: Int,
    sides: Int,
    isRolling: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isRolling) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "diceScale",
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
            val rollTotal = total(rolls)
            val totalDescription = stringResource(R.string.cd_dice_roller_total, rollTotal)
            Text(
                text = if (rolls.isEmpty()) {
                    stringResource(R.string.dice_roller_empty_result)
                } else {
                    rollTotal.toString()
                },
                style = ResultTextStyle,
                color = if (rolls.isEmpty()) {
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
                        if (rolls.isNotEmpty()) contentDescription = totalDescription
                    },
            )

            // With one die the total is the die, so the breakdown adds nothing.
            if (rolls.size > 1) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    rolls.forEach { value ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Text(
                text = if (rolls.isEmpty()) {
                    stringResource(R.string.dice_roller_empty_hint)
                } else {
                    stringResource(R.string.dice_roller_spec, count, sides)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiceTypePicker(
    selectedSides: Int,
    onSelectSides: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dice_roller_type_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiceTypes.forEach { sides ->
                FilterChip(
                    selected = selectedSides == sides,
                    onClick = { onSelectSides(sides) },
                    enabled = enabled,
                    label = { Text(stringResource(R.string.dice_roller_die, sides)) },
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
            text = stringResource(R.string.dice_roller_history_heading),
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

@Preview(showBackground = true)
@Composable
private fun DiceRollerScreenPreview() {
    UtilitiesTheme { DiceRollerScreen(onBack = {}) }
}
