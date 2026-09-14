import 'package:flutter/material.dart';
import 'ui/receiver_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const QRPhotoTransferApp());
}

class QRPhotoTransferApp extends StatelessWidget {
  const QRPhotoTransferApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'QR Photo Transfer',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF101216),
        primaryColor: const Color(0xFF00E5FF),
      ),
      home: const ReceiverScreen(),
    );
  }
}
