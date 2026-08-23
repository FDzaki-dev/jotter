import 'dart:convert';

enum NoteType { text, checklist }

class ChecklistItem {
  final String id;
  String text;
  bool isChecked;

  ChecklistItem({required this.id, required this.text, this.isChecked = false});

  Map<String, dynamic> toMap() => {'id': id, 'text': text, 'isChecked': isChecked};

  factory ChecklistItem.fromMap(Map<String, dynamic> map) => ChecklistItem(
        id: map['id'] as String,
        text: map['text'] as String,
        isChecked: (map['isChecked'] as bool?) ?? false,
      );
}

class Note {
  final String id;
  String title;
  String content;
  NoteType type;
  List<ChecklistItem> checklistItems;
  int colorIndex;
  DateTime createdAt;
  DateTime modifiedAt;
  DateTime? reminderAt;
  bool isArchived;
  bool isDeleted;
  bool isLocked;

  Note({
    required this.id,
    this.title = '',
    this.content = '',
    this.type = NoteType.text,
    List<ChecklistItem>? checklistItems,
    this.colorIndex = 0,
    DateTime? createdAt,
    DateTime? modifiedAt,
    this.reminderAt,
    this.isArchived = false,
    this.isDeleted = false,
    this.isLocked = false,
  })  : checklistItems = checklistItems ?? [],
        createdAt = createdAt ?? DateTime.now(),
        modifiedAt = modifiedAt ?? DateTime.now();

  Map<String, dynamic> toMap() => {
        'id': id,
        'title': title,
        'content': content,
        'type': type.name,
        'checklistItems': jsonEncode(checklistItems.map((e) => e.toMap()).toList()),
        'colorIndex': colorIndex,
        'createdAt': createdAt.millisecondsSinceEpoch,
        'modifiedAt': modifiedAt.millisecondsSinceEpoch,
        'reminderAt': reminderAt?.millisecondsSinceEpoch,
        'isArchived': isArchived ? 1 : 0,
        'isDeleted': isDeleted ? 1 : 0,
        'isLocked': isLocked ? 1 : 0,
      };

  factory Note.fromMap(Map<String, dynamic> map) {
    final rawItems = jsonDecode(map['checklistItems'] as String? ?? '[]') as List;
    return Note(
      id: map['id'] as String,
      title: map['title'] as String? ?? '',
      content: map['content'] as String? ?? '',
      type: (map['type'] == 'checklist') ? NoteType.checklist : NoteType.text,
      checklistItems: rawItems.map((e) => ChecklistItem.fromMap(e as Map<String, dynamic>)).toList(),
      colorIndex: map['colorIndex'] as int? ?? 0,
      createdAt: DateTime.fromMillisecondsSinceEpoch(map['createdAt'] as int),
      modifiedAt: DateTime.fromMillisecondsSinceEpoch(map['modifiedAt'] as int),
      reminderAt: map['reminderAt'] != null ? DateTime.fromMillisecondsSinceEpoch(map['reminderAt'] as int) : null,
      isArchived: (map['isArchived'] as int? ?? 0) == 1,
      isDeleted: (map['isDeleted'] as int? ?? 0) == 1,
      isLocked: (map['isLocked'] as int? ?? 0) == 1,
    );
  }

  String get searchableText =>
      '$title $content ${checklistItems.map((e) => e.text).join(' ')}'.toLowerCase();
}
