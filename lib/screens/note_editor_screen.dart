import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import 'package:uuid/uuid.dart';
import '../models/note.dart';
import '../providers/notes_provider.dart';
import '../services/notification_service.dart';
import '../utils/app_colors.dart';

class NoteEditorScreen extends StatefulWidget {
  final Note? note;
  const NoteEditorScreen({super.key, this.note});

  @override
  State<NoteEditorScreen> createState() => _NoteEditorScreenState();
}

class _NoteEditorScreenState extends State<NoteEditorScreen> {
  late Note _note;
  late TextEditingController _titleCtrl;
  late TextEditingController _contentCtrl;
  final _newItemCtrl = TextEditingController();
  bool _isNew = false;

  @override
  void initState() {
    super.initState();
    _isNew = widget.note == null;
    _note = widget.note ?? Note(id: const Uuid().v4());
    _titleCtrl = TextEditingController(text: _note.title);
    _contentCtrl = TextEditingController(text: _note.content);
  }

  Future<void> _saveAndPop() async {
    _note.title = _titleCtrl.text.trim();
    _note.content = _contentCtrl.text;
    final isEmpty = _note.title.isEmpty && _note.content.isEmpty && _note.checklistItems.isEmpty;
    if (isEmpty && _isNew) return;
    await context.read<NotesProvider>().saveNote(_note);
    if (_note.reminderAt != null) {
      await NotificationService().scheduleReminder(_note);
    } else {
      await NotificationService().cancelReminder(_note.id);
    }
  }

  void _pickColor() {
    showCupertinoModalPopup(
      context: context,
      builder: (ctx) => Container(
        padding: const EdgeInsets.all(20),
        color: CupertinoColors.systemBackground.resolveFrom(context),
        child: Wrap(
          spacing: 14,
          runSpacing: 14,
          alignment: WrapAlignment.center,
          children: List.generate(kNoteColors.length, (i) {
            final selected = _note.colorIndex == i;
            return GestureDetector(
              onTap: () {
                setState(() => _note.colorIndex = i);
                Navigator.pop(ctx);
              },
              child: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: kNoteColors[i].color,
                  shape: BoxShape.circle,
                  border: selected ? Border.all(color: CupertinoColors.label.resolveFrom(context), width: 2) : null,
                ),
              ),
            );
          }),
        ),
      ),
    );
  }

  Future<void> _pickReminder() async {
    DateTime temp = _note.reminderAt ?? DateTime.now().add(const Duration(hours: 1));
    await showCupertinoModalPopup(
      context: context,
      builder: (ctx) => Container(
        height: 300,
        color: CupertinoColors.systemBackground.resolveFrom(context),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                CupertinoButton(
                  child: const Text('Hapus Pengingat'),
                  onPressed: () {
                    setState(() => _note.reminderAt = null);
                    Navigator.pop(ctx);
                  },
                ),
                CupertinoButton(
                  child: const Text('Selesai'),
                  onPressed: () {
                    setState(() => _note.reminderAt = temp);
                    Navigator.pop(ctx);
                  },
                ),
              ],
            ),
            Expanded(
              child: CupertinoDatePicker(
                mode: CupertinoDatePickerMode.dateAndTime,
                minimumDate: DateTime.now(),
                initialDateTime: temp,
                onDateTimeChanged: (d) => temp = d,
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _addChecklistItem() {
    final text = _newItemCtrl.text.trim();
    if (text.isEmpty) return;
    setState(() {
      _note.checklistItems.add(ChecklistItem(id: const Uuid().v4(), text: text));
      _newItemCtrl.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvoked: (didPop) async {
        if (didPop) return;
        await _saveAndPop();
        if (mounted) Navigator.of(context).pop();
      },
      child: CupertinoPageScaffold(
        navigationBar: CupertinoNavigationBar(
          middle: const Text('Catatan'),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              CupertinoButton(
                padding: EdgeInsets.zero,
                onPressed: _pickColor,
                child: Container(
                    width: 20, height: 20, decoration: BoxDecoration(color: noteColorFor(_note.colorIndex), shape: BoxShape.circle)),
              ),
              CupertinoButton(
                padding: EdgeInsets.zero,
                onPressed: _pickReminder,
                child: Icon(_note.reminderAt != null ? CupertinoIcons.bell_fill : CupertinoIcons.bell, size: 22),
              ),
              CupertinoButton(
                padding: EdgeInsets.zero,
                onPressed: () => setState(() => _note.isLocked = !_note.isLocked),
                child: Icon(_note.isLocked ? CupertinoIcons.lock_fill : CupertinoIcons.lock_open, size: 22),
              ),
            ],
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: CupertinoTextField(
                  controller: _titleCtrl,
                  placeholder: 'Judul',
                  decoration: const BoxDecoration(),
                  style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                ),
              ),
              Expanded(child: _note.type == NoteType.checklist ? _buildChecklist() : _buildTextEditor()),
              if (_isNew)
                Padding(
                  padding: const EdgeInsets.all(8),
                  child: CupertinoSlidingSegmentedControl<NoteType>(
                    groupValue: _note.type,
                    children: const {
                      NoteType.text: Padding(padding: EdgeInsets.symmetric(horizontal: 12), child: Text('Teks')),
                      NoteType.checklist: Padding(padding: EdgeInsets.symmetric(horizontal: 12), child: Text('Checklist')),
                    },
                    onValueChanged: (v) {
                      if (v != null) setState(() => _note.type = v);
                    },
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTextEditor() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: CupertinoTextField(
        controller: _contentCtrl,
        placeholder: 'Tulis catatan...',
        maxLines: null,
        expands: true,
        textAlignVertical: TextAlignVertical.top,
        decoration: const BoxDecoration(),
        style: const TextStyle(fontSize: 16),
      ),
    );
  }

  Widget _buildChecklist() {
    return Column(
      children: [
        Expanded(
          child: ListView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            itemCount: _note.checklistItems.length,
            itemBuilder: (ctx, i) {
              final item = _note.checklistItems[i];
              return Row(
                children: [
                  CupertinoButton(
                    padding: EdgeInsets.zero,
                    onPressed: () => setState(() => item.isChecked = !item.isChecked),
                    child: Icon(item.isChecked ? CupertinoIcons.checkmark_circle_fill : CupertinoIcons.circle,
                        color: CupertinoColors.systemGrey),
                  ),
                  Expanded(
                      child:
                          Text(item.text, style: TextStyle(decoration: item.isChecked ? TextDecoration.lineThrough : null))),
                  CupertinoButton(
                    padding: EdgeInsets.zero,
                    onPressed: () => setState(() => _note.checklistItems.removeAt(i)),
                    child: const Icon(CupertinoIcons.xmark_circle, size: 18, color: CupertinoColors.systemGrey),
                  ),
                ],
              );
            },
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              Expanded(
                child: CupertinoTextField(controller: _newItemCtrl, placeholder: 'Tambah item', onSubmitted: (_) => _addChecklistItem()),
              ),
              CupertinoButton(onPressed: _addChecklistItem, child: const Icon(CupertinoIcons.add_circled_solid)),
            ],
          ),
        ),
      ],
    );
  }
}
