import 'dart:async';
import 'dart:typed_data';
import 'dart:ui' as ui;
import '../models/qr_pixel_packet.dart';

class ImageReconstructionEngine {
  int _width = 0;
  int _height = 0;
  int _totalFrames = 0;

  final Map<int, Uint8List> _chunks = {};
  final Set<int> _receivedFrames = {};

  Uint8List? _completeJpegBytes;
  ui.Image? _uiImage;
  bool _hasDecodeError = false;

  int get width => _width;
  int get height => _height;
  int get totalFrames => _totalFrames;
  int get receivedFrameCount => _receivedFrames.length;
  bool get isComplete => _totalFrames > 0 && _receivedFrames.length >= _totalFrames;
  double get progressPercentage => _totalFrames > 0 ? (_receivedFrames.length / _totalFrames) * 100.0 : 0.0;
  ui.Image? get uiImage => _uiImage;
  Uint8List? get completeJpegBytes => _completeJpegBytes;
  bool get hasDecodeError => _hasDecodeError;

  /// Processes a received QR JPEG chunk packet.
  /// Stores the JPEG chunk by frameIndex.
  /// Does NOT create pixels or partial images after each QR frame.
  /// Once ALL frames are received, concatenates chunks in frameIndex order and decodes the full JPEG.
  Future<bool> processPacket(QrPixelPacket packet) async {
    // Initialize session if resolution or total frames changed
    if (_totalFrames == 0 || _totalFrames != packet.totalFrames) {
      _width = packet.width;
      _height = packet.height;
      _totalFrames = packet.totalFrames;
      _chunks.clear();
      _receivedFrames.clear();
      _completeJpegBytes = null;
      _hasDecodeError = false;
    }

    // Filter out duplicate frames
    if (_receivedFrames.contains(packet.frameIndex)) {
      return false;
    }

    // Store JPEG chunk by frameIndex (supports out-of-order arrival)
    _chunks[packet.frameIndex] = packet.data;
    _receivedFrames.add(packet.frameIndex);

    // If ALL frames are received, concatenate chunks and decode the completed JPEG
    if (isComplete) {
      await _assembleAndDecodeJpeg();
    }

    return true;
  }

  Future<void> _assembleAndDecodeJpeg() async {
    try {
      final builder = BytesBuilder(copy: false);
      for (int i = 0; i < _totalFrames; i++) {
        final chunk = _chunks[i];
        if (chunk == null) {
          _hasDecodeError = true;
          return;
        }
        builder.add(chunk);
      }

      _completeJpegBytes = builder.toBytes();

      // Decode full JPEG bytes into ui.Image for Flutter rendering
      final completer = Completer<ui.Image>();
      ui.decodeImageFromList(_completeJpegBytes!, (ui.Image img) {
        completer.complete(img);
      });

      final newImage = await completer.future;

      // Dispose previous texture reference to avoid GPU memory leak
      _uiImage?.dispose();
      _uiImage = newImage;
    } catch (e) {
      _hasDecodeError = true;
    }
  }

  void reset() {
    _width = 0;
    _height = 0;
    _totalFrames = 0;
    _chunks.clear();
    _receivedFrames.clear();
    _completeJpegBytes = null;
    _hasDecodeError = false;
    _uiImage?.dispose();
    _uiImage = null;
  }
}
