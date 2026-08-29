package com.viennnaa.utilities.feature.wifiqr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.core.qr.encodeQr
import com.viennnaa.utilities.core.qr.matrixToPixels
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WifiQrScreen(onBack: () -> Unit) {
    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var securityName by rememberSaveable { mutableStateOf(WifiSecurity.WPA.name) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val security = WifiSecurity.entries.firstOrNull { it.name == securityName } ?: WifiSecurity.WPA
    val payload = wifiPayload(ssid, password, security, hidden)
    val missingPassword = ssid.isNotEmpty() && needsPassword(security, password)

    // Nothing here is stored: a network password is not a setting to remember,
    // and writing it to disk would outlive the reason it was typed.
    val bitmap = remember(payload) {
        if (payload == null || missingPassword) {
            null
        } else {
            encodeQr(payload, size = 1)?.let { matrix ->
                val pixels = matrixToPixels(
                    matrix,
                    on = android.graphics.Color.BLACK,
                    off = android.graphics.Color.WHITE,
                )
                Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.wifi_qr_title),
        onBack = onBack,
        actions = {
            if (ssid.isNotEmpty() || password.isNotEmpty()) {
                TextButton(
                    onClick = {
                        ssid = ""
                        password = ""
                    },
                ) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.cd_wifi_qr_code),
                        filterQuality = FilterQuality.None,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp),
                    )
                } else {
                    Text(
                        text = stringResource(
                            when {
                                missingPassword -> R.string.wifi_qr_needs_password
                                ssid.isEmpty() -> R.string.wifi_qr_empty_hint
                                else -> R.string.wifi_qr_too_long
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            OutlinedTextField(
                value = ssid,
                onValueChange = { if (it.length <= MAX_SSID_LENGTH) ssid = it },
                label = { Text(stringResource(R.string.wifi_qr_ssid)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            if (security != WifiSecurity.NONE) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { if (it.length <= MAX_PASSWORD_LENGTH) password = it },
                    label = { Text(stringResource(R.string.wifi_qr_password)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                stringResource(
                                    if (passwordVisible) {
                                        R.string.wifi_qr_hide
                                    } else {
                                        R.string.wifi_qr_show
                                    },
                                ),
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.wifi_qr_security),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WifiSecurity.entries.forEach { option ->
                        FilterChip(
                            selected = option == security,
                            onClick = { securityName = option.name },
                            label = { Text(stringResource(securityLabel(option))) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.wifi_qr_hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = hidden, onCheckedChange = { hidden = it })
            }

            Text(
                text = stringResource(R.string.wifi_qr_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun securityLabel(security: WifiSecurity): Int = when (security) {
    WifiSecurity.WPA -> R.string.wifi_qr_wpa
    WifiSecurity.WEP -> R.string.wifi_qr_wep
    WifiSecurity.NONE -> R.string.wifi_qr_open
}

@Preview(showBackground = true)
@Composable
private fun WifiQrScreenPreview() {
    UtilitiesTheme { WifiQrScreen(onBack = {}) }
}
