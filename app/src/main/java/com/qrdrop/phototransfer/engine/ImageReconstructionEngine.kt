package com.qrdrop.phototransfer.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.qrdrop.phototransfer.model.QrPixelPacket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe engine that reconstructs the photo live in memory as QR frames arrive.
 */
class ImageReconstructionEngine {

    companion object {
        const val PIXELS_PER_FRAME = 500
    }

    @Volatile
    var width: Int = 0
        private set

    @Volatile
    var height: Int = 0
        private set

    @Volatile
    var totalFrames: Int = 0
        private set

    private val receivedFrames = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    @Volatile
    var bitmap: Bitmap? = null
        private set

    private var pixelColors: IntArray? = null

    val receivedFrameCount: Int
        get() = receivedFrames.size

    val isComplete: Boolean
        get() = totalFrames > 0 && receivedFrames.size >= totalFrames

    val progressPercentage: Float
        get() = if (totalFrames > 0) (receivedFrames.size.toFloat() / totalFrames) * 100f else 0f

    /**
     * Processes a received QR pixel packet.
     * Places the actual RGB pixels into the correct canvas location immediately.
     *
     * @return true if new pixels were placed; false if duplicate, out of range, or invalid.
     */
    @Synchronized
    fun processPacket(packet: QrPixelPacket): Boolean {
        // Initialize canvas on first valid packet or if resolution changed
        if (bitmap == null || width != packet.width || height != packet.height) {
            width = packet.width
            height = packet.height
            totalFrames = packet.totalFrames
            receivedFrames.clear()

            val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            newBitmap.eraseColor(Color.BLACK)
            bitmap = newBitmap

            val colors = IntArray(width * height) { Color.BLACK }
            pixelColors = colors
        }

        // Ignore duplicate frame
        if (receivedFrames.contains(packet.frameIndex)) {
            return false
        }

        val currentBitmap = bitmap ?: return false
        val colors = pixelColors ?: return false

        val rgbData = packet.rgbData
        val pixelCount = rgbData.size / 3
        val startPixel = packet.frameIndex * PIXELS_PER_FRAME
        val totalCanvasPixels = width * height

        var pixelsPlaced = 0
        for (i in 0 until pixelCount) {
            val destPixelIdx = startPixel + i
            if (destPixelIdx >= totalCanvasPixels) break

            val r = rgbData[i * 3].toInt() and 0xFF
            val g = rgbData[i * 3 + 1].toInt() and 0xFF
            val b = rgbData[i * 3 + 2].toInt() and 0xFF

            // ARGB_8888 format
            val colorInt = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            colors[destPixelIdx] = colorInt

            val x = destPixelIdx % width
            val y = destPixelIdx / width
            currentBitmap.setPixel(x, y, colorInt)
            pixelsPlaced++
        }

        if (pixelsPlaced > 0) {
            receivedFrames.add(packet.frameIndex)
            return true
        }

        return false
    }

    /**
     * Resets the engine state for a new transfer session.
     */
    @Synchronized
    fun reset() {
        width = 0
        height = 0
        totalFrames = 0
        receivedFrames.clear()
        bitmap?.recycle()
        bitmap = null
        pixelColors = null
    }
}
