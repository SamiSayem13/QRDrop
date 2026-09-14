import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter_test/flutter_test.dart';
import 'package:qrdrop/parser/packet_parser.dart';

void main() {
  test('Parses valid PIXEL JPEG chunk packet correctly', () {
    final jpegBytes = Uint8List.fromList([0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10]);
    final base64Data = base64Encode(jpegBytes);
    final rawPacket = 'PIXEL|0|10|640|480|$base64Data';

    final packet = PacketParser.parse(rawPacket);

    expect(packet, isNotNull);
    expect(packet!.frameIndex, equals(0));
    expect(packet.totalFrames, equals(10));
    expect(packet.width, equals(640));
    expect(packet.height, equals(480));
    expect(packet.data, equals(jpegBytes));
  });

  test('Returns null for non-PIXEL packet type', () {
    final rawPacket = 'INVALID|0|10|640|480|AAAA';
    final packet = PacketParser.parse(rawPacket);
    expect(packet, isNull);
  });

  test('Returns null for malformed parts count', () {
    final rawPacket = 'PIXEL|0|10|640';
    final packet = PacketParser.parse(rawPacket);
    expect(packet, isNull);
  });

  test('Returns null when frameIndex is out of range', () {
    final rawPacket = 'PIXEL|10|10|640|480|AAAA';
    final packet = PacketParser.parse(rawPacket);
    expect(packet, isNull);
  });
}
