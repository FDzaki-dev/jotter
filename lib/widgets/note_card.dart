import 'package:flutter/cupertino.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import '../models/note.dart';
import '../utils/app_colors.dart';

class NoteCard extends StatelessWidget {
  final Note note;
  final bool isGrid;
  final VoidCallback onTap;
  final VoidCallback onArchive;
  final VoidCallback onDelete;
  final String archiveLabel;
  final IconData archiveIcon;
  final Color archiveColor;
  final String deleteLabel;
  final IconData deleteIcon;

  const NoteCard({
    super.key,
    required this.note,
    required this.isGrid,
    required this.onTap,
    required this.onArchive,
    required this.onDelete,
    this.archiveLabel = 'Arsip',
    this.archiveIcon = CupertinoIcons.archivebox_fill,
    this.archiveColor = CupertinoColors.systemOrange,
    this.deleteLabel = 'Hapus',
    this.deleteIcon = CupertinoIcons.delete_solid,
  });

  @override
  Widget build(BuildContext context) {
    final borderColor = noteColorFor(note.colorIndex);

    final card = GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: CupertinoColors.systemBackground.resolveFrom(context),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: borderColor, width: 1.5),
          boxShadow: [
            BoxShadow(color: CupertinoColors.black.withOpacity(0.04), blurRadius: 6, offset: const Offset(0, 2)),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Container(width: 10, height: 10, decoration: BoxDecoration(color: borderColor, shape: BoxShape.circle)),
                const SizedBox(width: 6),
                if (note.isLocked) const Icon(CupertinoIcons.lock_fill, size: 14, color: CupertinoColors.systemGrey),
                const Spacer(),
                if (note.reminderAt != null) const Icon(CupertinoIcons.bell_fill, size: 14, color: CupertinoColors.systemGrey),
              ],
            ),
            const SizedBox(height: 6),
            if (note.title.isNotEmpty)
              Text(note.isLocked ? 'Catatan Terkunci' : note.title,
                  maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
            const SizedBox(height: 4),
            if (note.isLocked)
              const Text('•••••••', style: TextStyle(color: CupertinoColors.systemGrey))
            else if (note.type == NoteType.checklist)
              ..._checklistPreview()
            else
              Text(note.content,
                  maxLines: isGrid ? 4 : 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(color: CupertinoColors.secondaryLabel, fontSize: 13)),
          ],
        ),
      ),
    );

    return Slidable(
      key: ValueKey(note.id),
      endActionPane: ActionPane(
        motion: const DrawerMotion(),
        extentRatio: 0.5,
        children: [
          SlidableAction(onPressed: (_) => onArchive(), backgroundColor: archiveColor, icon: archiveIcon, label: archiveLabel),
          SlidableAction(onPressed: (_) => onDelete(), backgroundColor: CupertinoColors.systemRed, icon: deleteIcon, label: deleteLabel),
        ],
      ),
      child: card,
    );
  }

  List<Widget> _checklistPreview() {
    final items = note.checklistItems.take(3).toList();
    return items.map((item) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(item.isChecked ? CupertinoIcons.checkmark_circle_fill : CupertinoIcons.circle,
              size: 14, color: CupertinoColors.systemGrey),
          const SizedBox(width: 4),
          Flexible(
            child: Text(item.text,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                    fontSize: 13,
                    decoration: item.isChecked ? TextDecoration.lineThrough : null,
                    color: CupertinoColors.secondaryLabel)),
          ),
        ],
      );
    }).toList();
  }
}
