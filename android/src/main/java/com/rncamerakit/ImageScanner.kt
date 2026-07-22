package com.rncamerakit

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class ImageScanner {
    companion object {
        fun scanFromUri(
            context: Context,
            uri: Uri,
            allowedBarcodeTypes: List<String>?,
            callback: (List<Map<String, Any>>) -> Unit
        ) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = createScanner(allowedBarcodeTypes)

                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val imageWidth = inputImage.width.toFloat()
                        val imageHeight = inputImage.height.toFloat()

                        val results = barcodes.mapNotNull { barcode ->
                            val value = barcode.rawValue ?: return@mapNotNull null
                            val format = CodeFormat.fromBarcodeType(barcode.format)
                            if (allowedBarcodeTypes != null && allowedBarcodeTypes.isNotEmpty()) {
                                if (!allowedBarcodeTypes.contains(format.code)) return@mapNotNull null
                            }
                            val boundingBox = barcode.boundingBox
                            mapOf(
                                "codeStringValue" to value,
                                "codeFormat" to format.code,
                                "displayValue" to (barcode.displayValue ?: value),
                                "boundingBox" to mapOf(
                                    "x" to if (imageWidth > 0) (boundingBox?.left?.toFloat() ?: 0f) / imageWidth else 0f,
                                    "y" to if (imageHeight > 0) (boundingBox?.top?.toFloat() ?: 0f) / imageHeight else 0f,
                                    "width" to if (imageWidth > 0) (boundingBox?.width()?.toFloat() ?: 0f) / imageWidth else 0f,
                                    "height" to if (imageHeight > 0) (boundingBox?.height()?.toFloat() ?: 0f) / imageHeight else 0f
                                )
                            )
                        }
                        callback(results)
                    }
                    .addOnFailureListener {
                        callback(emptyList())
                    }
            } catch (e: Exception) {
                callback(emptyList())
            }
        }

        private fun createScanner(allowedBarcodeTypes: List<String>?): com.google.mlkit.vision.barcode.BarcodeScanner {
            if (allowedBarcodeTypes.isNullOrEmpty()) {
                return BarcodeScanning.getClient()
            }

            val formats = allowedBarcodeTypes.mapNotNull { CodeFormat.fromName(it)?.toBarcodeType() }
            if (formats.isEmpty()) {
                return BarcodeScanning.getClient()
            }

            val optionsBuilder = BarcodeScannerOptions.Builder()
            optionsBuilder.setBarcodeFormats(
                formats.first(),
                *formats.drop(1).toIntArray()
            )
            return BarcodeScanning.getClient(optionsBuilder.build())
        }
    }
}
