import 'package:flutter_test/flutter_test.dart';
import 'package:qrdrop/main.dart';

void main() {
  testWidgets('App renders QR Photo Transfer title', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const QRPhotoTransferApp());

    // Verify that the app title is present.
    expect(find.text('QR Photo Transfer'), findsOneWidget);
  });
}
