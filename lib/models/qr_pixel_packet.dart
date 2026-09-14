import 'dart:typed_data';

/// Represents a parsed QR packet containing frame metadata and raw JPEG chunk bytes.
class QrPixelPacket {
  final int frameIndex;
  final int totalFrames;
  final int width;
  final int height;
  final Uint8List data;

  const QrPixelPacket({
    required this.frameIndex,
    required this.totalFrames,
    required this.width,
    required this.height,
    required this.data,
  });

  @override
  String toString() {
    return 'QrPixelPacket(frame: $frameIndex/$totalFrames, dim: ${width}x$height, bytes: ${data.length})';
  }
}
