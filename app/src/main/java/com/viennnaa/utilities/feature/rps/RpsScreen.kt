package com.viennnaa.utilities.feature.rps

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import com.viennnaa.utilities.ui.theme.extendedColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Moves flashed while the opponent "thinks", and the gap between them. */
private const val THINK_FRAMES = 8
private const val THINK_FRAME_MILLIS = 60L

@Composable
fun RpsScreen(onBack: () -> Unit) {
    var playerMove by rememberSaveable { mutableStateOf<Move?>(null) }
    var opponentMove by rememberSaveable { mutableStateOf<Move?>(null) }
    var outcome by rememberSaveable { mutableStateOf<Outcome?>(null) }
    var wins by rememberSaveable { mutableStateOf(0) }
    var losses by rememberSaveable { mutableStateOf(0) }
    var draws by rememberSaveable { mutableStateOf(0) }

    var preview by remember { mutableStateOf<Move?>(null) }
    var playJob by remember { mutableStateOf<Job?>(null) }
    val thinking = preview != null

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val score = Score(wins = wins, losses = losses, draws = draws)

    fun play(move: Move) {
        playJob?.cancel()
        playJob = scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            playerMove = move
            outcome = null
            repeat(THINK_FRAMES) { frame ->
                preview = Move.entries[frame % Move.entries.size]
                delay(THINK_FRAME_MILLIS)
            }
            val theirs = randomMove()
            val result = judge(move, theirs)
            opponentMove = theirs
            outcome = result
            when (result) {
                Outcome.WIN -> wins++
                Outcome.LOSE -> losses++
                Outcome.DRAW -> draws++
            }
            preview = null
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.rps_title),
        onBack = onBack,
        actions = {
            if (score.played > 0) {
                TextButton(
                    onClick = {
                        wins = 0
                        losses = 0
                        draws = 0
                        playerMove = null
                        opponentMove = null
                        outcome = null
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
                playerMove = playerMove,
                opponentMove = preview ?: opponentMove,
                outcome = if (thinking) null else outcome,
                thinking = thinking,
                score = score,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Move.entries.forEach { move ->
                    OutlinedButton(
                        onClick = { play(move) },
                        enabled = !thinking,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                    ) {
                        Text(text = emojiFor(move), fontSize = 28.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    playerMove: Move?,
    opponentMove: Move?,
    outcome: Outcome?,
    thinking: Boolean,
    score: Score,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (thinking) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "rpsScale",
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                Text(text = playerMove?.let { emojiFor(it) } ?: "❓", fontSize = 56.sp)
                Text(
                    text = stringResource(R.string.rps_versus),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = opponentMove?.let { emojiFor(it) } ?: "❓", fontSize = 56.sp)
            }

            Text(
                text = when {
                    thinking -> stringResource(R.string.rps_thinking)
                    outcome != null -> stringResource(outcomeLabel(outcome))
                    else -> stringResource(R.string.rps_empty_hint)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = when (outcome) {
                    Outcome.WIN -> extendedColors.yes
                    Outcome.LOSE -> extendedColors.no
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )

            if (score.played > 0) {
                Text(
                    text = stringResource(
                        R.string.rps_score,
                        score.wins,
                        score.losses,
                        score.draws,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun emojiFor(move: Move): String = when (move) {
    Move.ROCK -> "✊"
    Move.PAPER -> "✋"
    Move.SCISSORS -> "✌️"
}

private fun outcomeLabel(outcome: Outcome): Int = when (outcome) {
    Outcome.WIN -> R.string.rps_win
    Outcome.LOSE -> R.string.rps_lose
    Outcome.DRAW -> R.string.rps_draw
}

@Preview(showBackground = true)
@Composable
private fun RpsScreenPreview() {
    UtilitiesTheme { RpsScreen(onBack = {}) }
}
