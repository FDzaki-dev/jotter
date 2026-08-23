import 'dart:async';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:uuid/uuid.dart';

class CrashLoggerService {
  static const _channel = MethodChannel('com.jotter.notes/crashlogger');
  static const String appName = 'Jotter';
  static const _uuid = Uuid();

  static Future<void> init() async {
    FlutterError.onError = (FlutterErrorDetails details) {
      FlutterError.presentError(details);
      _logError(details.exceptionAsString(), details.stack ?? StackTrace.current);
    };
    PlatformDispatcher.instance.onError = (error, stack) {
      _logError(error.toString(), stack);
      return true;
    };
  }

  static Future<R?> runGuarded<R>(FutureOr<R> Function() body) async {
    R? result;
    await runZonedGuarded(() async {
      result = await body();
    }, (error, stack) {
      _logError(error.toString(), stack);
    });
    return result;
  }

  static Future<void> _logError(String error, StackTrace stack) async {
    try {
      String version = 'unknown';
      try {
        final info = await PackageInfo.fromPlatform();
        version = '${info.version}+${info.buildNumber}';
      } catch (_) {}

      String osInfo = 'Android';
      String model = 'unknown';
      try {
        if (Platform.isAndroid) {
          final androidInfo = await DeviceInfoPlugin().androidInfo;
          osInfo = 'Android ${androidInfo.version.release} (SDK ${androidInfo.version.sdkInt})';
          model = '${androidInfo.manufacturer} ${androidInfo.model}';
        }
      } catch (_) {}

      final now = DateTime.now();
      final ts = _formatTimestamp(now);
      final fileName = 'crash_${ts}_${_uuid.v4()}.txt';
      final content = StringBuffer()
        ..writeln('Version: $version')
        ..writeln('OS: $osInfo')
        ..writeln('Device: $model')
        ..writeln('Timestamp: ${now.toIso8601String()}')
        ..writeln('Thread: ${Zone.current.hashCode}')
        ..writeln('Source: dart')
        ..writeln('---')
        ..writeln('Error: $error')
        ..writeln()
        ..writeln('StackTrace:')
        ..writeln(stack.toString());

      await _channel.invokeMethod('saveCrashLog', {
        'fileName': fileName,
        'content': content.toString(),
        'appName': appName,
      });
    } catch (_) {
      // fail-safe: crash logger must never throw
    }
  }

  static String _formatTimestamp(DateTime dt) {
    String two(int n) => n.toString().padLeft(2, '0');
    return '${dt.year}${two(dt.month)}${two(dt.day)}_${two(dt.hour)}${two(dt.minute)}${two(dt.second)}';
  }
}
