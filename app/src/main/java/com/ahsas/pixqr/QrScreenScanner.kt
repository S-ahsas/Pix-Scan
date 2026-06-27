package com.ahsas.pixqr

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var lastScanTime by remember { mutableStateOf(0L) }
    var isTorchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var zoomLevel by remember { mutableStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }
    val boxWidthRef = remember { mutableStateOf(0f) }
    val boxHeightRef = remember { mutableStateOf(0f) }
    val screenWidthRef = remember { mutableStateOf(0f) }
    val screenHeightRef = remember { mutableStateOf(0f) }

    // Box state — starts as square, resets each session
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }
    var boxWidthPx by remember { mutableStateOf(0f) }
    var boxHeightPx by remember { mutableStateOf(0f) }

// Keep refs in sync
    LaunchedEffect(screenWidth, screenHeight, boxWidthPx, boxHeightPx) {
        screenWidthRef.value = screenWidth
        screenHeightRef.value = screenHeight
        boxWidthRef.value = boxWidthPx
        boxHeightRef.value = boxHeightPx
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraExecutor.shutdown()
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                future.get().unbindAll()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageProxy(
                                    imageProxy = imageProxy,
                                    boxWidthFraction = if (screenWidthRef.value > 0f) boxWidthRef.value / screenWidthRef.value else 0.7f,
                                    boxHeightFraction = if (screenHeightRef.value > 0f) boxHeightRef.value / screenHeightRef.value else 0.7f
                                ) { result ->
                                    val now = System.currentTimeMillis()
                                    if (now - lastScanTime > 2000L) {
                                        lastScanTime = now
                                        onQrCodeScanned(result)
                                    }
                                }
                            }
                        }

                    try {
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Binding failed", e)
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Scanning overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    screenWidth = size.width.toFloat()
                    screenHeight = size.height.toFloat()
                    if (!initialized && size.width > 0) {
                        // Start as square: 70% of screen width
                        val initialSize = size.width * 0.7f
                        boxWidthPx = initialSize
                        boxHeightPx = initialSize
                        initialized = true
                    }
                }
        ) {
            if (boxWidthPx == 0f) return@Canvas

            val left = (size.width - boxWidthPx) / 2
            val top = (size.height - boxHeightPx) / 2

            drawRect(color = Color.Black.copy(alpha = 0.5f))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidthPx, boxHeightPx),
                cornerRadius = CornerRadius(24.dp.toPx()),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(boxWidthPx, boxHeightPx),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Drag handle — bottom right corner of box
        if (initialized && screenWidth > 0f) {
            val left = (screenWidth - boxWidthPx) / 2
            val top = (screenHeight - boxHeightPx) / 2
            val handleX = left + boxWidthPx
            val handleY = top + boxHeightPx

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (handleX - 24.dp.toPx()).toInt(),
                            y = (handleY - 24.dp.toPx()).toInt()
                        )
                    }
                    .size(48.dp)
                    .pointerInput(screenWidth, screenHeight) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Expand/shrink symmetrically from center
                            val newWidth = (boxWidthPx + dragAmount.x * 2)
                                .coerceIn(screenWidth * 0.2f, screenWidth * 0.95f)
                            val newHeight = (boxHeightPx + dragAmount.y * 2)
                                .coerceIn(screenHeight * 0.15f, screenHeight * 0.85f)
                            boxWidthPx = newWidth
                            boxHeightPx = newHeight
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.OpenInFull,
                    contentDescription = "Resize",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom controls — zoom slider + flashlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Rounded.ZoomOut,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Slider(
                    value = zoomLevel,
                    onValueChange = { newZoom ->
                        zoomLevel = newZoom
                        val minZoom = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                        val maxZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 4f
                        val actualZoom = minZoom + (maxZoom - minZoom) * newZoom
                        camera?.cameraControl?.setZoomRatio(actualZoom)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Icon(
                    Icons.Rounded.ZoomIn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                FloatingActionButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        camera?.cameraControl?.enableTorch(isTorchOn)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(48.dp),
                    containerColor = if (isTorchOn)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    contentColor = if (isTorchOn)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        contentDescription = "Toggle flashlight",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    boxWidthFraction: Float,
    boxHeightFraction: Float,
    onResult: (String) -> Unit
) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient()

    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            val imageWidth = mediaImage.width.toFloat()
            val imageHeight = mediaImage.height.toFloat()

            // Calculate box boundaries in image coordinates
            val boxLeft = imageWidth * (1f - boxWidthFraction) / 2f
            val boxRight = imageWidth * (1f + boxWidthFraction) / 2f
            val boxTop = imageHeight * (1f - boxHeightFraction) / 2f
            val boxBottom = imageHeight * (1f + boxHeightFraction) / 2f

            barcodes
                .filter { barcode ->
                    val bounds = barcode.boundingBox ?: return@filter true
                    // Check if barcode center is within the scan box
                    val centerX = bounds.exactCenterX()
                    val centerY = bounds.exactCenterY()
                    centerX >= boxLeft && centerX <= boxRight &&
                            centerY >= boxTop && centerY <= boxBottom
                }
                .firstOrNull { it.valueType == Barcode.TYPE_URL || it.rawValue != null }
                ?.rawValue
                ?.let { onResult(it) }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}