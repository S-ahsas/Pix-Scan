package com.ahsas.pixqr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

fun scanImageForQr(
    context: Context,
    uri: Uri,
    onResult: (String?) -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val result = barcodes.firstOrNull()?.rawValue
                onResult(result)
            }
            .addOnFailureListener {
                onResult(null)
            }
    } catch (e: Exception) {
        onResult(null)
    }
}