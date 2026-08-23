package com.jotter.notes

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channelName = "com.jotter.notes/crashlogger"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).setMethodCallHandler { call, result ->
            if (call.method == "saveCrashLog") {
                try {
                    val fileName = call.argument<String>("fileName") ?: "crash_unknown.txt"
                    val content = call.argument<String>("content") ?: ""
                    val appName = call.argument<String>("appName") ?: "Jotter"
                    CrashLogWriter.write(applicationContext, appName, fileName, content)
                    result.success(true)
                } catch (e: Exception) {
                    result.error("SAVE_FAILED", e.message, null)
                }
            } else {
                result.notImplemented()
            }
        }
    }
}
