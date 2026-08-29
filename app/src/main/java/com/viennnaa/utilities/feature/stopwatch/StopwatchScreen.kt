package com.viennnaa.utilities.feature.stopwatch

import android.Manifest
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import kotlinx.coroutines.delay

/** Redraw rate while running: fast enough for hundredths to look continuous. */
private const val FRAME_MILLIS = 33L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var modeName by rememberSaveable { mutableStateOf(ClockMode.STOPWATCH.name) }
    val mode = ClockMode.entries.firstOrNull { it.name == modeName } ?: ClockMode.STOPWATCH

    var stopwatch by remember { mutableStateOf(StopwatchState()) }
    var timer by remember { mutableStateOf(setTimer(TimerPresets.first())) }

    // Recomputed from the monotonic clock rather than counted up, so the reading
    // is right even if this loop was not running for a while.
    var now by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    val running = if (mode == ClockMode.STOPWATCH) stopwatch.isRunning else timer.isRunning

    LaunchedEffect(running) {
        while (running) {
            now = SystemClock.elapsedRealtime()
            delay(FRAME_MILLIS)
        }
        now = SystemClock.elapsedRealtime()
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Running without the notification is fine; only the alert is lost. */ }

    fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // A finished countdown stops itself so the button reads "start" again.
    LaunchedEffect(timer, now) {
        if (mode == ClockMode.TIMER && timer.isRunning && isFinished(timer, now)) {
            timer = pauseTimer(timer, now)
            TimerService.stop(context)
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.stopwatch_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClockMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = { modeName = option.name },
                        label = { Text(stringResource(modeLabel(option))) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (mode == ClockMode.TIMER && timer.total > 0) {
                    CircularProgressIndicator(
                        progress = { timerProgress(timer, now) },
                        modifier = Modifier.size(260.dp),
                        strokeWidth = 10.dp,
                    )
                }
                Text(
                    text = if (mode == ClockMode.STOPWATCH) {
                        formatStopwatch(elapsedOf(stopwatch, now))
                    } else {
                        formatTimer(remainingOf(timer, now))
                    },
                    // Monospaced, or the whole readout shifts as digits change.
                    fontFamily = FontFamily.Monospace,
                    fontSize = 52.sp,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (mode == ClockMode.TIMER) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimerPresets.forEach { preset ->
                        FilterChip(
                            selected = timer.total == preset && !timer.isRunning,
                            onClick = {
                                TimerService.stop(context)
                                timer = setTimer(preset)
                            },
                            enabled = !timer.isRunning,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.stopwatch_preset_minutes,
                                        presetMinutes(preset),
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = {
                        val clock = SystemClock.elapsedRealtime()
                        if (mode == ClockMode.STOPWATCH) {
                            if (stopwatch.isRunning) {
                                stopwatch = pauseStopwatch(stopwatch, clock)
                                TimerService.stop(context)
                            } else {
                                stopwatch = startStopwatch(stopwatch, clock)
                                askForNotifications()
                                TimerService.start(
                                    context,
                                    isTimer = false,
                                    reference = clock - stopwatch.accumulated,
                                )
                            }
                        } else {
                            if (timer.isRunning) {
                                timer = pauseTimer(timer, clock)
                                TimerService.stop(context)
                            } else {
                                val started = startTimer(timer, clock)
                                if (started.isRunning) {
                                    timer = started
                                    askForNotifications()
                                    started.deadline?.let {
                                        TimerService.start(context, isTimer = true, reference = it)
                                    }
                                }
                            }
                        }
                    },
                    enabled = mode == ClockMode.STOPWATCH || remainingOf(timer, now) > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (running) R.string.stopwatch_pause else R.string.stopwatch_start,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                OutlinedButton(
                    onClick = {
                        val clock = SystemClock.elapsedRealtime()
                        if (mode == ClockMode.STOPWATCH) {
                            if (stopwatch.isRunning) {
                                stopwatch = recordLap(stopwatch, clock)
                            } else {
                                stopwatch = resetStopwatch()
                                TimerService.stop(context)
                            }
                        } else {
                            TimerService.stop(context)
                            timer = setTimer(timer.total)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = stringResource(
                            when {
                                mode == ClockMode.TIMER -> R.string.stopwatch_reset
                                stopwatch.isRunning -> R.string.stopwatch_lap
                                else -> R.string.stopwatch_reset
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            if (mode == ClockMode.STOPWATCH && stopwatch.laps.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                ) {
                    itemsIndexed(stopwatch.laps) { index, cumulative ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.stopwatch_lap_number,
                                    stopwatch.laps.size - index,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = formatStopwatch(lapDuration(stopwatch.laps, index)),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = formatStopwatch(cumulative),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun modeLabel(mode: ClockMode): Int = when (mode) {
    ClockMode.STOPWATCH -> R.string.stopwatch_mode_stopwatch
    ClockMode.TIMER -> R.string.stopwatch_mode_timer
}

@Preview(showBackground = true)
@Composable
private fun StopwatchScreenPreview() {
    UtilitiesTheme { StopwatchScreen(onBack = {}) }
}
