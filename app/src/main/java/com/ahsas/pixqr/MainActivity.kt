package com.ahsas.pixqr

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ahsas.pixqr.ui.theme.PixQRTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if launched from Quick Tile
        val openScannerFromTile = intent?.action == QrScannerTileService.ACTION_OPEN_SCANNER

        setContent {
            PixQRTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(openScannerDirectly = openScannerFromTile)
                }
            }
        }
    }

    @Composable
    fun MainScreen(openScannerDirectly: Boolean = false) {
        var hasCameraPermission by remember { mutableStateOf(false) }
        var showScanner by remember { mutableStateOf(openScannerDirectly) }
        var showGenerateQr by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var selectedScan by remember { mutableStateOf<ScanRecord?>(null) }
    var imageScanResult by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val db = (context.applicationContext as App).database
    val dao = db.scanDao()
    val scope = rememberCoroutineScope()

    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // Permission launcher — declared before use
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    // Image picker launcher
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

    // Add this block right after the showSettings block
    if (showGenerateQr) {
            GenerateQrScreen(onBack = { showGenerateQr = false })
            return
    }

    // Settings screen — no animation needed, plain swap
    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    // Camera ↔ History with slide animation
    AnimatedContent(
        targetState = showScanner && hasCameraPermission,
        transitionSpec = {
            if (targetState) {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, easing = emphasizedDecelerate)
                ) togetherWith slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(200, easing = emphasizedAccelerate)
                )
            } else {
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
                onScanImageClick = { imagePickerLauncher.launch("image/*") },
                onSettingsClick = { showSettings = true },
                onGenerateQrClick = { showGenerateQr = true }
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
}}