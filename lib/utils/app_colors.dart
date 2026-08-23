import 'package:flutter/cupertino.dart';

class NoteColorOption {
  final String label;
  final Color color;
  const NoteColorOption(this.label, this.color);
}

const List<NoteColorOption> kNoteColors = [
  NoteColorOption('Merah', CupertinoColors.systemRed),
  NoteColorOption('Oranye', CupertinoColors.systemOrange),
  NoteColorOption('Kuning', CupertinoColors.systemYellow),
  NoteColorOption('Hijau', CupertinoColors.systemGreen),
  NoteColorOption('Toska', CupertinoColors.systemTeal),
  NoteColorOption('Biru', CupertinoColors.systemBlue),
  NoteColorOption('Nila', CupertinoColors.systemIndigo),
  NoteColorOption('Ungu', CupertinoColors.systemPurple),
  NoteColorOption('Merah Muda', CupertinoColors.systemPink),
];

Color noteColorFor(int index) {
  if (index < 0 || index >= kNoteColors.length) return kNoteColors[0].color;
  return kNoteColors[index].color;
}
