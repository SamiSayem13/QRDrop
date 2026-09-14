package com.qrdrop.phototransfer.parser

import android.os.Build
import android.util.Base64
import com.qrdrop.phototransfer.model.QrPixelPacket

object PacketParser {

    /**
     * Parses a raw QR code string formatted as:
     * PIXEL|frame_index|total_frames|width|height|base64_rgb_data
     *
     * Returns null if format is invalid, frame count invalid, or Base64 decode fails.
     */
    fun parse(rawText: String): QrPixelPacket? {
        if (rawText.isBlank()) return null

        return try {
            val parts = rawText.split("|", limit = 6)
            if (parts.size != 6) return null

            val packetType = parts[0]
            if (packetType != "PIXEL") return null

            val frameIndex = parts[1].toIntOrNull() ?: return null
            val totalFrames = parts[2].toIntOrNull() ?: return null
            val width = parts[3].toIntOrNull() ?: return null
            val height = parts[4].toIntOrNull() ?: return null
            val encodedData = parts[5]

            if (frameIndex < 0 || totalFrames <= 0 || width <= 0 || height <= 0 || encodedData.isEmpty()) {
                return null
            }

            val decodedBytes = decodeBase64(encodedData) ?: return null
            if (decodedBytes.isEmpty() || decodedBytes.size % 3 != 0) {
                return null
            }

            QrPixelPacket(
                frameIndex = frameIndex,
                totalFrames = totalFrames,
                width = width,
                height = height,
                rgbData = decodedBytes
            )
        } catch (e: Exception) {
            // Malformed packet or decoding error must never crash the app
            null
        }
    }

    private fun decodeBase64(input: String): ByteArray? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.util.Base64.getDecoder().decode(input)
            } else {
                Base64.decode(input, Base64.DEFAULT)
            }
        } catch (e: Throwable) {
            try {
                java.util.Base64.getDecoder().decode(input)
            } catch (e2: Throwable) {
                null
            }
        }
    }
}
