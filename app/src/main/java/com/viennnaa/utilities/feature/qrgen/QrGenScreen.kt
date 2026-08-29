package com.viennnaa.utilities.feature.qrgen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QrGenScreen(onBack: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var correctionName by rememberSaveable { mutableStateOf(Correction.MEDIUM.name) }
    val correction = Correction.entries.firstOrNull { it.name == correctionName } ?: Correction.MEDIUM

    // Encoded at its natural module size and scaled up with no filtering, so the
    // modules stay square-edged instead of being blurred by interpolation.
    val bitmap = remember(text, correction) {
        encodeQr(text, size = 1, correction = correction)?.let { matrix ->
            val pixels = matrixToPixels(
                matrix,
                on = android.graphics.Color.BLACK,
                off = android.graphics.Color.WHITE,
            )
            Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }
    }
    val tooDense = text.isNotEmpty() && bitmap == null

    MiniAppScaffold(
        title = stringResource(R.string.qr_gen_title),
        onBack = onBack,
        actions = {
            if (text.isNotEmpty()) {
                TextButton(onClick = { text = "" }) {
                    Text(stringResource(R.string.action_clear))
                }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.cd_qr_gen_code),
                        filterQuality = FilterQuality.None,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp),
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (tooDense) {
                                R.string.qr_gen_too_long
                            } else {
                                R.string.qr_gen_empty_hint
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (tooDense) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= MAX_CONTENT_LENGTH) text = it },
                label = { Text(stringResource(R.string.qr_gen_input)) },
                placeholder = { Text(stringResource(R.string.qr_gen_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.qr_gen_correction_heading),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Correction.entries.forEach { option ->
                        FilterChip(
                            selected = option == correction,
                            onClick = { correctionName = option.name },
                            label = { Text(stringResource(correctionLabel(option))) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.qr_gen_correction_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun correctionLabel(correction: Correction): Int = when (correction) {
    Correction.LOW -> R.string.qr_gen_correction_low
    Correction.MEDIUM -> R.string.qr_gen_correction_medium
    Correction.QUARTILE -> R.string.qr_gen_correction_quartile
    Correction.HIGH -> R.string.qr_gen_correction_high
}

@Preview(showBackground = true)
@Composable
private fun QrGenScreenPreview() {
    UtilitiesTheme { QrGenScreen(onBack = {}) }
}
