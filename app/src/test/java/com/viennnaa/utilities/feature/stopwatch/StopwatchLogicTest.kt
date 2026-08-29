package com.viennnaa.utilities.feature.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchLogicTest {

    @Test
    fun `a fresh stopwatch is stopped at zero`() {
        val state = StopwatchState()
        assertFalse(state.isRunning)
        assertEquals(0L, elapsedOf(state, 1_000L))
    }

    @Test
    fun `elapsed grows with the clock while running`() {
        val state = startStopwatch(StopwatchState(), now = 1_000L)
        assertEquals(0L, elapsedOf(state, 1_000L))
        assertEquals(500L, elapsedOf(state, 1_500L))
        assertEquals(5_000L, elapsedOf(state, 6_000L))
    }

    @Test
    fun `elapsed survives a long gap, which is the point of using timestamps`() {
        // Ten minutes backgrounded: a tick counter would have missed all of it.
        val state = startStopwatch(StopwatchState(), now = 0L)
        assertEquals(600_000L, elapsedOf(state, 600_000L))
    }

    @Test
    fun `pausing banks the elapsed time`() {
        var state = startStopwatch(StopwatchState(), now = 1_000L)
        state = pauseStopwatch(state, now = 4_000L)
        assertFalse(state.isRunning)
        assertEquals(3_000L, elapsedOf(state, 99_000L))
    }

    @Test
    fun `resuming continues from where it stopped`() {
        var state = startStopwatch(StopwatchState(), now = 0L)
        state = pauseStopwatch(state, now = 3_000L)
        state = startStopwatch(state, now = 10_000L)
        assertEquals(3_000L, elapsedOf(state, 10_000L))
        assertEquals(5_000L, elapsedOf(state, 12_000L))
    }

    @Test
    fun `starting an already running stopwatch changes nothing`() {
        val state = startStopwatch(StopwatchState(), now = 1_000L)
        assertEquals(state, startStopwatch(state, now = 5_000L))
    }

    @Test
    fun `pausing a stopped stopwatch changes nothing`() {
        val state = StopwatchState(accumulated = 500L)
        assertEquals(state, pauseStopwatch(state, now = 9_000L))
    }

    @Test
    fun `a clock that goes backwards cannot produce negative time`() {
        val state = startStopwatch(StopwatchState(), now = 5_000L)
        assertEquals(0L, elapsedOf(state, 1_000L))
    }

    @Test
    fun `reset clears everything`() {
        assertEquals(StopwatchState(), resetStopwatch())
    }

    @Test
    fun `laps are cumulative and newest first`() {
        var state = startStopwatch(StopwatchState(), now = 0L)
        state = recordLap(state, now = 1_000L)
        state = recordLap(state, now = 3_000L)
        assertEquals(listOf(3_000L, 1_000L), state.laps)
    }

    @Test
    fun `lap duration is the gap from the previous lap`() {
        val laps = listOf(3_000L, 1_000L)
        assertEquals(2_000L, lapDuration(laps, 0))
        assertEquals(1_000L, lapDuration(laps, 1))
        assertEquals(0L, lapDuration(laps, 5))
    }

    @Test
    fun `laps are capped`() {
        var state = startStopwatch(StopwatchState(), now = 0L)
        repeat(MAX_LAPS + 10) { state = recordLap(state, now = it.toLong() * 100) }
        assertEquals(MAX_LAPS, state.laps.size)
    }

    @Test
    fun `a timer counts down from the set duration`() {
        var timer = setTimer(60_000L)
        assertEquals(60_000L, remainingOf(timer, 0L))
        timer = startTimer(timer, now = 1_000L)
        assertEquals(60_000L, remainingOf(timer, 1_000L))
        assertEquals(30_000L, remainingOf(timer, 31_000L))
    }

    @Test
    fun `a timer stops at zero rather than going negative`() {
        val timer = startTimer(setTimer(10_000L), now = 0L)
        assertEquals(0L, remainingOf(timer, 10_000L))
        assertEquals(0L, remainingOf(timer, 999_000L))
    }

    @Test
    fun `a timer reports finished only once it reaches zero`() {
        val timer = startTimer(setTimer(10_000L), now = 0L)
        assertFalse(isFinished(timer, 9_999L))
        assertTrue(isFinished(timer, 10_000L))
        assertTrue(isFinished(timer, 20_000L))
    }

    @Test
    fun `an unarmed timer is never finished`() {
        assertFalse(isFinished(TimerState(), 1_000L))
        assertFalse(isFinished(setTimer(0L), 1_000L))
    }

    @Test
    fun `pausing a timer keeps the remaining time`() {
        var timer = startTimer(setTimer(60_000L), now = 0L)
        timer = pauseTimer(timer, now = 20_000L)
        assertFalse(timer.isRunning)
        assertEquals(40_000L, remainingOf(timer, 99_000L))
    }

    @Test
    fun `resuming a paused timer picks up where it left off`() {
        var timer = startTimer(setTimer(60_000L), now = 0L)
        timer = pauseTimer(timer, now = 20_000L)
        timer = startTimer(timer, now = 100_000L)
        assertEquals(40_000L, remainingOf(timer, 100_000L))
        assertEquals(35_000L, remainingOf(timer, 105_000L))
    }

    @Test
    fun `a timer with nothing left cannot start`() {
        val timer = setTimer(0L)
        assertEquals(timer, startTimer(timer, now = 0L))
    }

    @Test
    fun `progress runs from zero to one`() {
        val timer = startTimer(setTimer(100L), now = 0L)
        assertEquals(0f, timerProgress(timer, 0L), 1e-6f)
        assertEquals(0.5f, timerProgress(timer, 50L), 1e-6f)
        assertEquals(1f, timerProgress(timer, 100L), 1e-6f)
        assertEquals(1f, timerProgress(timer, 500L), 1e-6f)
        assertEquals(0f, timerProgress(TimerState(), 0L), 1e-6f)
    }

    @Test
    fun `stopwatch formatting grows an hours field only when needed`() {
        assertEquals("00:00.00", formatStopwatch(0L))
        assertEquals("00:01.50", formatStopwatch(1_500L))
        assertEquals("01:00.00", formatStopwatch(60_000L))
        assertEquals("59:59.99", formatStopwatch(3_599_990L))
        assertEquals("1:00:00.00", formatStopwatch(3_600_000L))
    }

    @Test
    fun `stopwatch formatting refuses to show negative time`() {
        assertEquals("00:00.00", formatStopwatch(-500L))
    }

    @Test
    fun `timer formatting rounds up so the last second is still shown`() {
        // Showing 0:00 while a second remains would look finished early.
        assertEquals("00:01", formatTimer(1L))
        assertEquals("00:01", formatTimer(1_000L))
        assertEquals("00:02", formatTimer(1_001L))
        assertEquals("00:00", formatTimer(0L))
        assertEquals("01:00", formatTimer(60_000L))
        assertEquals("1:00:00", formatTimer(3_600_000L))
    }

    @Test
    fun `presets are whole minutes`() {
        assertEquals(listOf(1, 3, 5, 10, 30), TimerPresets.map { presetMinutes(it) })
    }
}
