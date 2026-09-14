package com.qrdrop.phototransfer

import com.qrdrop.phototransfer.parser.PacketParser
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class PacketParserTest {

    @Test
    fun testValidPacketParsing() {
        val rgbBytes = byteArrayOf(255.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 255.toByte(), 0.toByte())
        val base64Data = Base64.getEncoder().encodeToString(rgbBytes)
        val rawPacket = "PIXEL|0|228|640|480|$base64Data"

        val packet = PacketParser.parse(rawPacket)

        assertNotNull(packet)
        assertEquals(0, packet!!.frameIndex)
        assertEquals(228, packet.totalFrames)
        assertEquals(640, packet.width)
        assertEquals(480, packet.height)
        assertArrayEquals(rgbBytes, packet.rgbData)
    }

    @Test
    fun testInvalidPrefix() {
        val rawPacket = "INVALID|0|228|640|480|AAAA"
        val packet = PacketParser.parse(rawPacket)
        assertNull(packet)
    }

    @Test
    fun testMalformedParts() {
        val rawPacket = "PIXEL|0|228|640"
        val packet = PacketParser.parse(rawPacket)
        assertNull(packet)
    }

    @Test
    fun testNonNumericIndex() {
        val rawPacket = "PIXEL|abc|228|640|480|AAAA"
        val packet = PacketParser.parse(rawPacket)
        assertNull(packet)
    }

    @Test
    fun testInvalidRgbDataLengthNotMultipleOfThree() {
        val invalidBytes = byteArrayOf(1, 2)
        val base64Data = Base64.getEncoder().encodeToString(invalidBytes)
        val rawPacket = "PIXEL|0|228|640|480|$base64Data"

        val packet = PacketParser.parse(rawPacket)
        assertNull(packet)
    }
}
