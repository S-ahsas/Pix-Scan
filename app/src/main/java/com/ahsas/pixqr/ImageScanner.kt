package com.ahsas.pixqr

import android.content.Context
import android.graphics.*
import android.net.Uri
import boofcv.abst.fiducial.QrCodeDetector
import boofcv.android.ConvertBitmap
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.struct.image.GrayU8
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

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
                if (result != null) {
                    onResult(result)
                } else {
                    // ML Kit failed — load bitmap once for ZXing + BoofCV
                    val bitmap = loadBitmap(context, uri)
                    if (bitmap != null) {
                        val zxingResult = scanWithZxingAllAttempts(bitmap)
                        if (zxingResult != null) {
                            onResult(zxingResult)
                        } else {
                            // ZXing failed — try BoofCV
                            onResult(scanWithBoofCV(bitmap))
                        }
                    } else {
                        onResult(null)
                    }
                }
            }
            .addOnFailureListener {
                val bitmap = loadBitmap(context, uri)
                if (bitmap != null) {
                    val zxingResult = scanWithZxingAllAttempts(bitmap)
                    if (zxingResult != null) {
                        onResult(zxingResult)
                    } else {
                        onResult(scanWithBoofCV(bitmap))
                    }
                } else {
                    onResult(null)
                }
            }

    } catch (e: Exception) {
        onResult(null)
    }
}

// ─── Bitmap loader ───────────────────────────────────────────────────────────

private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        bitmap
    } catch (e: Exception) {
        null
    }
}

// ─── ZXing (24 attempts) ─────────────────────────────────────────────────────

private fun scanWithZxingAllAttempts(original: Bitmap): String? {
    return try {
        val highContrast = applyHighContrast(original)
        val inverted = applyInvert(original)

        for (bitmap in listOf(original, highContrast, inverted)) {
            for (degrees in listOf(0, 90, 180, 270)) {
                val rotated = if (degrees == 0) bitmap else rotateBitmap(bitmap, degrees)
                tryZxingDecode(rotated, useHybrid = true)?.let { return it }
                tryZxingDecode(rotated, useHybrid = false)?.let { return it }
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

private fun tryZxingDecode(bitmap: Bitmap, useHybrid: Boolean): String? {
    return try {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(
            if (useHybrid) HybridBinarizer(source) else GlobalHistogramBinarizer(source)
        )

        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )

        MultiFormatReader().decode(binaryBitmap, hints).text
    } catch (e: NotFoundException) {
        null
    } catch (e: Exception) {
        null
    }
}

// ─── BoofCV fallback ─────────────────────────────────────────────────────────

private fun scanWithBoofCV(bitmap: Bitmap): String? {
    return try {
        val gray = GrayU8(bitmap.width, bitmap.height)
        ConvertBitmap.bitmapToGray(bitmap, gray, null)

        val detector: QrCodeDetector<GrayU8> =
            FactoryFiducial.qrcode(null, GrayU8::class.java)
        detector.process(gray)

        detector.detections.firstOrNull()?.message
    } catch (e: Exception) {
        null
    }
}

// ─── Image processing helpers ─────────────────────────────────────────────────

private fun applyHighContrast(original: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint()
    val colorMatrix = ColorMatrix().apply {
        val contrast = 2.5f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        set(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(original, 0f, 0f, paint)
    return result
}

private fun applyInvert(original: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint()
    val colorMatrix = ColorMatrix().apply {
        set(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f,  0f, 1f, 0f
        ))
    }
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(original, 0f, 0f, paint)
    return result
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}