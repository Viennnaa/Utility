package com.viennnaa.utilities.feature.percentage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PercentageScreen(onBack: () -> Unit) {
    var modeName by rememberSaveable { mutableStateOf(PercentageMode.PERCENT_OF.name) }
    var firstText by rememberSaveable { mutableStateOf("") }
    var secondText by rememberSaveable { mutableStateOf("") }

    val mode = PercentageMode.entries.firstOrNull { it.name == modeName }
        ?: PercentageMode.PERCENT_OF
    val first = parseNumber(firstText)
    val second = parseNumber(secondText)

    val answer: Double? = if (first != null && second != null) {
        when (mode) {
            PercentageMode.PERCENT_OF -> percentOf(first, second)
            PercentageMode.WHAT_PERCENT -> whatPercent(first, second)
            PercentageMode.CHANGE -> percentChange(first, second)
        }
    } else {
        null
    }

    // Both numbers are present but the maths has no answer: dividing by zero.
    val undefined = first != null && second != null && answer == null

    MiniAppScaffold(
        title = stringResource(R.string.percentage_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                        text = when {
                            answer != null && mode == PercentageMode.PERCENT_OF ->
                                formatNumber(answer)

                            answer != null -> "${formatNumber(answer)}%"
                            else -> "—"
                        },
                        fontSize = 52.sp,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = when {
                            undefined -> stringResource(undefinedHint(mode))
                            answer != null -> stringResource(resultHint(mode))
                            else -> stringResource(R.string.percentage_empty_hint)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (undefined) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PercentageMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = { modeName = option.name },
                        label = { Text(stringResource(modeLabel(option))) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = firstText,
                    onValueChange = { firstText = it },
                    labelRes = firstLabel(mode),
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = secondText,
                    onValueChange = { secondText = it },
                    labelRes = secondLabel(mode),
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        modifier = modifier,
    )
}

private fun modeLabel(mode: PercentageMode): Int = when (mode) {
    PercentageMode.PERCENT_OF -> R.string.percentage_mode_of
    PercentageMode.WHAT_PERCENT -> R.string.percentage_mode_what
    PercentageMode.CHANGE -> R.string.percentage_mode_change
}

private fun firstLabel(mode: PercentageMode): Int = when (mode) {
    PercentageMode.PERCENT_OF -> R.string.percentage_field_percent
    PercentageMode.WHAT_PERCENT -> R.string.percentage_field_part
    PercentageMode.CHANGE -> R.string.percentage_field_from
}

private fun secondLabel(mode: PercentageMode): Int = when (mode) {
    PercentageMode.PERCENT_OF -> R.string.percentage_field_value
    PercentageMode.WHAT_PERCENT -> R.string.percentage_field_whole
    PercentageMode.CHANGE -> R.string.percentage_field_to
}

private fun resultHint(mode: PercentageMode): Int = when (mode) {
    PercentageMode.PERCENT_OF -> R.string.percentage_result_of
    PercentageMode.WHAT_PERCENT -> R.string.percentage_result_what
    PercentageMode.CHANGE -> R.string.percentage_result_change
}

private fun undefinedHint(mode: PercentageMode): Int = when (mode) {
    PercentageMode.WHAT_PERCENT -> R.string.percentage_undefined_whole
    else -> R.string.percentage_undefined_from
}

@Preview(showBackground = true)
@Composable
private fun PercentageScreenPreview() {
    UtilitiesTheme { PercentageScreen(onBack = {}) }
}
