import 'dart:typed_data';
import 'package:gal/gal.dart';

class GallerySaver {
  /// Saves completed JPEG bytes to the device Gallery/MediaStore.
  static Future<String?> saveToGallery({
    required Uint8List jpegBytes,
  }) async {
    try {
      final hasAccess = await Gal.hasAccess();
      if (!hasAccess) {
        final granted = await Gal.requestAccess();
        if (!granted) {
          return 'Gallery permission was denied';
        }
      }

      await Gal.putImageBytes(
        jpegBytes,
        name: 'QRTransfer_${DateTime.now().millisecondsSinceEpoch}',
      );

      return 'Saved successfully to Gallery!';
    } catch (e) {
      return 'Failed to save: $e';
    }
  }
}
