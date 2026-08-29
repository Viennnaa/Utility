package com.viennnaa.utilities.feature.compass

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.sensors.SensorEffect
import com.viennnaa.utilities.core.sensors.rememberHasSensors
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import com.viennnaa.utilities.ui.theme.extendedColors

@Composable
fun CompassScreen(onBack: () -> Unit) {
    var heading by remember { mutableStateOf<Double?>(null) }
    val hasSensors = rememberHasSensors(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD)

    // The rotation matrix needs both readings, and they arrive separately, so
    // the latest of each is held until a pair is available.
    val gravity = remember { FloatArray(3) }
    val geomagnetic = remember { FloatArray(3) }
    val rotation = remember { FloatArray(9) }
    val orientation = remember { FloatArray(3) }

    SensorEffect(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD) { event ->
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            else -> return@SensorEffect
        }
        if (SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotation, orientation)
            val degrees = Math.toDegrees(orientation[0].toDouble())
            heading = smoothHeading(heading, degrees)
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.compass_title),
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
            if (!hasSensors) {
                Text(
                    text = stringResource(R.string.compass_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                return@Column
            }

            val current = heading
            val outline = MaterialTheme.colorScheme.outline
            val surface = MaterialTheme.colorScheme.surfaceVariant
            val needleNorth = extendedColors.no
            val needleSouth = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                val radius = size.minDimension / 2f
                val centre = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = surface, radius = radius, center = centre)
                drawCircle(color = outline, radius = radius, center = centre, style = Stroke(width = 3f))

                // The dial turns under a fixed needle, so "up" is where the
                // device is pointing, which is how a real compass reads.
                rotate(degrees = -(current ?: 0.0).toFloat(), pivot = centre) {
                    for (tick in 0 until 72) {
                        val major = tick % 9 == 0
                        val length = if (major) radius * 0.14f else radius * 0.06f
                        val angle = Math.toRadians(tick * 5.0)
                        val outer = Offset(
                            centre.x + (radius * Math.sin(angle)).toFloat(),
                            centre.y - (radius * Math.cos(angle)).toFloat(),
                        )
                        val inner = Offset(
                            centre.x + ((radius - length) * Math.sin(angle)).toFloat(),
                            centre.y - ((radius - length) * Math.cos(angle)).toFloat(),
                        )
                        drawLine(
                            color = if (major) outline else outline.copy(alpha = 0.5f),
                            start = inner,
                            end = outer,
                            strokeWidth = if (major) 4f else 2f,
                        )
                    }

                    // North marker on the dial.
                    val markerTop = Offset(centre.x, centre.y - radius * 0.78f)
                    drawCircle(color = needleNorth, radius = radius * 0.05f, center = markerTop)
                }

                // Fixed needle pointing up the screen.
                val needle = Path().apply {
                    moveTo(centre.x, centre.y - radius * 0.6f)
                    lineTo(centre.x - radius * 0.09f, centre.y)
                    lineTo(centre.x + radius * 0.09f, centre.y)
                    close()
                }
                drawPath(needle, color = needleNorth)
                val tail = Path().apply {
                    moveTo(centre.x, centre.y + radius * 0.6f)
                    lineTo(centre.x - radius * 0.09f, centre.y)
                    lineTo(centre.x + radius * 0.09f, centre.y)
                    close()
                }
                drawPath(tail, color = needleSouth)
            }

            Text(
                text = if (current == null) "—" else "${formatHeading(current)}°",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = if (current == null) {
                    stringResource(R.string.compass_waiting)
                } else {
                    cardinalFor(current).label
                },
                style = MaterialTheme.typography.titleLarge,
                color = if (current != null && isFacingNorth(current)) {
                    extendedColors.yes
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = stringResource(R.string.compass_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CompassScreenPreview() {
    UtilitiesTheme { CompassScreen(onBack = {}) }
}
