package com.viennnaa.utilities.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R

/**
 * A labelled "fewer / more" control for the small whole numbers mini apps ask
 * for — how many dice, how many teams, how many people are splitting the bill.
 *
 * [onCountChange] only fires with values inside [range]; the buttons disable at
 * each end rather than reporting a value the caller would have to clamp.
 */
@Composable
fun CountStepper(
    @StringRes labelRes: Int,
    count: Int,
    onCountChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val label = stringResource(labelRes)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onCountChange(count - 1) },
            enabled = enabled && count > range.first,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_stepper_decrease, label),
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp),
        )
        IconButton(
            onClick = { onCountChange(count + 1) },
            enabled = enabled && count < range.last,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.cd_stepper_increase, label),
            )
        }
    }
}
