import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

class DatabaseHelper {
  DatabaseHelper._internal();
  static final DatabaseHelper instance = DatabaseHelper._internal();
  static Database? _db;

  Future<Database> get database async {
    _db ??= await _initDb();
    return _db!;
  }

  Future<Database> _initDb() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'jotter.db');
    return openDatabase(
      path,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE notes (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL DEFAULT '',
            content TEXT NOT NULL DEFAULT '',
            type TEXT NOT NULL DEFAULT 'text',
            checklistItems TEXT NOT NULL DEFAULT '[]',
            colorIndex INTEGER NOT NULL DEFAULT 0,
            createdAt INTEGER NOT NULL,
            modifiedAt INTEGER NOT NULL,
            reminderAt INTEGER,
            isArchived INTEGER NOT NULL DEFAULT 0,
            isDeleted INTEGER NOT NULL DEFAULT 0,
            isLocked INTEGER NOT NULL DEFAULT 0
          )
        ''');
        await db.execute('CREATE INDEX idx_notes_flags ON notes (isArchived, isDeleted)');
      },
    );
  }
}
