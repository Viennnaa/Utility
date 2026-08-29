package com.viennnaa.utilities.feature.discount

import androidx.annotation.StringRes
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.money.BASIS_POINTS_PER_PERCENT
import com.viennnaa.utilities.core.money.formatBasisPoints
import com.viennnaa.utilities.core.money.formatCents
import com.viennnaa.utilities.core.money.parseAmountCents
import com.viennnaa.utilities.core.money.parsePercentBasisPoints
import com.viennnaa.utilities.core.storage.rememberMiniAppPreferences
import com.viennnaa.utilities.miniapp.MiniAppIds
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

private const val KEY_MODE = "mode"
private const val KEY_RATE = "rate"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscountScreen(onBack: () -> Unit) {
    var modeName by rememberSaveable { mutableStateOf(PriceMode.DISCOUNT.name) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var rateText by rememberSaveable { mutableStateOf("20") }

    val mode = PriceMode.entries.firstOrNull { it.name == modeName } ?: PriceMode.DISCOUNT
    val amount = parseAmountCents(amountText)
    val rate = parsePercentBasisPoints(rateText, MAX_RATE_PERCENT)
    val result = remember(mode, amount, rate) {
        if (amount != null && rate != null) priceFor(mode, amount, rate) else null
    }
    val amountError = amountText.isNotBlank() && amount == null
    val rateError = rateText.isNotBlank() && rate == null

    val prefs = rememberMiniAppPreferences(MiniAppIds.DISCOUNT)
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restored) {
            modeName = prefs.getString(KEY_MODE, modeName)
            rateText = prefs.getString(KEY_RATE, rateText)
            restored = true
        }
    }
    LaunchedEffect(restored, modeName, rateText) {
        if (restored) {
            prefs.setString(KEY_MODE, modeName)
            prefs.setString(KEY_RATE, rateText)
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.discount_title),
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
                result = result,
                rate = rate,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriceMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = { modeName = option.name },
                        label = { Text(stringResource(modeLabel(option))) },
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(amountLabel(mode))) },
                singleLine = true,
                isError = amountError,
                supportingText = if (amountError) {
                    { Text(stringResource(R.string.discount_amount_error)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(rateLabel(mode)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatePresets.forEach { preset ->
                        FilterChip(
                            selected = rate == preset * BASIS_POINTS_PER_PERCENT,
                            onClick = { rateText = preset.toString() },
                            label = { Text(stringResource(R.string.discount_rate_chip, preset)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text(stringResource(R.string.discount_rate_custom)) },
                    singleLine = true,
                    isError = rateError,
                    supportingText = if (rateError) {
                        { Text(stringResource(R.string.discount_rate_error)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    mode: PriceMode,
    result: PriceResult?,
    rate: Int?,
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
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // The headline is what the user is actually after: what they pay
            // for a discount, and the net when stripping tax back out.
            val headline = when {
                result == null -> null
                mode == PriceMode.REMOVE_TAX -> result.base
                else -> result.result
            }
            Text(
                text = headline?.let { formatCents(it) } ?: "—",
                fontSize = 52.sp,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (result == null) {
                    stringResource(R.string.discount_empty_hint)
                } else {
                    stringResource(headlineLabel(mode))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (result != null && rate != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                when (mode) {
                    PriceMode.DISCOUNT -> {
                        SummaryRow(R.string.discount_row_original, formatCents(result.base))
                        SummaryRow(
                            R.string.discount_row_saved,
                            formatCents(result.adjustment),
                            emphasis = true,
                        )
                    }

                    PriceMode.ADD_TAX -> {
                        SummaryRow(R.string.discount_row_net, formatCents(result.base))
                        SummaryRow(R.string.discount_row_tax, formatCents(result.adjustment))
                    }

                    PriceMode.REMOVE_TAX -> {
                        SummaryRow(R.string.discount_row_gross, formatCents(result.result))
                        SummaryRow(R.string.discount_row_tax, formatCents(result.adjustment))
                    }
                }
                Text(
                    text = stringResource(
                        R.string.discount_rate_applied,
                        formatBasisPoints(rate),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(@StringRes labelRes: Int, value: String, emphasis: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
            color = if (emphasis) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private fun modeLabel(mode: PriceMode): Int = when (mode) {
    PriceMode.DISCOUNT -> R.string.discount_mode_discount
    PriceMode.ADD_TAX -> R.string.discount_mode_add_tax
    PriceMode.REMOVE_TAX -> R.string.discount_mode_remove_tax
}

private fun amountLabel(mode: PriceMode): Int = when (mode) {
    PriceMode.DISCOUNT -> R.string.discount_amount_price
    PriceMode.ADD_TAX -> R.string.discount_amount_net
    PriceMode.REMOVE_TAX -> R.string.discount_amount_gross
}

private fun rateLabel(mode: PriceMode): Int = when (mode) {
    PriceMode.DISCOUNT -> R.string.discount_rate_off
    else -> R.string.discount_rate_tax
}

private fun headlineLabel(mode: PriceMode): Int = when (mode) {
    PriceMode.DISCOUNT -> R.string.discount_headline_pay
    PriceMode.ADD_TAX -> R.string.discount_headline_total
    PriceMode.REMOVE_TAX -> R.string.discount_headline_net
}

@Preview(showBackground = true)
@Composable
private fun DiscountScreenPreview() {
    UtilitiesTheme { DiscountScreen(onBack = {}) }
}
