package com.viennnaa.utilities.feature.datecalc

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
import androidx.compose.runtime.remember
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
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateCalcScreen(onBack: () -> Unit) {
    // Read once per composition entry rather than per recomposition, so the
    // defaults do not shift under the user if the day rolls over mid session.
    val today = remember { LocalDate.now() }

    var modeName by rememberSaveable { mutableStateOf(DateMode.BETWEEN.name) }
    var startText by rememberSaveable { mutableStateOf(today.toString()) }
    var endText by rememberSaveable { mutableStateOf(today.toString()) }
    var daysText by rememberSaveable { mutableStateOf("30") }

    val mode = DateMode.entries.firstOrNull { it.name == modeName } ?: DateMode.BETWEEN
    val start = parseDate(startText)
    val end = parseDate(endText)
    val days = daysText.trim().toLongOrNull()

    MiniAppScaffold(
        title = stringResource(R.string.date_calc_title),
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
            ResultCard(
                mode = mode,
                start = start,
                end = end,
                days = days,
                today = today,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = { modeName = option.name },
                        label = { Text(stringResource(modeLabel(option))) },
                    )
                }
            }

            when (mode) {
                DateMode.BETWEEN -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(startText, { startText = it }, R.string.date_calc_from, start == null, Modifier.weight(1f))
                    DateField(endText, { endText = it }, R.string.date_calc_to, end == null, Modifier.weight(1f))
                }

                DateMode.ADD -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(startText, { startText = it }, R.string.date_calc_date, start == null, Modifier.weight(1f))
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it },
                        label = { Text(stringResource(R.string.date_calc_days)) },
                        singleLine = true,
                        isError = daysText.isNotBlank() && days == null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                DateMode.AGE -> DateField(
                    startText,
                    { startText = it },
                    R.string.date_calc_birth,
                    start == null,
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        placeholder = { Text(stringResource(R.string.date_calc_format)) },
        singleLine = true,
        isError = isError && value.isNotBlank(),
        supportingText = if (isError && value.isNotBlank()) {
            { Text(stringResource(R.string.date_calc_format_error)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier,
    )
}

@Composable
private fun ResultCard(
    mode: DateMode,
    start: LocalDate?,
    end: LocalDate?,
    days: Long?,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val headline: String?
    val detail: String
    when (mode) {
        DateMode.BETWEEN -> {
            if (start != null && end != null) {
                val count = daysBetween(start, end)
                val magnitude = if (count < 0) -count else count
                val (weeks, leftover) = weeksAndDays(count)
                headline = stringResource(R.string.date_calc_days_count, magnitude)
                detail = stringResource(R.string.date_calc_weeks_detail, weeks, leftover)
            } else {
                headline = null
                detail = stringResource(R.string.date_calc_between_hint)
            }
        }

        DateMode.ADD -> {
            if (start != null && days != null) {
                val landing = addDays(start, days)
                headline = landing.toString()
                detail = stringResource(R.string.date_calc_weekday, landing.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() })
            } else {
                headline = null
                detail = stringResource(R.string.date_calc_add_hint)
            }
        }

        DateMode.AGE -> {
            val age = start?.let { ageOn(it, today) }
            if (age != null) {
                headline = stringResource(R.string.date_calc_years, age.years)
                detail = stringResource(R.string.date_calc_age_detail, age.months, age.days)
            } else {
                headline = null
                detail = if (start != null) {
                    stringResource(R.string.date_calc_future_birth)
                } else {
                    stringResource(R.string.date_calc_age_hint)
                }
            }
        }
    }

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
                text = headline ?: "—",
                fontSize = 40.sp,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun modeLabel(mode: DateMode): Int = when (mode) {
    DateMode.BETWEEN -> R.string.date_calc_mode_between
    DateMode.ADD -> R.string.date_calc_mode_add
    DateMode.AGE -> R.string.date_calc_mode_age
}

@Preview(showBackground = true)
@Composable
private fun DateCalcScreenPreview() {
    UtilitiesTheme { DateCalcScreen(onBack = {}) }
}
