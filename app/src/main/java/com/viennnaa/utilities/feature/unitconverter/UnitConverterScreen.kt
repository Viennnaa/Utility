package com.viennnaa.utilities.feature.unitconverter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.viennnaa.utilities.core.storage.rememberMiniAppPreferences
import com.viennnaa.utilities.miniapp.MiniAppIds
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

private const val KEY_CATEGORY = "category"
private const val KEY_FROM = "from"
private const val KEY_TO = "to"

@Composable
fun UnitConverterScreen(onBack: () -> Unit) {
    var categoryName by rememberSaveable { mutableStateOf(MeasureCategory.LENGTH.name) }
    var fromId by rememberSaveable { mutableStateOf("km") }
    var toId by rememberSaveable { mutableStateOf("mi") }
    var valueText by rememberSaveable { mutableStateOf("1") }

    val category = MeasureCategory.entries.firstOrNull { it.name == categoryName }
        ?: MeasureCategory.LENGTH
    val units = unitsIn(category)
    // Fall back within the category, so a saved unit from another category or a
    // renamed id cannot leave the screen without a selection.
    val from = unitById(fromId)?.takeIf { it.category == category } ?: units.first()
    val to = unitById(toId)?.takeIf { it.category == category } ?: units.last()

    val value = parseValue(valueText)
    val result = value?.let { convert(it, from, to) }
    val showError = valueText.isNotBlank() && value == null

    val prefs = rememberMiniAppPreferences(MiniAppIds.UNIT_CONVERTER)
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restored) {
            categoryName = prefs.getString(KEY_CATEGORY, categoryName)
            fromId = prefs.getString(KEY_FROM, fromId)
            toId = prefs.getString(KEY_TO, toId)
            restored = true
        }
    }
    LaunchedEffect(restored, categoryName, fromId, toId) {
        if (restored) {
            prefs.setString(KEY_CATEGORY, categoryName)
            prefs.setString(KEY_FROM, fromId)
            prefs.setString(KEY_TO, toId)
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.unit_converter_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ResultCard(
                result = result,
                from = from,
                to = to,
                value = value,
                modifier = Modifier.fillMaxWidth(),
            )

            ChipRow(
                headingRes = R.string.unit_converter_category_heading,
                labels = MeasureCategory.entries.map { stringResource(categoryLabel(it)) },
                selectedIndex = MeasureCategory.entries.indexOf(category),
                onSelect = { index ->
                    val next = MeasureCategory.entries[index]
                    categoryName = next.name
                    // Units do not carry across categories, so pick fresh ends.
                    val nextUnits = unitsIn(next)
                    fromId = nextUnits.first().id
                    toId = nextUnits.last().id
                },
            )

            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text(stringResource(R.string.unit_converter_value)) },
                singleLine = true,
                isError = showError,
                supportingText = if (showError) {
                    { Text(stringResource(R.string.unit_converter_value_error)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            ChipRow(
                headingRes = R.string.unit_converter_from,
                labels = units.map { it.symbol },
                selectedIndex = units.indexOf(from),
                onSelect = { fromId = units[it].id },
            )
            ChipRow(
                headingRes = R.string.unit_converter_to,
                labels = units.map { it.symbol },
                selectedIndex = units.indexOf(to),
                onSelect = { toId = units[it].id },
            )
        }
    }
}

@Composable
private fun ResultCard(
    result: Double?,
    from: MeasureUnit,
    to: MeasureUnit,
    value: Double?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (result != null) "${formatValue(result)} ${to.symbol}" else "—",
                fontSize = 40.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (value != null) {
                    stringResource(
                        R.string.unit_converter_summary,
                        formatValue(value),
                        from.symbol,
                    )
                } else {
                    stringResource(R.string.unit_converter_empty_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    headingRes: Int,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(headingRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { index, label ->
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    label = { Text(label) },
                )
            }
        }
    }
}

private fun categoryLabel(category: MeasureCategory): Int = when (category) {
    MeasureCategory.LENGTH -> R.string.unit_converter_length
    MeasureCategory.MASS -> R.string.unit_converter_mass
    MeasureCategory.TEMPERATURE -> R.string.unit_converter_temperature
    MeasureCategory.VOLUME -> R.string.unit_converter_volume
}

@Preview(showBackground = true)
@Composable
private fun UnitConverterScreenPreview() {
    UtilitiesTheme { UnitConverterScreen(onBack = {}) }
}
