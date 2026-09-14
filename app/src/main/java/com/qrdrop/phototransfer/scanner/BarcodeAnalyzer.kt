package com.qrdrop.phototransfer.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient()
    private var lastScannedText: String = ""
    private var lastScannedTime: Long = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrEmpty()) {
                            val now = System.currentTimeMillis()
                            // Skip re-processing identical text if received within 40ms to avoid redundant parsing
                            if (rawValue != lastScannedText || now - lastScannedTime > 40) {
                                lastScannedText = rawValue
                                lastScannedTime = now
                                onBarcodeDetected(rawValue)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Suppress ML Kit failures gracefully
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
