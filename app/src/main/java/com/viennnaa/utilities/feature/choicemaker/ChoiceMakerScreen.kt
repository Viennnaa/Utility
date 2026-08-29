package com.viennnaa.utilities.feature.choicemaker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.ResultTextStyle
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import com.viennnaa.utilities.ui.theme.extendedColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Frames flashed before the answer settles, and the gap between them. */
private const val THINK_FRAMES = 8
private const val THINK_FRAME_MILLIS = 55L

@Composable
fun ChoiceMakerScreen(onBack: () -> Unit) {
    var answer by rememberSaveable { mutableStateOf<Choice?>(null) }
    var history by rememberSaveable { mutableStateOf(listOf<Choice>()) }

    // Only alive while the coin is in the air, so it is deliberately not saved.
    var preview by remember { mutableStateOf<Choice?>(null) }
    var thinkJob by remember { mutableStateOf<Job?>(null) }
    val isThinking = preview != null

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    fun ask() {
        thinkJob?.cancel()
        thinkJob = scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(THINK_FRAMES) { frame ->
                // Alternating rather than random, so the flicker reads as a coin
                // spinning instead of a stutter.
                preview = if (frame % 2 == 0) Choice.Yes else Choice.No
                delay(THINK_FRAME_MILLIS)
            }
            val settled = decide()
            answer = settled
            history = recordChoice(history, settled)
            preview = null
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.choice_maker_title),
        onBack = onBack,
        actions = {
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = {
                        history = emptyList()
                        answer = null
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AnswerCard(
                answer = preview ?: answer,
                isThinking = isThinking,
                tally = tally(history),
                onClick = { if (!isThinking) ask() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Button(
                onClick = { ask() },
                enabled = !isThinking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (answer == null) {
                            R.string.choice_maker_decide
                        } else {
                            R.string.choice_maker_again
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
private fun AnswerCard(
    answer: Choice?,
    isThinking: Boolean,
    tally: Tally,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetColor = when (answer) {
        Choice.Yes -> extendedColors.yes
        Choice.No -> extendedColors.no
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val color by animateColorAsState(targetValue = targetColor, label = "answerColor")
    val scale by animateFloatAsState(
        targetValue = if (isThinking) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "answerScale",
    )

    Card(
        modifier = modifier.clickable(onClick = onClick),
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
            Text(
                text = when (answer) {
                    Choice.Yes -> stringResource(R.string.choice_maker_yes)
                    Choice.No -> stringResource(R.string.choice_maker_no)
                    null -> stringResource(R.string.choice_maker_empty_result)
                },
                style = ResultTextStyle,
                color = color,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            )
            Text(
                text = when {
                    isThinking -> stringResource(R.string.choice_maker_thinking)
                    tally.total > 0 -> stringResource(
                        R.string.choice_maker_tally,
                        tally.yes,
                        tally.no,
                    )

                    else -> stringResource(R.string.choice_maker_empty_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun HistoryStrip(history: List<Choice>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) return

    val yes = extendedColors.yes
    val no = extendedColors.no

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.choice_maker_history_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(history) { index, choice ->
                val accent = if (choice == Choice.Yes) yes else no
                // The newest answer is solid; older ones fade back.
                val alpha = if (index == 0) 1f else 0.18f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = alpha))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (choice == Choice.Yes) {
                                R.string.choice_maker_yes
                            } else {
                                R.string.choice_maker_no
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (index == 0) contrastOn(accent) else accent,
                    )
                }
            }
        }
    }
}

/** Black or white, whichever stays readable on [background]. */
private fun contrastOn(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White

@Preview(showBackground = true)
@Composable
private fun ChoiceMakerScreenPreview() {
    UtilitiesTheme { ChoiceMakerScreen(onBack = {}) }
}
