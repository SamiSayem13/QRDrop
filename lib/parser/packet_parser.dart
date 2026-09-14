import 'dart:convert';
import '../models/qr_pixel_packet.dart';

class PacketParser {
  /// Parses raw QR code string formatted as:
  /// PIXEL|frame_index|total_frames|width|height|base64_jpeg_chunk
  ///
  /// Returns null if format is invalid, frame count invalid, or Base64 decode fails.
  static QrPixelPacket? parse(String rawText) {
    if (rawText.trim().isEmpty) return null;

    try {
      final parts = rawText.split('|');
      if (parts.length < 6) return null;

      final packetType = parts[0];
      if (packetType != 'PIXEL') return null;

      final frameIndex = int.tryParse(parts[1]);
      final totalFrames = int.tryParse(parts[2]);
      final width = int.tryParse(parts[3]);
      final height = int.tryParse(parts[4]);
      final encodedData = parts[5];

      if (frameIndex == null ||
          totalFrames == null ||
          width == null ||
          height == null ||
          frameIndex < 0 ||
          totalFrames <= 0 ||
          width <= 0 ||
          height <= 0 ||
          frameIndex >= totalFrames ||
          width > 4000 ||
          height > 4000 ||
          totalFrames > 50000 ||
          encodedData.isEmpty) {
        return null;
      }

      final decodedBytes = base64Decode(encodedData);
      if (decodedBytes.isEmpty) {
        return null;
      }

      return QrPixelPacket(
        frameIndex: frameIndex,
        totalFrames: totalFrames,
        width: width,
        height: height,
        data: decodedBytes,
      );
    } catch (e) {
      // Malformed packets or Base64 decode failures must never crash the app
      return null;
    }
  }
}
