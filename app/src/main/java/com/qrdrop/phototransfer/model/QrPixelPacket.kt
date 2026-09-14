package com.qrdrop.phototransfer.model

/**
 * Represents a parsed QR packet containing a portion of the raw image pixels.
 *
 * Packet Format: PIXEL|frame_index|total_frames|width|height|base64_rgb_data
 */
data class QrPixelPacket(
    val frameIndex: Int,
    val totalFrames: Int,
    val width: Int,
    val height: Int,
    val rgbData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QrPixelPacket

        if (frameIndex != other.frameIndex) return false
        if (totalFrames != other.totalFrames) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!rgbData.contentEquals(other.rgbData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameIndex
        result = 31 * result + totalFrames
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + rgbData.contentHashCode()
        return result
    }
}
