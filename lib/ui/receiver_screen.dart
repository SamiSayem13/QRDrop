import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:permission_handler/permission_handler.dart';
import '../engine/image_reconstruction_engine.dart';
import '../parser/packet_parser.dart';
import '../saver/gallery_saver.dart';

class ReceiverScreen extends StatefulWidget {
  const ReceiverScreen({super.key});

  @override
  State<ReceiverScreen> createState() => _ReceiverScreenState();
}

class _ReceiverScreenState extends State<ReceiverScreen> {
  final ImageReconstructionEngine _engine = ImageReconstructionEngine();
  final MobileScannerController _scannerController = MobileScannerController(
    detectionSpeed: DetectionSpeed.unrestricted,
    returnImage: false,
  );

  bool _isScanning = true;
  bool _isSaving = false;
  bool _hasCameraPermission = false;
  String _statusMessage = 'Scanning for QR stream...';

  @override
  void initState() {
    super.initState();
    _checkPermission();
  }

  Future<void> _checkPermission() async {
    final status = await Permission.camera.status;
    if (status.isGranted) {
      setState(() {
        _hasCameraPermission = true;
      });
    } else {
      final requestStatus = await Permission.camera.request();
      setState(() {
        _hasCameraPermission = requestStatus.isGranted;
      });
    }
  }

  void _onBarcodeDetected(BarcodeCapture capture) async {
    if (!_isScanning || _engine.isComplete) return;

    for (final barcode in capture.barcodes) {
      final rawValue = barcode.rawValue;
      if (rawValue == null || rawValue.isEmpty) continue;

      final packet = PacketParser.parse(rawValue);
      if (packet == null) continue;

      final isNewChunk = await _engine.processPacket(packet);
      if (isNewChunk && mounted) {
        setState(() {
          if (_engine.isComplete) {
            _isScanning = false;
            _statusMessage = _engine.hasDecodeError
                ? 'JPEG decode error'
                : 'Transfer Complete!';
          } else {
            _statusMessage =
                'Receiving JPEG chunk ${packet.frameIndex + 1} / ${_engine.totalFrames}';
          }
        });
      }
    }
  }

  Future<void> _saveToGallery() async {
    final jpegBytes = _engine.completeJpegBytes;
    if (jpegBytes == null || _isSaving) return;

    setState(() {
      _isSaving = true;
    });

    final result = await GallerySaver.saveToGallery(
      jpegBytes: jpegBytes,
    );

    if (mounted) {
      setState(() {
        _isSaving = false;
      });

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result ?? 'Saved to Gallery')),
      );
    }
  }

  void _reset() {
    _engine.reset();
    setState(() {
      _isScanning = true;
      _isSaving = false;
      _statusMessage = 'Scanning for QR stream...';
    });
  }

  @override
  void dispose() {
    _scannerController.dispose();
    _engine.reset();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF101216),
      body: SafeArea(
        child: Stack(
          children: [
            Column(
              children: [
                // Header Bar
                _buildHeader(),

                const SizedBox(height: 12),

                // Main Photo Reconstruction Display
                Expanded(
                  child: Container(
                    margin: const EdgeInsets.symmetric(horizontal: 16),
                    decoration: BoxDecoration(
                      color: const Color(0xFF1A1D24),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: const Color(0xFF252932)),
                    ),
                    clipBehavior: Clip.antiAlias,
                    child: Center(
                      child: _engine.uiImage != null
                          ? RawImage(
                              image: _engine.uiImage,
                              fit: BoxFit.contain,
                            )
                          : _engine.hasDecodeError
                              ? _buildErrorDisplay()
                              : _buildInitialPlaceholder(),
                    ),
                  ),
                ),

                const SizedBox(height: 16),

                // Telemetry & Progress Panel
                _buildProgressPanel(),
              ],
            ),

            // Secondary Small Floating Camera Preview (PIP mode)
            if (_isScanning && _hasCameraPermission)
              Positioned(
                right: 24,
                bottom: 120,
                child: Container(
                  width: 110,
                  height: 150,
                  decoration: BoxDecoration(
                    color: Colors.black,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: const Color(0xFF00E5FF), width: 2),
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: Stack(
                    children: [
                      MobileScanner(
                        controller: _scannerController,
                        onDetect: _onBarcodeDetected,
                        errorBuilder: (context, error, child) {
                          return const Center(
                            child: Icon(Icons.camera_alt_outlined, color: Colors.white54, size: 28),
                          );
                        },
                      ),
                      Positioned(
                        top: 6,
                        left: 6,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                          decoration: BoxDecoration(
                            color: Colors.black.withAlpha(180),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              CircleAvatar(
                                radius: 3,
                                backgroundColor: Color(0xFF00E676),
                              ),
                              SizedBox(width: 4),
                              Text(
                                'SCANNING',
                                style: TextStyle(
                                  fontSize: 8,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),

            if (!_hasCameraPermission)
              Positioned.fill(
                child: Container(
                  color: const Color(0xFF101216),
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.camera_alt, size: 64, color: Color(0xFF00E5FF)),
                      const SizedBox(height: 16),
                      const Text(
                        'Camera Permission Required',
                        style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        'Camera access is needed to scan compressed-JPEG QR streams.',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 13, color: Color(0xFF90A4AE)),
                      ),
                      const SizedBox(height: 24),
                      ElevatedButton(
                        onPressed: _checkPermission,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF00E5FF),
                          foregroundColor: Colors.black,
                        ),
                        child: const Text('Grant Camera Permission', style: TextStyle(fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'QR Photo Transfer',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFFECEFF1),
                ),
              ),
              Text(
                'Compressed-JPEG QR Receiver',
                style: TextStyle(
                  fontSize: 12,
                  color: Color(0xFF90A4AE),
                ),
              ),
            ],
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: _engine.isComplete
                  ? const Color(0xFF00E676).withAlpha(50)
                  : const Color(0xFF00E5FF).withAlpha(50),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: _engine.isComplete ? const Color(0xFF00E676) : const Color(0xFF00E5FF),
              ),
            ),
            child: Text(
              _engine.isComplete ? 'COMPLETE' : 'LIVE SCAN',
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.bold,
                color: _engine.isComplete ? const Color(0xFF00E676) : const Color(0xFF00E5FF),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInitialPlaceholder() {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.camera_alt,
            size: 56,
            color: Color(0x9900E5FF),
          ),
          const SizedBox(height: 16),
          const Text(
            'Waiting for Compressed-JPEG QR Stream...',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFFECEFF1),
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Text(
            _engine.totalFrames > 0
                ? 'Receiving JPEG chunks (${_engine.receivedFrameCount} / ${_engine.totalFrames})...'
                : 'Point camera at sender screen. Full photo will decode when all chunks arrive.',
            style: const TextStyle(
              fontSize: 12,
              color: Color(0xFF90A4AE),
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildErrorDisplay() {
    return const Padding(
      padding: EdgeInsets.all(24.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.error_outline,
            size: 56,
            color: Colors.redAccent,
          ),
          SizedBox(height: 16),
          Text(
            'JPEG Decoding Error',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Colors.redAccent,
            ),
            textAlign: TextAlign.center,
          ),
          SizedBox(height: 8),
          Text(
            'The reconstructed JPEG data is corrupted or incomplete. Please reset and scan again.',
            style: TextStyle(
              fontSize: 12,
              color: Color(0xFF90A4AE),
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildProgressPanel() {
    final isComplete = _engine.isComplete;
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF1A1D24),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          if (isComplete && !_engine.hasDecodeError) ...[
            const Row(
              children: [
                Icon(Icons.check_circle, color: Color(0xFF00E676), size: 28),
                SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Transfer Complete',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF00E676),
                        ),
                      ),
                      Text(
                        'JPEG photo decoded successfully',
                        style: TextStyle(fontSize: 12, color: Color(0xFF90A4AE)),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isSaving ? null : _saveToGallery,
                    icon: _isSaving
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black),
                          )
                        : const Icon(Icons.save, size: 18),
                    label: const Text('Save to Gallery', style: TextStyle(fontWeight: FontWeight.bold)),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF00E676),
                      foregroundColor: Colors.black,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                OutlinedButton(
                  onPressed: _reset,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFECEFF1),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  ),
                  child: const Icon(Icons.refresh, size: 18),
                ),
              ],
            ),
          ] else ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  _engine.totalFrames > 0
                      ? '${_engine.receivedFrameCount} / ${_engine.totalFrames} chunks'
                      : '0 / 0 chunks',
                  style: const TextStyle(fontSize: 14, color: Color(0xFFECEFF1)),
                ),
                Text(
                  '${_engine.progressPercentage.toStringAsFixed(1)}%',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    fontFamily: 'monospace',
                    color: Color(0xFF00E5FF),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: _engine.totalFrames > 0 ? _engine.receivedFrameCount / _engine.totalFrames : 0,
              backgroundColor: const Color(0xFF252932),
              color: _engine.hasDecodeError ? Colors.redAccent : const Color(0xFF00E5FF),
              minHeight: 8,
              borderRadius: BorderRadius.circular(4),
            ),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Status: $_statusMessage',
                  style: const TextStyle(fontSize: 12, color: Color(0xFF90A4AE)),
                ),
                if (_engine.width > 0)
                  Text(
                    '${_engine.width}x${_engine.height}px',
                    style: const TextStyle(fontSize: 12, color: Color(0xFF90A4AE), fontFamily: 'monospace'),
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
