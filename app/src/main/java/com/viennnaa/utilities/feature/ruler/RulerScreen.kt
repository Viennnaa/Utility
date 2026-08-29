package com.viennnaa.utilities.feature.ruler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.storage.rememberMiniAppPreferences
import com.viennnaa.utilities.miniapp.MiniAppIds
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

private const val KEY_CALIBRATION = "calibration"
private const val KEY_UNIT = "unit"

/** Calibration is a float; DataStore holds it as a whole percentage. */
private const val PERCENT = 100f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // The physical density the manufacturer reports, which is what calibration
    // exists to correct.
    val dpi = remember { context.resources.displayMetrics.ydpi }

    var calibration by rememberSaveable { mutableStateOf(1f) }
    var unitName by rememberSaveable { mutableStateOf(RulerUnit.MILLIMETRES.name) }
    val unit = RulerUnit.entries.firstOrNull { it.name == unitName } ?: RulerUnit.MILLIMETRES

    val prefs = rememberMiniAppPreferences(MiniAppIds.RULER)
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restored) {
            calibration = clampCalibration(prefs.getInt(KEY_CALIBRATION, PERCENT.toInt()) / PERCENT)
            unitName = prefs.getString(KEY_UNIT, unitName)
            restored = true
        }
    }
    LaunchedEffect(restored, calibration, unitName) {
        if (restored) {
            prefs.setInt(KEY_CALIBRATION, (calibration * PERCENT).toInt())
            prefs.setString(KEY_UNIT, unitName)
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary

    val pixelsPerTick = when (unit) {
        RulerUnit.MILLIMETRES -> pixelsPerMillimetre(dpi, calibration)
        // Eight ticks to the inch, so a tick is an eighth.
        RulerUnit.INCHES -> pixelsPerInch(dpi, calibration)?.div(8f)
    }

    MiniAppScaffold(
        title = stringResource(R.string.ruler_title),
        onBack = onBack,
        actions = {
            if (calibration != 1f) {
                TextButton(onClick = { calibration = 1f }) {
                    Text(stringResource(R.string.ruler_reset))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (pixelsPerTick == null) {
                Text(
                    text = stringResource(R.string.ruler_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
                return@Column
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val ticks = tickCount(size.height, pixelsPerTick)
                val fullLength = size.width * 0.42f
                for (index in 0 until ticks) {
                    val y = index * pixelsPerTick
                    val length = fullLength * tickFraction(index, unit)
                    drawLine(
                        color = onSurface,
                        start = Offset(0f, y),
                        end = Offset(length, y),
                        strokeWidth = if (tickIsLabelled(index, unit)) 4f else 2f,
                    )
                    if (tickIsLabelled(index, unit) && index > 0) {
                        val label = tickLabel(index, unit).toString()
                        drawText(
                            textMeasurer = textMeasurer,
                            text = label,
                            topLeft = Offset(length + 12f, y - 20f),
                            style = TextStyle(color = onSurface, fontSize = 14.sp),
                        )
                    }
                }
                // Edge line, so the zero end is obvious.
                drawLine(
                    color = accent,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4f,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.ruler_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RulerUnit.entries.forEach { option ->
                        FilterChip(
                            selected = option == unit,
                            onClick = { unitName = option.name },
                            label = { Text(stringResource(unitLabel(option))) },
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.ruler_calibration,
                            calibrationPercent(calibration),
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { calibration = clampCalibration(calibration - CALIBRATION_STEP) },
                        enabled = calibration > MIN_CALIBRATION,
                    ) {
                        Text(stringResource(R.string.ruler_shorter))
                    }
                    TextButton(
                        onClick = { calibration = clampCalibration(calibration + CALIBRATION_STEP) },
                        enabled = calibration < MAX_CALIBRATION,
                    ) {
                        Text(stringResource(R.string.ruler_longer))
                    }
                }
            }
        }
    }
}

private fun unitLabel(unit: RulerUnit): Int = when (unit) {
    RulerUnit.MILLIMETRES -> R.string.ruler_mm
    RulerUnit.INCHES -> R.string.ruler_inches
}

@Preview(showBackground = true)
@Composable
private fun RulerScreenPreview() {
    UtilitiesTheme { RulerScreen(onBack = {}) }
}
