import 'dart:convert';

import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/timezone.dart' as tz;
import 'package:timezone/data/latest_all.dart' as tzdata;
import 'package:flutter_timezone/flutter_timezone.dart';
import '../models/note.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  bool _initialized = false;

  Future<void> init() async {
    if (_initialized) return;
    tzdata.initializeTimeZones();
    try {
      final tzInfo = await FlutterTimezone.getLocalTimezone();
      tz.setLocalLocation(tz.getLocation(tzInfo.identifier));
    } catch (_) {
      tz.setLocalLocation(tz.getLocation('UTC'));
    }

    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const initSettings = InitializationSettings(android: androidInit);
    await _plugin.initialize(initSettings);

    final androidImpl =
        _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
    await androidImpl?.requestNotificationsPermission();
    await androidImpl?.createNotificationChannel(const AndroidNotificationChannel(
      'jotter_reminders',
      'Pengingat Catatan',
      description: 'Notifikasi pengingat untuk catatan Jotter',
      importance: Importance.high,
    ));

    _initialized = true;
  }

  Future<void> scheduleReminder(Note note) async {
    if (note.reminderAt == null) return;
    await cancelReminder(note.id);
    final scheduledDate = tz.TZDateTime.from(note.reminderAt!, tz.local);
    if (scheduledDate.isBefore(tz.TZDateTime.now(tz.local))) return;

    final String title = note.isLocked ? 'Jotter' : (note.title.isEmpty ? 'Jotter' : note.title);
    final String body = note.isLocked
        ? 'Anda memiliki catatan terkunci yang perlu diperiksa'
        : (note.type == NoteType.checklist
            ? 'Anda memiliki checklist yang perlu diselesaikan'
            : note.content);

    await _plugin.zonedSchedule(
      _stableNotificationId(note.id),
      title,
      body,
      scheduledDate,
      const NotificationDetails(
        android: AndroidNotificationDetails(
          'jotter_reminders',
          'Pengingat Catatan',
          importance: Importance.high,
          priority: Priority.high,
        ),
      ),
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      uiLocalNotificationDateInterpretation: UILocalNotificationDateInterpretation.absoluteTime,
    );
  }

  Future<void> cancelReminder(String noteId) async {
    await _plugin.cancel(_stableNotificationId(noteId));
  }

  /// FNV-1a 32-bit hash, dijalankan manual di atas byte UTF-8 `id`.
  /// Dijamin selalu positif & muat di 32-bit int Android (`NotificationManager`
  /// butuh Java int) — TIDAK bergantung pada `String.hashCode` bawaan Dart,
  /// yang implementasinya bisa berbeda antar versi/platform Dart VM (AOT arm64
  /// pakai int 64-bit) dan tidak dijamin muat di 32-bit.
  int _stableNotificationId(String id) {
    const int fnvPrime = 0x01000193;
    int hash = 0x811c9dc5;
    for (final byte in utf8.encode(id)) {
      hash ^= byte;
      hash = (hash * fnvPrime) & 0xFFFFFFFF;
    }
    return hash & 0x7FFFFFFF;
  }
}
