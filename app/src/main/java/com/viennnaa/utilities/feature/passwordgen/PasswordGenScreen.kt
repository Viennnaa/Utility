package com.viennnaa.utilities.feature.passwordgen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.storage.rememberMiniAppPreferences
import com.viennnaa.utilities.miniapp.MiniAppIds
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

private const val KEY_LENGTH = "length"
private const val KEY_SETS = "sets"

@Composable
fun PasswordGenScreen(onBack: () -> Unit) {
    var length by rememberSaveable { mutableStateOf(DEFAULT_LENGTH) }
    var setNames by rememberSaveable { mutableStateOf(PasswordSpec().sets.map { it.name }) }
    var password by rememberSaveable { mutableStateOf<String?>(null) }

    val clipboard = LocalClipboardManager.current
    val sets = setNames.mapNotNull { name ->
        CharacterSet.entries.firstOrNull { it.name == name }
    }.toSet()
    val spec = PasswordSpec(length = length, sets = sets)

    val prefs = rememberMiniAppPreferences(MiniAppIds.PASSWORD_GEN)
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restored) {
            length = clampLength(prefs.getInt(KEY_LENGTH, length))
            val saved = prefs.getStringList(KEY_SETS)
            if (saved.isNotEmpty()) setNames = saved
            restored = true
        }
    }
    LaunchedEffect(restored, length, setNames) {
        if (restored) {
            prefs.setInt(KEY_LENGTH, length)
            prefs.setStringList(KEY_SETS, setNames)
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.password_gen_title),
        onBack = onBack,
        actions = {
            if (password != null) {
                TextButton(
                    onClick = { password?.let { clipboard.setText(AnnotatedString(it)) } },
                ) {
                    Text(stringResource(R.string.action_copy))
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
                password = password,
                spec = spec,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Column {
                Text(
                    text = stringResource(R.string.password_gen_length, length),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = length.toFloat(),
                    onValueChange = { length = clampLength(it.toInt()) },
                    valueRange = MIN_LENGTH.toFloat()..MAX_LENGTH.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            CharacterSetPicker(
                selected = sets,
                onToggle = { set ->
                    setNames = if (set.name in setNames) {
                        setNames - set.name
                    } else {
                        setNames + set.name
                    }
                },
            )

            Button(
                onClick = { password = generatePassword(spec) },
                enabled = sets.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (password == null) {
                            R.string.password_gen_generate
                        } else {
                            R.string.password_gen_generate_again
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(password: String?, spec: PasswordSpec, modifier: Modifier = Modifier) {
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
            Text(
                text = password ?: stringResource(R.string.password_gen_empty_hint),
                // Monospaced so l, 1, O and 0 can be told apart when copying by eye.
                fontFamily = if (password != null) FontFamily.Monospace else FontFamily.Default,
                fontSize = if (password != null) 22.sp else 16.sp,
                textAlign = TextAlign.Center,
                color = if (password == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (spec.sets.isNotEmpty()) {
                Text(
                    text = stringResource(
                        strengthLabel(strengthOf(spec)),
                        entropyBits(spec).toInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterSetPicker(
    selected: Set<CharacterSet>,
    onToggle: (CharacterSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.password_gen_sets_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CharacterSet.entries.forEach { set ->
                FilterChip(
                    selected = set in selected,
                    onClick = { onToggle(set) },
                    label = { Text(stringResource(setLabel(set))) },
                )
            }
        }
        if (selected.isEmpty()) {
            Row {
                Text(
                    text = stringResource(R.string.password_gen_no_sets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun setLabel(set: CharacterSet): Int = when (set) {
    CharacterSet.LOWERCASE -> R.string.password_gen_set_lower
    CharacterSet.UPPERCASE -> R.string.password_gen_set_upper
    CharacterSet.DIGITS -> R.string.password_gen_set_digits
    CharacterSet.SYMBOLS -> R.string.password_gen_set_symbols
}

private fun strengthLabel(strength: Strength): Int = when (strength) {
    Strength.WEAK -> R.string.password_gen_strength_weak
    Strength.FAIR -> R.string.password_gen_strength_fair
    Strength.STRONG -> R.string.password_gen_strength_strong
    Strength.EXCELLENT -> R.string.password_gen_strength_excellent
}

@Preview(showBackground = true)
@Composable
private fun PasswordGenScreenPreview() {
    UtilitiesTheme { PasswordGenScreen(onBack = {}) }
}
