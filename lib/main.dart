import 'package:flutter/cupertino.dart';
import 'services/crash_logger_service.dart';
import 'app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await CrashLoggerService.init();
  await CrashLoggerService.runGuarded(() async {
    runApp(const JotterApp());
  });
}
