import '../db/database_helper.dart';
import '../models/note.dart';
import '../services/notification_service.dart';

enum SortMode { modified, created, alphabetical, color }

class NoteRepository {
  final _dbHelper = DatabaseHelper.instance;

  Future<List<Note>> getNotes({
    required bool archived,
    required bool deleted,
    String query = '',
    SortMode sortMode = SortMode.modified,
  }) async {
    final db = await _dbHelper.database;
    final maps = await db.query(
      'notes',
      where: 'isArchived = ? AND isDeleted = ?',
      whereArgs: [archived ? 1 : 0, deleted ? 1 : 0],
    );
    var notes = maps.map((m) => Note.fromMap(m)).toList();

    if (query.trim().isNotEmpty) {
      final q = query.trim().toLowerCase();
      notes = notes.where((n) => n.searchableText.contains(q)).toList();
    }

    switch (sortMode) {
      case SortMode.modified:
        notes.sort((a, b) => b.modifiedAt.compareTo(a.modifiedAt));
        break;
      case SortMode.created:
        notes.sort((a, b) => b.createdAt.compareTo(a.createdAt));
        break;
      case SortMode.alphabetical:
        notes.sort((a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()));
        break;
      case SortMode.color:
        notes.sort((a, b) => a.colorIndex.compareTo(b.colorIndex));
        break;
    }
    return notes;
  }

  Future<List<Note>> getNotesWithReminders() async {
    final db = await _dbHelper.database;
    final maps = await db.query('notes', where: 'isDeleted = 0 AND reminderAt IS NOT NULL');
    return maps.map((m) => Note.fromMap(m)).toList();
  }

  Future<void> insert(Note note) async {
    final db = await _dbHelper.database;
    await db.insert('notes', note.toMap());
  }

  Future<void> update(Note note) async {
    final db = await _dbHelper.database;
    note.modifiedAt = DateTime.now();
    await db.update('notes', note.toMap(), where: 'id = ?', whereArgs: [note.id]);
  }

  Future<void> setArchived(String id, bool value) async {
    final db = await _dbHelper.database;
    await db.update('notes', {'isArchived': value ? 1 : 0, 'modifiedAt': DateTime.now().millisecondsSinceEpoch},
        where: 'id = ?', whereArgs: [id]);
  }

  Future<void> setDeleted(String id, bool value) async {
    final db = await _dbHelper.database;
    await db.update('notes', {'isDeleted': value ? 1 : 0, 'modifiedAt': DateTime.now().millisecondsSinceEpoch},
        where: 'id = ?', whereArgs: [id]);
  }

  Future<void> setLocked(String id, bool value) async {
    final db = await _dbHelper.database;
    await db.update('notes', {'isLocked': value ? 1 : 0}, where: 'id = ?', whereArgs: [id]);
  }

  Future<void> permanentDelete(String id) async {
    final db = await _dbHelper.database;
    await db.delete('notes', where: 'id = ?', whereArgs: [id]);
  }

  Future<void> emptyTrash() async {
    final db = await _dbHelper.database;
    final trashed = await db.query('notes', columns: ['id'], where: 'isDeleted = 1');
    for (final row in trashed) {
      await NotificationService().cancelReminder(row['id'] as String);
    }
    await db.delete('notes', where: 'isDeleted = 1');
  }
}
