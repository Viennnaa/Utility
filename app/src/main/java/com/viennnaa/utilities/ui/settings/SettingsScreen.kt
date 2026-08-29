package com.viennnaa.utilities.ui.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.BuildConfig
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.settings.AppSettings
import com.viennnaa.utilities.core.settings.MAX_FAVOURITES
import com.viennnaa.utilities.core.settings.ThemeMode
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

/**
 * App-wide settings. Anything belonging to a single mini app stays inside that
 * mini app, so this screen only holds what applies everywhere.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    miniAppCount: Int,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onClearFavourites: () -> Unit,
    onBack: () -> Unit,
) {
    MiniAppScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeading(R.string.settings_appearance)

            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == settings.themeMode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(stringResource(themeLabel(mode))) },
                    )
                }
            }

            // Material You only exists from Android 12; offering a switch that
            // does nothing on older devices would be a lie.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingSwitch(
                    titleRes = R.string.settings_dynamic_color,
                    summaryRes = R.string.settings_dynamic_color_summary,
                    checked = settings.dynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeading(R.string.settings_home)
            Text(
                text = stringResource(R.string.settings_favourites_summary, MAX_FAVOURITES),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (settings.favouriteIds.isNotEmpty()) {
                TextButton(
                    onClick = onClearFavourites,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        stringResource(
                            R.string.settings_clear_favourites,
                            settings.favouriteIds.size,
                        ),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeading(R.string.settings_about)
            AboutRow(R.string.settings_version, BuildConfig.VERSION_NAME)
            AboutRow(R.string.settings_mini_apps, miniAppCount.toString())
            Text(
                text = stringResource(R.string.settings_offline_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SectionHeading(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingSwitch(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(summaryRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AboutRow(@StringRes labelRes: Int, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    UtilitiesTheme {
        SettingsScreen(
            settings = AppSettings(),
            miniAppCount = 22,
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onClearFavourites = {},
            onBack = {},
        )
    }
}
