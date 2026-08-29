package com.viennnaa.utilities.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.options.AddRejection
import com.viennnaa.utilities.core.options.MAX_OPTIONS

/**
 * The list-building control shared by mini apps that collect short text entries:
 * a chip per entry, tap to remove, and a field that adds one on Done or Add.
 *
 * The caller owns the list and applies `core.options` rules to it; this only
 * renders and reports intent.
 *
 * @param labelRes label for the input field, so each mini app can ask for what it
 *   actually wants ("Add an option", "Add a name").
 * @param rejection why [draft] cannot be added, from `rejectionFor`. Blank is not
 *   surfaced — an empty field simply leaves the Add button disabled.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OptionEditor(
    options: List<String>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    rejection: AddRejection?,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (options.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .heightIn(max = 132.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEachIndexed { index, option ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(index) },
                        enabled = enabled,
                        label = { Text(option) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(
                                    R.string.cd_options_remove,
                                    option,
                                ),
                            )
                        },
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = { Text(stringResource(labelRes)) },
                singleLine = true,
                enabled = enabled,
                isError = rejection != null && rejection != AddRejection.Blank,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAdd, enabled = enabled && rejection == null) {
                Text(stringResource(R.string.options_add))
            }
        }

        val errorRes = when (rejection) {
            AddRejection.Duplicate -> R.string.options_error_duplicate
            AddRejection.Full -> R.string.options_error_full
            AddRejection.Blank, null -> null
        }
        if (errorRes != null) {
            Text(
                text = stringResource(errorRes, MAX_OPTIONS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
