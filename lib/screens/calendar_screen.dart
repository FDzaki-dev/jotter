import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import 'package:table_calendar/table_calendar.dart';
import '../models/note.dart';
import '../providers/notes_provider.dart';
import '../utils/app_colors.dart';
import 'note_editor_screen.dart';
import 'lock_screen.dart';

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  DateTime _selectedDay = DateTime.now();
  DateTime _focusedDay = DateTime.now();
  List<Note> _allReminders = [];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final notes = await context.read<NotesProvider>().loadNotesWithReminders();
    if (mounted) setState(() => _allReminders = notes);
  }

  List<Note> _notesForDay(DateTime day) {
    return _allReminders.where((n) {
      final r = n.reminderAt;
      if (r == null) return false;
      return r.year == day.year && r.month == day.month && r.day == day.day;
    }).toList();
  }

  Future<void> _openNote(Note note) async {
    if (note.isLocked) {
      final unlocked = await Navigator.of(context).push<bool>(
        CupertinoPageRoute(builder: (_) => const LockScreen(mode: LockMode.verify)),
      );
      if (unlocked != true) return;
    }
    if (!mounted) return;
    await Navigator.of(context).push(CupertinoPageRoute(builder: (_) => NoteEditorScreen(note: note)));
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final dayNotes = _notesForDay(_selectedDay);
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(middle: Text('Kalender')),
      child: SafeArea(
        child: Column(
          children: [
            TableCalendar<Note>(
              firstDay: DateTime.utc(2015, 1, 1),
              lastDay: DateTime.utc(2035, 12, 31),
              focusedDay: _focusedDay,
              selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
              eventLoader: _notesForDay,
              onDaySelected: (selected, focused) {
                setState(() {
                  _selectedDay = selected;
                  _focusedDay = focused;
                });
              },
              calendarStyle: const CalendarStyle(
                todayDecoration: BoxDecoration(color: CupertinoColors.systemGrey4, shape: BoxShape.circle),
                selectedDecoration: BoxDecoration(color: CupertinoColors.activeBlue, shape: BoxShape.circle),
                markerDecoration: BoxDecoration(color: CupertinoColors.systemRed, shape: BoxShape.circle),
              ),
              headerStyle: const HeaderStyle(formatButtonVisible: false, titleCentered: true),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: dayNotes.isEmpty
                  ? const Center(child: Text('Tidak ada pengingat', style: TextStyle(color: CupertinoColors.systemGrey)))
                  : ListView.builder(
                      itemCount: dayNotes.length,
                      itemBuilder: (ctx, i) {
                        final n = dayNotes[i];
                        return CupertinoListTile(
                          leading: Container(
                              width: 12, height: 12, decoration: BoxDecoration(color: noteColorFor(n.colorIndex), shape: BoxShape.circle)),
                          title: Text(n.title.isEmpty ? '(Tanpa judul)' : n.title),
                          subtitle: Text(
                              '${n.reminderAt!.hour.toString().padLeft(2, '0')}:${n.reminderAt!.minute.toString().padLeft(2, '0')}'),
                          onTap: () => _openNote(n),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
