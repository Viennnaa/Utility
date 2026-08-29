package com.viennnaa.utilities.feature.flashlight

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme

/** The first camera that has a flash, or null on a device without one. */
private fun torchCameraId(manager: CameraManager?): String? = try {
    manager?.cameraIdList?.firstOrNull { id ->
        manager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }
} catch (e: CameraAccessException) {
    null
}

@Composable
fun FlashlightScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val manager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }
    val cameraId = remember(manager) { torchCameraId(manager) }

    var isOn by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    // The torch is shared with the rest of the system, so follow what it
    // actually does rather than assuming our own writes stuck.
    DisposableEffect(manager, cameraId) {
        if (manager == null || cameraId == null) return@DisposableEffect onDispose {}
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) isOn = enabled
            }

            override fun onTorchModeUnavailable(id: String) {
                if (id == cameraId) isOn = false
            }
        }
        manager.registerTorchCallback(callback, null)
        onDispose {
            manager.unregisterTorchCallback(callback)
            // Leaving the screen with the torch on would leave no way to turn it
            // off short of reopening the app, so it goes out with the screen.
            try {
                manager.setTorchMode(cameraId, false)
            } catch (e: CameraAccessException) {
                // Nothing useful to do while tearing down.
            }
        }
    }

    fun toggle() {
        val id = cameraId ?: return
        val target = !isOn
        try {
            manager?.setTorchMode(id, target)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            failed = false
        } catch (e: CameraAccessException) {
            // Another app can hold the camera; say so rather than looking stuck.
            failed = true
        }
    }

    MiniAppScaffold(
        title = stringResource(R.string.flashlight_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val available = cameraId != null
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOn) {
                            Color(0xFFFFC107)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .clickable(
                        enabled = available,
                        role = Role.Switch,
                        onClick = { toggle() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = if (isOn) "🔦" else "💡", fontSize = 72.sp)
            }

            Text(
                text = when {
                    !available -> stringResource(R.string.flashlight_unavailable)
                    failed -> stringResource(R.string.flashlight_failed)
                    isOn -> stringResource(R.string.flashlight_on)
                    else -> stringResource(R.string.flashlight_off)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (failed || !available) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FlashlightScreenPreview() {
    UtilitiesTheme { FlashlightScreen(onBack = {}) }
}
