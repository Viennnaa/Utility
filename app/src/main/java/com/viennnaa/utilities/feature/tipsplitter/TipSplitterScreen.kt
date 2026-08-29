package com.viennnaa.utilities.feature.tipsplitter

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.CountStepper
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.ResultTextStyle
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

@Composable
fun TipSplitterScreen(onBack: () -> Unit) {
    var billText by rememberSaveable { mutableStateOf("") }
    var tipPercent by rememberSaveable { mutableStateOf(15) }
    var people by rememberSaveable { mutableStateOf(2) }

    val billCents = remember(billText) { parseAmountCents(billText) }
    val split = remember(billCents, tipPercent, people) {
        billCents?.let { splitBill(it, tipPercent, people) }
    }
    // An empty field is waiting for input, not an error.
    val showError = billText.isNotBlank() && billCents == null

    MiniAppScaffold(
        title = stringResource(R.string.tip_splitter_title),
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
                split = split,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            OutlinedTextField(
                value = billText,
                onValueChange = { billText = it },
                label = { Text(stringResource(R.string.tip_splitter_bill)) },
                singleLine = true,
                isError = showError,
                supportingText = if (showError) {
                    { Text(stringResource(R.string.tip_splitter_bill_error)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            TipPicker(
                selectedPercent = tipPercent,
                onSelectPercent = { tipPercent = it },
            )

            CountStepper(
                labelRes = R.string.tip_splitter_people_heading,
                count = people,
                onCountChange = { people = clampPeople(it) },
                range = MIN_PEOPLE..MAX_PEOPLE,
            )
        }
    }
}

@Composable
private fun ResultCard(split: BillSplit?, modifier: Modifier = Modifier) {
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
            // perPerson always holds at least one share, so first() is safe.
            val highest = split?.perPerson?.first()
            Text(
                text = highest?.let { formatCents(it) }
                    ?: stringResource(R.string.tip_splitter_empty_result),
                style = ResultTextStyle.copy(fontSize = 58.sp, lineHeight = 66.sp),
                color = if (split == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (split == null) {
                    stringResource(R.string.tip_splitter_empty_hint)
                } else {
                    stringResource(R.string.tip_splitter_per_person)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (split != null) {
                // Only when the cents did not divide evenly: some people pay a
                // cent less than the headline figure.
                val lowest = split.perPerson.last()
                if (lowest != split.perPerson.first()) {
                    Text(
                        text = stringResource(
                            R.string.tip_splitter_uneven,
                            split.perPerson.count { it == lowest },
                            formatCents(lowest),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                SummaryRow(R.string.tip_splitter_summary_bill, formatCents(split.billCents))
                SummaryRow(R.string.tip_splitter_summary_tip, formatCents(split.tipCents))
                SummaryRow(R.string.tip_splitter_summary_total, formatCents(split.totalCents))
            }
        }
    }
}

@Composable
private fun SummaryRow(@StringRes labelRes: Int, value: String) {
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
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TipPicker(
    selectedPercent: Int,
    onSelectPercent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.tip_splitter_tip_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipPresets.forEach { percent ->
                FilterChip(
                    selected = selectedPercent == percent,
                    onClick = { onSelectPercent(percent) },
                    label = { Text(stringResource(R.string.tip_splitter_percent, percent)) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TipSplitterScreenPreview() {
    UtilitiesTheme { TipSplitterScreen(onBack = {}) }
}
