package com.viennnaa.utilities.feature.level

import android.hardware.Sensor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.sensors.SensorEffect
import com.viennnaa.utilities.core.sensors.rememberHasSensors
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import com.viennnaa.utilities.ui.theme.extendedColors

/** Tilt beyond this pins the bubble to the rim rather than off the dial. */
private const val BUBBLE_RANGE_DEGREES = 30f

@Composable
fun LevelScreen(onBack: () -> Unit) {
    var tilt by remember { mutableStateOf<Tilt?>(null) }
    val hasSensor = rememberHasSensors(Sensor.TYPE_ACCELEROMETER)

    SensorEffect(Sensor.TYPE_ACCELEROMETER) { event ->
        val reading = tiltFrom(event.values[0], event.values[1], event.values[2])
        // Smoothed, or the bubble jitters with every hand tremor.
        tilt = smooth(tilt, reading)
    }

    MiniAppScaffold(
        title = stringResource(R.string.level_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!hasSensor) {
                Text(
                    text = stringResource(R.string.level_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                return@Column
            }

            val current = tilt
            val levelColor = extendedColors.yes
            val offColor = MaterialTheme.colorScheme.primary
            val outline = MaterialTheme.colorScheme.outline
            val surface = MaterialTheme.colorScheme.surfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                val radius = size.minDimension / 2f
                val centre = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = surface, radius = radius, center = centre)
                drawCircle(
                    color = outline,
                    radius = radius,
                    center = centre,
                    style = Stroke(width = 3f),
                )
                // Rings at a third and two thirds, as a coarse scale.
                for (fraction in listOf(0.33f, 0.66f)) {
                    drawCircle(
                        color = outline.copy(alpha = 0.4f),
                        radius = radius * fraction,
                        center = centre,
                        style = Stroke(width = 2f),
                    )
                }
                drawLine(outline.copy(alpha = 0.4f), Offset(centre.x - radius, centre.y), Offset(centre.x + radius, centre.y), strokeWidth = 2f)
                drawLine(outline.copy(alpha = 0.4f), Offset(centre.x, centre.y - radius), Offset(centre.x, centre.y + radius), strokeWidth = 2f)

                if (current != null) {
                    val bubbleRadius = radius * 0.16f
                    val travel = radius - bubbleRadius
                    // Clamped so a steep tilt parks the bubble on the rim
                    // instead of drawing it outside the dial.
                    val x = (current.roll / BUBBLE_RANGE_DEGREES).coerceIn(-1.0, 1.0) * travel
                    val y = (current.pitch / BUBBLE_RANGE_DEGREES).coerceIn(-1.0, 1.0) * travel
                    drawCircle(
                        color = if (current.isLevel) levelColor else offColor,
                        radius = bubbleRadius,
                        center = Offset(centre.x + x.toFloat(), centre.y - y.toFloat()),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Reading(R.string.level_roll, current?.roll)
                Reading(R.string.level_pitch, current?.pitch)
            }

            Text(
                text = when {
                    current == null -> stringResource(R.string.level_waiting)
                    current.isLevel -> stringResource(R.string.level_is_level)
                    else -> stringResource(R.string.level_off_by, formatDegrees(current.magnitude))
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (current?.isLevel == true) {
                    extendedColors.yes
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun Reading(labelRes: Int, value: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (value == null) "—" else "${formatDegrees(value)}°",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LevelScreenPreview() {
    UtilitiesTheme { LevelScreen(onBack = {}) }
}
