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
      final tzName = await FlutterTimezone.getLocalTimezone();
      tz.setLocalLocation(tz.getLocation(tzName));
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

    await _plugin.zonedSchedule(
      note.id.hashCode,
      note.title.isEmpty ? 'Jotter' : note.title,
      note.type == NoteType.checklist ? 'Anda memiliki checklist yang perlu diselesaikan' : note.content,
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
    await _plugin.cancel(noteId.hashCode);
  }
}
