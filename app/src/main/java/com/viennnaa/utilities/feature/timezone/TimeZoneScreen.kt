package com.viennnaa.utilities.feature.timezone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.storage.rememberMiniAppPreferences
import com.viennnaa.utilities.miniapp.MiniAppIds
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val KEY_HOME_ZONE = "homeZone"

/** Zones offered as the reference, kept short so the chips stay tappable. */
private const val REFERENCE_CHIP_COUNT = 6

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeZoneScreen(onBack: () -> Unit) {
    // Read once on entry so the list does not shift under the user at midnight.
    val deviceZone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(deviceZone) }
    val openedAt = remember { Instant.now() }

    var homeZoneId by rememberSaveable { mutableStateOf(deviceZone.id) }
    var clockText by rememberSaveable {
        mutableStateOf(formatClock(openedAt, deviceZone))
    }

    val homeZone = zoneOf(homeZoneId) ?: deviceZone
    val time = parseClock(clockText)
    val instant = remember(time, homeZone, today) {
        time?.let { instantAt(today, it, homeZone) }
    }
    val clockError = clockText.isNotBlank() && time == null

    val prefs = rememberMiniAppPreferences(MiniAppIds.TIME_ZONES)
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restored) {
            homeZoneId = prefs.getString(KEY_HOME_ZONE, homeZoneId)
            restored = true
        }
    }
    LaunchedEffect(restored, homeZoneId) {
        if (restored) prefs.setString(KEY_HOME_ZONE, homeZoneId)
    }

    // The device's own zone first, then the best-known others.
    val referenceChoices = remember(deviceZone) {
        val device = CommonZones.firstOrNull { it.id == deviceZone.id }
            ?: ZoneEntry(deviceZone.id, deviceZone.id.substringAfterLast('/').replace('_', ' '))
        listOf(device) + CommonZones.filter { it.id != device.id }.take(REFERENCE_CHIP_COUNT)
    }

    MiniAppScaffold(
        title = stringResource(R.string.time_zones_title),
        onBack = onBack,
        actions = {
            TextButton(
                onClick = {
                    val now = Instant.now()
                    clockText = formatClock(now, homeZone)
                },
            ) {
                Text(stringResource(R.string.time_zones_now))
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = clockText,
                onValueChange = { clockText = it },
                label = { Text(stringResource(R.string.time_zones_time)) },
                placeholder = { Text(stringResource(R.string.time_zones_time_format)) },
                singleLine = true,
                isError = clockError,
                supportingText = if (clockError) {
                    { Text(stringResource(R.string.time_zones_time_error)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.time_zones_reference),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                referenceChoices.forEach { entry ->
                    FilterChip(
                        selected = entry.id == homeZone.id,
                        onClick = { homeZoneId = entry.id },
                        label = { Text(entry.label) },
                    )
                }
            }

            HorizontalDivider()

            if (instant == null) {
                Text(
                    text = stringResource(R.string.time_zones_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                val ordered = remember(instant) { zonesByOffset(instant) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(ordered, key = { it.id }) { entry ->
                        ZoneRow(entry = entry, instant = instant, reference = homeZone)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneRow(entry: ZoneEntry, instant: Instant, reference: ZoneId) {
    val zone = zoneOf(entry.id) ?: return
    val shift = dayShift(instant, reference, zone)
    val isReference = zone.id == reference.id

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isReference) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = offsetLabel(instant, zone),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatClock(instant, zone),
                // Monospaced so the column of times lines up.
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (shift != 0) {
                Text(
                    text = stringResource(
                        if (shift > 0) {
                            R.string.time_zones_next_day
                        } else {
                            R.string.time_zones_previous_day
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeZoneScreenPreview() {
    UtilitiesTheme { TimeZoneScreen(onBack = {}) }
}
