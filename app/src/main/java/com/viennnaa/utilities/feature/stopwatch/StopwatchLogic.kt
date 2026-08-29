package com.viennnaa.utilities.feature.stopwatch

import java.util.Locale

/**
 * Stopwatch and countdown state.
 *
 * Timing is derived from a monotonic timestamp rather than counted in ticks.
 * A tick counter drifts whenever the UI stops being drawn — backgrounded, screen
 * off, process killed — so the only reading that stays honest is one computed
 * from when the clock actually started. Every function takes `now` rather than
 * reading the clock itself, which is also what makes it testable.
 */

/** Which half of the mini app is showing. */
enum class ClockMode { STOPWATCH, TIMER }

/** How many laps are kept. */
const val MAX_LAPS = 50

/**
 * @param startedAt monotonic timestamp the current run began, or null if paused.
 * @param accumulated milliseconds banked by previous runs.
 */
data class StopwatchState(
    val startedAt: Long? = null,
    val accumulated: Long = 0L,
    val laps: List<Long> = emptyList(),
) {
    val isRunning: Boolean get() = startedAt != null
}

/** Total elapsed time at [now]. */
fun elapsedOf(state: StopwatchState, now: Long): Long {
    val running = state.startedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L
    return state.accumulated + running
}

fun startStopwatch(state: StopwatchState, now: Long): StopwatchState =
    if (state.isRunning) state else state.copy(startedAt = now)

fun pauseStopwatch(state: StopwatchState, now: Long): StopwatchState =
    if (!state.isRunning) state else StopwatchState(
        startedAt = null,
        accumulated = elapsedOf(state, now),
        laps = state.laps,
    )

fun resetStopwatch(): StopwatchState = StopwatchState()

/** Records a lap at [now]. Laps are newest first. */
fun recordLap(state: StopwatchState, now: Long): StopwatchState =
    state.copy(laps = (listOf(elapsedOf(state, now)) + state.laps).take(MAX_LAPS))

/** How long lap [index] took on its own, given laps are cumulative and newest first. */
fun lapDuration(laps: List<Long>, index: Int): Long {
    if (index !in laps.indices) return 0L
    val previous = laps.getOrNull(index + 1) ?: 0L
    return laps[index] - previous
}

/**
 * @param deadline monotonic timestamp the countdown ends, or null if paused.
 * @param remainingWhenPaused milliseconds left while paused.
 * @param total the duration originally set, for the progress ring.
 */
data class TimerState(
    val deadline: Long? = null,
    val remainingWhenPaused: Long = 0L,
    val total: Long = 0L,
) {
    val isRunning: Boolean get() = deadline != null
}

/** Milliseconds left at [now], never below zero. */
fun remainingOf(timer: TimerState, now: Long): Long =
    if (timer.deadline != null) {
        (timer.deadline - now).coerceAtLeast(0L)
    } else {
        timer.remainingWhenPaused
    }

/** True once a running timer has reached zero. */
fun isFinished(timer: TimerState, now: Long): Boolean =
    timer.total > 0L && remainingOf(timer, now) == 0L

fun startTimer(timer: TimerState, now: Long): TimerState {
    if (timer.isRunning || timer.remainingWhenPaused <= 0L) return timer
    return timer.copy(deadline = now + timer.remainingWhenPaused, remainingWhenPaused = 0L)
}

fun pauseTimer(timer: TimerState, now: Long): TimerState {
    if (!timer.isRunning) return timer
    return TimerState(
        deadline = null,
        remainingWhenPaused = remainingOf(timer, now),
        total = timer.total,
    )
}

/** Arms a timer for [duration], replacing whatever was there. */
fun setTimer(duration: Long): TimerState =
    TimerState(deadline = null, remainingWhenPaused = duration.coerceAtLeast(0L), total = duration.coerceAtLeast(0L))

/** How far through the countdown, 0 to 1, for the progress ring. */
fun timerProgress(timer: TimerState, now: Long): Float {
    if (timer.total <= 0L) return 0f
    return ((timer.total - remainingOf(timer, now)).toFloat() / timer.total).coerceIn(0f, 1f)
}

/** mm:ss.hh, growing an hours field only once it is needed. */
fun formatStopwatch(millis: Long): String {
    val safe = millis.coerceAtLeast(0L)
    val hundredths = (safe % 1000) / 10
    val totalSeconds = safe / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
    } else {
        String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }
}

/**
 * mm:ss for a countdown, rounded up: a timer showing 0:01 should still have a
 * second to run, not be somewhere inside the final second.
 */
fun formatTimer(millis: Long): String {
    val safe = millis.coerceAtLeast(0L)
    val totalSeconds = (safe + 999) / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/** Durations offered as one-tap chips, in milliseconds. */
val TimerPresets: List<Long> = listOf(
    60_000L,
    3 * 60_000L,
    5 * 60_000L,
    10 * 60_000L,
    30 * 60_000L,
)

/** A preset's label, e.g. "5 min". */
fun presetMinutes(millis: Long): Int = (millis / 60_000L).toInt()
