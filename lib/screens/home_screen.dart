import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import '../models/note.dart';
import '../providers/notes_provider.dart';
import '../repositories/note_repository.dart';
import '../widgets/note_card.dart';
import 'note_editor_screen.dart';
import 'lock_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<NotesProvider>().loadActive();
    });
  }

  Future<void> _openNote(Note? note) async {
    if (note != null && note.isLocked) {
      final unlocked = await Navigator.of(context).push<bool>(
        CupertinoPageRoute(builder: (_) => const LockScreen(mode: LockMode.verify)),
      );
      if (unlocked != true) return;
    }
    if (!mounted) return;
    await Navigator.of(context).push(CupertinoPageRoute(builder: (_) => NoteEditorScreen(note: note)));
    if (mounted) context.read<NotesProvider>().loadActive();
  }

  void _showSortSheet() {
    final provider = context.read<NotesProvider>();
    showCupertinoModalPopup(
      context: context,
      builder: (ctx) => CupertinoActionSheet(
        title: const Text('Urutkan berdasarkan'),
        actions: [
          _sortAction(ctx, provider, 'Waktu Diubah', SortMode.modified),
          _sortAction(ctx, provider, 'Waktu Dibuat', SortMode.created),
          _sortAction(ctx, provider, 'Abjad', SortMode.alphabetical),
          _sortAction(ctx, provider, 'Warna', SortMode.color),
        ],
        cancelButton: CupertinoActionSheetAction(onPressed: () => Navigator.pop(ctx), child: const Text('Batal')),
      ),
    );
  }

  Widget _sortAction(BuildContext ctx, NotesProvider provider, String label, SortMode mode) {
    return CupertinoActionSheetAction(
      onPressed: () {
        provider.setSortMode(mode);
        Navigator.pop(ctx);
      },
      child: Text(label),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<NotesProvider>();

    return CupertinoPageScaffold(
      child: CustomScrollView(
        slivers: [
          CupertinoSliverNavigationBar(
            largeTitle: const Text('Catatan'),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                CupertinoButton(padding: EdgeInsets.zero, onPressed: _showSortSheet, child: const Icon(CupertinoIcons.sort_down, size: 22)),
                CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: () => provider.setViewMode(provider.viewMode == ViewMode.grid ? ViewMode.list : ViewMode.grid),
                  child: Icon(
                      provider.viewMode == ViewMode.grid ? CupertinoIcons.list_bullet : CupertinoIcons.square_grid_2x2,
                      size: 22),
                ),
                CupertinoButton(padding: EdgeInsets.zero, onPressed: () => _openNote(null), child: const Icon(CupertinoIcons.add, size: 26)),
              ],
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
              child: CupertinoSearchTextField(
                controller: _searchController,
                placeholder: 'Cari catatan',
                onChanged: (v) => provider.setSearchQuery(v),
              ),
            ),
          ),
          if (provider.notes.isEmpty)
            const SliverFillRemaining(
              hasScrollBody: false,
              child: Center(child: Text('Belum ada catatan', style: TextStyle(color: CupertinoColors.systemGrey))),
            )
          else if (provider.viewMode == ViewMode.grid)
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2, mainAxisSpacing: 10, crossAxisSpacing: 10, childAspectRatio: 0.85),
                delegate: SliverChildBuilderDelegate(
                  (ctx, i) => NoteCard(
                    note: provider.notes[i],
                    isGrid: true,
                    onTap: () => _openNote(provider.notes[i]),
                    onArchive: () => provider.archiveNote(provider.notes[i].id),
                    onDelete: () => provider.trashNote(provider.notes[i].id),
                  ),
                  childCount: provider.notes.length,
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (ctx, i) => Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: NoteCard(
                      note: provider.notes[i],
                      isGrid: false,
                      onTap: () => _openNote(provider.notes[i]),
                      onArchive: () => provider.archiveNote(provider.notes[i].id),
                      onDelete: () => provider.trashNote(provider.notes[i].id),
                    ),
                  ),
                  childCount: provider.notes.length,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
