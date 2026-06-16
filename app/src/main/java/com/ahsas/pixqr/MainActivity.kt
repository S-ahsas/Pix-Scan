package com.ahsas.pixqr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ahsas.pixqr.ui.theme.PixQRTheme
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixQRTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var hasCameraPermission by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = (context.applicationContext as App).database
    val dao = db.scanDao()
    val scope = rememberCoroutineScope()
    var selectedScan by remember { mutableStateOf<ScanRecord?>(null) }
    var imageScanResult by remember { mutableStateOf<String?>(null) }
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scanImageForQr(context, it) { result ->
                if (result != null) {
                    imageScanResult = result
                    scope.launch {
                        val existing = dao.findByContent(result)
                        if (existing != null) {
                            dao.update(existing.copy(timestamp = System.currentTimeMillis()))
                        } else {
                            dao.insert(
                                ScanRecord(
                                    rawContent = result,
                                    cleanedContent = cleanUrl(result),
                                    type = when {
                                        result.startsWith("WIFI:") -> "WIFI"
                                        result.startsWith("http") -> "URL"
                                        else -> "TEXT"
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    AnimatedContent(
        targetState = showScanner && hasCameraPermission,
        transitionSpec = {
            if (targetState) {
                // Camera opening — slide up with decelerate (entering)
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, easing = emphasizedDecelerate)
                ) togetherWith slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(200, easing = emphasizedAccelerate)
                )
            } else {
                // Going back — slide down with accelerate (exiting)
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(500, easing = emphasizedDecelerate)
                ) togetherWith slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200, easing = emphasizedAccelerate)
                )
            }
        },
        label = "screen_transition"
    ) { isScanner ->
        if (isScanner) {
            QrScannerScreen(
                onQrCodeScanned = { result ->
                    if (scannedResult == null) {
                        scannedResult = result
                        scope.launch {
                            val existing = dao.findByContent(result)
                            if (existing != null) {
                                dao.update(existing.copy(timestamp = System.currentTimeMillis()))
                            } else {
                                dao.insert(
                                    ScanRecord(
                                        rawContent = result,
                                        cleanedContent = cleanUrl(result),
                                        type = when {
                                            result.startsWith("WIFI:") -> "WIFI"
                                            result.startsWith("http") -> "URL"
                                            else -> "TEXT"
                                        }
                                    )
                                )
                            }
                        }
                    }
                },
                onBack = { showScanner = false }
            )

            scannedResult?.let { result ->
                ScanResultBottomSheet(
                    rawResult = result,
                    onDismiss = { scannedResult = null }
                )
            }
        } else {
            HistoryScreen(
                onScanClick = {
                    if (hasCameraPermission) showScanner = true
                    else launcher.launch(Manifest.permission.CAMERA)
                },
                onSearchClick = { showSearch = true },
                onItemClick = { scan -> selectedScan = scan },
                onScanImageClick = { imagePickerLauncher.launch("image/*") }
            )

            selectedScan?.let { scan ->
                ScanResultBottomSheet(
                    rawResult = scan.rawContent,
                    onDismiss = { selectedScan = null }
                )
            }
            imageScanResult?.let { result ->
                ScanResultBottomSheet(
                    rawResult = result,
                    onDismiss = { imageScanResult = null }
                )
            }
        }
    }
}
