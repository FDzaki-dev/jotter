import 'package:flutter/foundation.dart';
import '../models/note.dart';
import '../repositories/note_repository.dart';

enum ViewMode { list, grid }

class NotesProvider extends ChangeNotifier {
  final NoteRepository _repo = NoteRepository();

  List<Note> notes = [];
  SortMode sortMode = SortMode.modified;
  ViewMode viewMode = ViewMode.grid;
  String searchQuery = '';

  Future<void> loadActive() async {
    notes = await _repo.getNotes(archived: false, deleted: false, query: searchQuery, sortMode: sortMode);
    notifyListeners();
  }

  Future<List<Note>> loadArchived() =>
      _repo.getNotes(archived: true, deleted: false, query: searchQuery, sortMode: sortMode);

  Future<List<Note>> loadTrashed() =>
      _repo.getNotes(archived: false, deleted: true, query: searchQuery, sortMode: sortMode);

  Future<List<Note>> loadNotesWithReminders() => _repo.getNotesWithReminders();

  void setSortMode(SortMode mode) {
    sortMode = mode;
    loadActive();
  }

  void setViewMode(ViewMode mode) {
    viewMode = mode;
    notifyListeners();
  }

  void setSearchQuery(String q) {
    searchQuery = q;
    loadActive();
  }

  Future<void> saveNote(Note note) async {
    final existingIndex = notes.indexWhere((n) => n.id == note.id);
    if (existingIndex == -1) {
      await _repo.insert(note);
    } else {
      await _repo.update(note);
    }
    await loadActive();
  }

  Future<void> archiveNote(String id) async {
    await _repo.setArchived(id, true);
    await loadActive();
  }

  Future<void> unarchiveNote(String id) async {
    await _repo.setArchived(id, false);
    await loadActive();
  }

  Future<void> trashNote(String id) async {
    await _repo.setDeleted(id, true);
    await loadActive();
  }

  Future<void> restoreNote(String id) async {
    await _repo.setDeleted(id, false);
    await loadActive();
  }

  Future<void> permanentDelete(String id) async {
    await _repo.permanentDelete(id);
    await loadActive();
  }

  Future<void> setLocked(String id, bool value) async {
    await _repo.setLocked(id, value);
    await loadActive();
  }
}
