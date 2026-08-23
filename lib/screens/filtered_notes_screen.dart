import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import '../models/note.dart';
import '../providers/notes_provider.dart';
import '../widgets/note_card.dart';
import 'note_editor_screen.dart';
import 'lock_screen.dart';

enum FilteredMode { archive, trash }

class FilteredNotesScreen extends StatefulWidget {
  final FilteredMode mode;
  const FilteredNotesScreen({super.key, required this.mode});

  @override
  State<FilteredNotesScreen> createState() => _FilteredNotesScreenState();
}

class _FilteredNotesScreenState extends State<FilteredNotesScreen> {
  List<Note> _items = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final provider = context.read<NotesProvider>();
    final items = widget.mode == FilteredMode.archive ? await provider.loadArchived() : await provider.loadTrashed();
    if (mounted) setState(() => _items = items);
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
    final provider = context.read<NotesProvider>();
    final isArchive = widget.mode == FilteredMode.archive;
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(middle: Text(isArchive ? 'Arsip' : 'Sampah')),
      child: SafeArea(
        child: _items.isEmpty
            ? Center(child: Text(isArchive ? 'Arsip kosong' : 'Sampah kosong', style: const TextStyle(color: CupertinoColors.systemGrey)))
            : ListView.builder(
                padding: const EdgeInsets.all(12),
                itemCount: _items.length,
                itemBuilder: (ctx, i) {
                  final note = _items[i];
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: NoteCard(
                      note: note,
                      isGrid: false,
                      archiveLabel: isArchive ? 'Buka Arsip' : 'Pulihkan',
                      archiveIcon: isArchive ? CupertinoIcons.archivebox : CupertinoIcons.arrow_uturn_left,
                      archiveColor: isArchive ? CupertinoColors.systemBlue : CupertinoColors.systemGreen,
                      deleteLabel: isArchive ? 'Hapus' : 'Hapus Permanen',
                      onTap: () => _openNote(note),
                      onArchive: () async {
                        if (isArchive) {
                          await provider.unarchiveNote(note.id);
                        } else {
                          await provider.restoreNote(note.id);
                        }
                        _load();
                      },
                      onDelete: () async {
                        if (isArchive) {
                          await provider.trashNote(note.id);
                        } else {
                          await provider.permanentDelete(note.id);
                        }
                        _load();
                      },
                    ),
                  );
                },
              ),
      ),
    );
  }
}
