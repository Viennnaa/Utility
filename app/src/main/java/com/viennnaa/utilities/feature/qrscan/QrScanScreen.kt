package com.viennnaa.utilities.feature.qrscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.viennnaa.utilities.R
import com.viennnaa.utilities.ui.components.MiniAppScaffold
import com.viennnaa.utilities.ui.theme.UtilitiesTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var denied by remember { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        denied = !allowed
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Analysis runs off the main thread; the executor outlives individual frames
    // so it is created once and shut down with the screen.
    val analysisExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    MiniAppScaffold(
        title = stringResource(R.string.qr_scan_title),
        onBack = onBack,
        actions = {
            if (result != null) {
                OutlinedButton(
                    onClick = { result = null },
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text(stringResource(R.string.qr_scan_again))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !granted -> Text(
                        text = stringResource(
                            if (denied) R.string.qr_scan_denied else R.string.qr_scan_needs_camera,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    else -> CameraPreview(
                        // Scanning stops once something is found, so the reader
                        // is not fighting the user while they read the result.
                        paused = result != null,
                        onScanned = { text -> if (result == null) result = text },
                        executor = analysisExecutor,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                    )
                }
            }

            result?.let { scanned ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = scanned,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val kind = kindOf(scanned)
                            if (kind != ScanKind.TEXT) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scanned.trim()))
                                        // No app may be able to handle it; that
                                        // is not worth crashing over.
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(stringResource(openLabel(kind)))
                                }
                            }
                            OutlinedButton(
                                onClick = { clipboard.setText(AnnotatedString(scanned)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                Text(stringResource(R.string.action_copy))
                            }
                        }
                    }
                }
            }

            if (granted && result == null) {
                Text(
                    text = stringResource(R.string.qr_scan_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    paused: Boolean,
    onScanned: (String) -> Unit,
    executor: ExecutorService,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reader = remember { newReader() }
    // The analyzer callback outlives recomposition, so it reads the latest
    // value through this rather than capturing the first one it saw.
    val isPaused = rememberUpdatedState(paused)

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val previewView = PreviewView(viewContext)
            val providerFuture = ProcessCameraProvider.getInstance(viewContext)
            providerFuture.addListener(
                {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        // Only the newest frame matters; queuing them would make
                        // the scanner lag behind what the camera is pointing at.
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { proxy ->
                        try {
                            if (!isPaused.value) {
                                val plane = proxy.planes[0]
                                val buffer = plane.buffer
                                val data = ByteArray(buffer.remaining())
                                buffer.get(data)
                                decodeLuminance(
                                    reader = reader,
                                    data = data,
                                    width = proxy.width,
                                    height = proxy.height,
                                    rowStride = plane.rowStride,
                                )?.let { text ->
                                    previewView.post { onScanned(text) }
                                }
                            }
                        } finally {
                            // Not closing stalls the whole analysis pipeline.
                            proxy.close()
                        }
                    }
                    runCatching {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }
                },
                ContextCompat.getMainExecutor(viewContext),
            )
            previewView
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }
}

private fun openLabel(kind: ScanKind): Int = when (kind) {
    ScanKind.URL -> R.string.qr_scan_open_link
    ScanKind.EMAIL -> R.string.qr_scan_open_email
    ScanKind.PHONE -> R.string.qr_scan_open_phone
    ScanKind.TEXT -> R.string.action_copy
}

@ComposePreview(showBackground = true)
@Composable
private fun QrScanScreenPreview() {
    UtilitiesTheme { QrScanScreen(onBack = {}) }
}
