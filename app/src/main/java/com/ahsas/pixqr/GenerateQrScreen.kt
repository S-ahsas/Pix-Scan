package com.ahsas.pixqr

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class QrFormat(val label: String) {
    TEXT("Text"),
    URL("URL"),
    WIFI("WiFi"),
    CONTACT("Contact")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateQrScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Format selection
    var selectedFormat by remember { mutableStateOf(QrFormat.TEXT) }
    var formatDropdownExpanded by remember { mutableStateOf(false) }

    // Text fields
    var textInput by remember { mutableStateOf("") }

    // URL fields
    var urlInput by remember { mutableStateOf("") }

    // WiFi fields
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurityExpanded by remember { mutableStateOf(false) }
    var wifiSecurity by remember { mutableStateOf("WPA") }

    // Contact fields
    var contactFirstName by remember { mutableStateOf("") }
    var contactLastName by remember { mutableStateOf("") }
    var contactPhones by remember { mutableStateOf(listOf("")) }

    // Generated QR
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showResultSheet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reset fields when format changes
    LaunchedEffect(selectedFormat) {
        textInput = ""
        urlInput = ""
        wifiSsid = ""
        wifiPassword = ""
        wifiSecurity = "WPA"
        contactFirstName = ""
        contactLastName = ""
        contactPhones = listOf("")
        generatedBitmap = null
        errorMessage = null
    }

    fun buildQrContent(): String? {
        return when (selectedFormat) {
            QrFormat.TEXT -> textInput.trim().takeIf { it.isNotEmpty() }
            QrFormat.URL -> {
                val url = urlInput.trim()
                if (url.isEmpty()) null
                else if (url.startsWith("http")) url
                else "https://$url"
            }
            QrFormat.WIFI -> {
                val ssid = wifiSsid.trim()
                if (ssid.isEmpty()) null
                else "WIFI:T:$wifiSecurity;S:$ssid;P:${wifiPassword.trim()};;"
            }
            QrFormat.CONTACT -> {
                val first = contactFirstName.trim()
                val last = contactLastName.trim()
                if (first.isEmpty() && last.isEmpty()) null
                else {
                    val phones = contactPhones
                        .filter { it.isNotBlank() }
                        .joinToString("\n") { "TEL:${it.trim()}" }
                    "BEGIN:VCARD\nVERSION:3.0\nN:$last;$first\nFN:$first $last\n$phones\nEND:VCARD"
                }
            }
        }
    }

    fun generateQr() {
        val content = buildQrContent()
        if (content == null) {
            errorMessage = "Please fill in the required fields"
            return
        }
        errorMessage = null
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val writer = QRCodeWriter()
                    val hints = mapOf(EncodeHintType.MARGIN to 1)
                    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
                    val width = bitMatrix.width
                    val height = bitMatrix.height
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            bmp.setPixel(
                                x, y,
                                if (bitMatrix[x, y]) android.graphics.Color.BLACK
                                else android.graphics.Color.WHITE
                            )
                        }
                    }
                    bmp
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                generatedBitmap = bitmap
                showResultSheet = true
            } else {
                errorMessage = "Failed to generate QR code"
            }
        }
    }

    // Result bottom sheet
    if (showResultSheet && generatedBitmap != null) {
        GenerateQrResultSheet(
            bitmap = generatedBitmap!!,
            onDismiss = { showResultSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Generate QR",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Format dropdown
        ExposedDropdownMenuBox(
            expanded = formatDropdownExpanded,
            onExpandedChange = { formatDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedFormat.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Format") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatDropdownExpanded)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = formatDropdownExpanded,
                onDismissRequest = { formatDropdownExpanded = false },
                modifier = Modifier.exposedDropdownSize()
            ) {
                QrFormat.entries.forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format.label) },
                        onClick = {
                            selectedFormat = format
                            formatDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Format-specific fields
        when (selectedFormat) {
            QrFormat.TEXT -> {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Enter your text") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            QrFormat.URL -> {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Website address") },
                    placeholder = { Text("https://example.com") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            QrFormat.WIFI -> {
                OutlinedTextField(
                    value = wifiSsid,
                    onValueChange = { wifiSsid = it },
                    label = { Text("Network name (SSID)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = wifiPassword,
                    onValueChange = { wifiPassword = it },
                    label = { Text("Password") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Security type dropdown
                ExposedDropdownMenuBox(
                    expanded = wifiSecurityExpanded,
                    onExpandedChange = { wifiSecurityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = wifiSecurity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Security type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = wifiSecurityExpanded)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = wifiSecurityExpanded,
                        onDismissRequest = { wifiSecurityExpanded = false }
                    ) {
                        listOf("WPA", "WEP", "nopass").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    wifiSecurity = type
                                    wifiSecurityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            QrFormat.CONTACT -> {
                OutlinedTextField(
                    value = contactFirstName,
                    onValueChange = { contactFirstName = it },
                    label = { Text("First name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = contactLastName,
                    onValueChange = { contactLastName = it },
                    label = { Text("Last name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Phone numbers
                contactPhones.forEachIndexed { index, phone ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                contactPhones = contactPhones.toMutableList().also { list ->
                                    list[index] = it
                                }
                            },
                            label = { Text(if (index == 0) "Phone number" else "Additional phone") },
                            placeholder = { Text("+8801XXXXXXXXX") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        if (index > 0) {
                            IconButton(
                                onClick = {
                                    contactPhones = contactPhones.toMutableList().also { list ->
                                        list.removeAt(index)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.RemoveCircleOutline,
                                    contentDescription = "Remove phone",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                if (contactPhones.size < 3) {
                    TextButton(
                        onClick = {
                            contactPhones = contactPhones + ""
                        }
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add phone")
                    }
                }
            }
        }

        // Error message
        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    // Create button — pinned to bottom
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 20.dp, bottom = 32.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Button(
            onClick = { generateQr() },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Icon(
                Icons.Rounded.QrCode2,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create", fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateQrResultSheet(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Your QR Code",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            // QR image
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(280.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated QR Code",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Share
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val uri = saveBitmapToCache(context, bitmap)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share QR Code"))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                // Save
                Button(
                    onClick = {
                        scope.launch {
                            saveBitmapToGallery(context, bitmap)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }
    }
}

// ─── Save helpers ─────────────────────────────────────────────────────────────

private suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(context.cacheDir, "qr_share_${System.currentTimeMillis()}.png")
            val out = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }
}

private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    withContext(Dispatchers.IO) {
        try {
            val filename = "PixScan_QR_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixScan")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            }
        } catch (e: Exception) {
            // handle silently
        }
    }
}