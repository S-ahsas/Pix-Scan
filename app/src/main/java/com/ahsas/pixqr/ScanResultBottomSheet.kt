package com.ahsas.pixqr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultBottomSheet(
    rawResult: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cleanedResult = remember(rawResult) { cleanUrl(rawResult) }
    val isUrl = remember(rawResult) { rawResult.startsWith("http") }
    val wasModified = remember(rawResult, cleanedResult) { rawResult != cleanedResult }
    val wifiData = remember(rawResult) { parseWifi(rawResult) }
    val isWifi = wifiData != null
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true }
    )
    var isConnected by remember(wifiData) {
        mutableStateOf(
            if (isWifi && wifiData != null) isConnectedToWifi(context, wifiData.ssid)
            else false
        )
    }

    // Micro interaction sources
    val copyInteraction = remember { MutableInteractionSource() }
    val copyPressed by copyInteraction.collectIsPressedAsState()
    val copyScale by animateFloatAsState(
        targetValue = if (copyPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "copy_scale"
    )

    val shareInteraction = remember { MutableInteractionSource() }
    val sharePressed by shareInteraction.collectIsPressedAsState()
    val shareScale by animateFloatAsState(
        targetValue = if (sharePressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "share_scale"
    )

    val actionInteraction = remember { MutableInteractionSource() }
    val actionPressed by actionInteraction.collectIsPressedAsState()
    val actionScale by animateFloatAsState(
        targetValue = if (actionPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "action_scale"
    )

    LaunchedEffect(Unit) {
        playFeedback(context)
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = when {
                    isWifi -> "WiFi Network Scanned"
                    isUrl -> "Link Scanned"
                    else -> "Text Scanned"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            // WiFi details card
            if (isWifi && wifiData != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = wifiData.ssid,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (wifiData.password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Password: ${wifiData.password}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Tracking params removed note
            if (wasModified) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tracking parameters removed",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // URL display box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = cleanedResult,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Pix Scan", cleanedResult))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { scaleX = copyScale; scaleY = copyScale },
                    shape = RoundedCornerShape(12.dp),
                    interactionSource = copyInteraction
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 13.sp)
                }

                // Share
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, cleanedResult)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { scaleX = shareScale; scaleY = shareScale },
                    shape = RoundedCornerShape(12.dp),
                    interactionSource = shareInteraction
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 13.sp)
                }

                // WiFi / URL action button
                when {
                    isWifi && wifiData != null -> {
                        if (isConnected) {
                            OutlinedButton(
                                onClick = {
                                    @Suppress("MissingPermission")
                                    forgetWifi(context, wifiData)
                                    isConnected = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { scaleX = actionScale; scaleY = actionScale },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                interactionSource = actionInteraction
                            ) {
                                Icon(
                                    Icons.Rounded.WifiOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Forget", fontSize = 13.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    @Suppress("MissingPermission")
                                    connectToWifi(context, wifiData)
                                    isConnected = isConnectedToWifi(context, wifiData.ssid)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { scaleX = actionScale; scaleY = actionScale },
                                shape = RoundedCornerShape(12.dp),
                                interactionSource = actionInteraction
                            ) {
                                Icon(
                                    Icons.Rounded.WifiPassword,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect", fontSize = 13.sp)
                            }
                        }
                    }
                    isUrl -> {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanedResult)))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { scaleX = actionScale; scaleY = actionScale },
                            shape = RoundedCornerShape(12.dp),
                            interactionSource = actionInteraction
                        ) {
                            Icon(
                                Icons.Rounded.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

fun cleanUrl(rawUrl: String): String {
    if (!rawUrl.startsWith("http")) return rawUrl
    val trackingParams = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "gclsrc", "dclid", "msclkid",
        "ref", "mc_cid", "mc_eid", "_ga", "igshid", "yclid", "_gl"
    )
    return try {
        val uri = Uri.parse(rawUrl)
        val cleaned = uri.buildUpon().clearQuery()
        uri.queryParameterNames
            .filter { it !in trackingParams }
            .forEach { cleaned.appendQueryParameter(it, uri.getQueryParameter(it)) }
        cleaned.build().toString()
    } catch (e: Exception) {
        rawUrl
    }
}

fun playFeedback(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    when (audioManager.ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 40)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (e: Exception) { }
        }
        AudioManager.RINGER_MODE_VIBRATE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        }
        AudioManager.RINGER_MODE_SILENT -> { }
    }
}

data class WifiData(val ssid: String, val password: String, val type: String)

fun parseWifi(raw: String): WifiData? {
    if (!raw.startsWith("WIFI:")) return null
    return try {
        val ssid = Regex("S:(.*?);").find(raw)?.groupValues?.get(1) ?: return null
        val password = Regex("P:(.*?);").find(raw)?.groupValues?.get(1) ?: ""
        val type = Regex("T:(.*?);").find(raw)?.groupValues?.get(1) ?: "WPA"
        WifiData(ssid, password, type)
    } catch (e: Exception) { null }
}

@RequiresPermission(Manifest.permission.CHANGE_WIFI_STATE)
fun connectToWifi(context: Context, wifiData: WifiData) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val intent = Intent("android.settings.WIFI_ADD_NETWORKS").apply {
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(wifiData.ssid)
                .apply {
                    if (wifiData.password.isNotEmpty()) {
                        when (wifiData.type.uppercase()) {
                            "WPA", "WPA2" -> setWpa2Passphrase(wifiData.password)
                            "WEP" -> setWpa2Passphrase(wifiData.password)
                        }
                    }
                }
                .build()
            putParcelableArrayListExtra(
                "android.provider.extra.WIFI_NETWORK_LIST",
                arrayListOf(suggestion)
            )
        }
        context.startActivity(intent)
    } else {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }
}

fun isConnectedToWifi(context: Context, ssid: String): Boolean {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiManager.connectionInfo
    return wifiInfo.ssid == "\"$ssid\""
}

@RequiresPermission(Manifest.permission.CHANGE_WIFI_STATE)
fun forgetWifi(context: Context, wifiData: WifiData) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(wifiData.ssid)
            .apply {
                if (wifiData.password.isNotEmpty()) {
                    when (wifiData.type.uppercase()) {
                        "WPA", "WPA2" -> setWpa2Passphrase(wifiData.password)
                        "WEP" -> setWpa2Passphrase(wifiData.password)
                    }
                }
            }
            .build()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.removeNetworkSuggestions(listOf(suggestion))
    }
}