import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_barcode_scanner_platform_interface.dart';

/// An implementation of [FlutterBarcodeScannerPlatform] that uses method channels.
class MethodChannelFlutterBarcodeScanner extends FlutterBarcodeScannerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_barcode_scanner');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
