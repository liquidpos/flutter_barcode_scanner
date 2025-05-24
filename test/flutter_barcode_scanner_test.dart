import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_barcode_scanner/flutter_barcode_scanner.dart';
import 'package:flutter_barcode_scanner/flutter_barcode_scanner_platform_interface.dart';
import 'package:flutter_barcode_scanner/flutter_barcode_scanner_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterBarcodeScannerPlatform
    with MockPlatformInterfaceMixin
    implements FlutterBarcodeScannerPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final initialPlatform = FlutterBarcodeScannerPlatform.instance;

  test('$MethodChannelFlutterBarcodeScanner is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterBarcodeScanner>());
  });

  test('getPlatformVersion', () async {
    var flutterBarcodeScannerPlugin = FlutterBarcodeScanner();
    var fakePlatform = MockFlutterBarcodeScannerPlatform();
    FlutterBarcodeScannerPlatform.instance = fakePlatform;

    //expect(await flutterBarcodeScannerPlugin.getPlatformVersion(), '42');
  });
}
