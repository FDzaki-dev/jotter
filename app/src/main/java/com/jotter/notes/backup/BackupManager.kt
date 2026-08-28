package com.jotter.notes.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.jotter.notes.data.AppDatabase
import com.jotter.notes.data.ChecklistItem
import com.jotter.notes.data.Note
import com.jotter.notes.data.NoteType
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

sealed class BackupResult {
    data class Success(val fileName: String, val noteCount: Int) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

/** 1 file backup ditemukan di `Documents/<App>/backup/` - dipakai utk deteksi & tampilan
 * "restore dari backup tanggal X" (UI batch berikutnya). */
data class BackupFileInfo(val uri: Uri, val fileName: String, val dateAddedSeconds: Long)

sealed class RestoreResult {
    /** [lockedPlaceholderCount] = berapa dari notes yang direstore itu locked-note yang isinya
     * CUMA placeholder (bukan konten asli) - lihat catatan keamanan di [BackupManager.backup]. */
    data class Success(val restoredCount: Int, val lockedPlaceholderCount: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

/**
 * Backup semua notes lokal (termasuk arsip/sampah/terkunci - full dump, bukan cuma yang
 * keliatan di 1 tab tertentu) jadi 1 file JSON, ditulis via MediaStore (API 29+) - PATHWAY &
 * TEKNIK PERSIS SAMA dgn `CrashLogWriter.kt` (insert ContentValues + RELATIVE_PATH
 * `Documents/<App>/...`, TANPA butuh permission WRITE_EXTERNAL_STORAGE krn scoped storage).
 *
 * 2 PERBEDAAN SENGAJA dari CrashLogWriter (dicatat, BUKAN kelalaian):
 * 1. TIDAK ADA retention/FIFO cap. Crash log itu diagnostik & auto-generate tiap crash (bisa
 *    sering, gak semua penting disimpan lama2). Backup MEMANG di-trigger manual user - auto-hapus
 *    backup lama diam2 justru kontraproduktif utk fitur yang tujuannya jadi jaring pengaman data.
 *    Volume juga natural jauh lebih rendah (manual, bukan tiap error/tiap save).
 * 2. Note yang `isLocked = true` TIDAK ikut disertakan title/content-nya (diganti placeholder,
 *    metadata lain tetap ada). Locked note MEMANG didesain masking di seluruh UI (audit P0.1) -
 *    kalau backup nulis isinya mentah2 ke file JSON di folder Documents (bisa dibuka app File
 *    Manager mana pun, atau ke-ikut auto-backup cloud OS), itu SAMA SAJA bikin bocor persis yang
 *    coba dicegah fitur kunci. Belum ada infra enkripsi utk restore-kan isi asli locked note
 *    (butuh desain terpisah, mis. reuse `androidx.security-crypto`/Tink yang udah ada di project
 *    sejak Batch4 - dicatat sbg follow-up candidate kalau user mau, BUKAN diasumsikan otomatis).
 *
 * [findLatestBackup] + [restore]: deteksi & pulihkan dari backup PALING BARU di folder
 * `Documents/<App>/backup/` - cover skenario app di-uninstall lalu install ulang (folder
 * Documents TIDAK ikut kehapus krn di luar sandbox app, beda dari data internal app yang
 * kehapus pas uninstall) ATAU DB app corrupt/kosong tiba2 ("galat aplikasi"). KAPAN check ini
 * dijalankan (mis. app dibuka & `count() == 0`) itu scope UI batch berikutnya - BELUM
 * disambungkan ke UI/trigger apapun di batch ini, murni logic dulu.
 */
object BackupManager {

    suspend fun backup(context: Context, appName: String): BackupResult {
        return try {
            val notes = AppDatabase.getInstance(context).noteDao().getAllForBackup()
            val json = notesToJson(notes)

            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
            val fileName = "jotter_backup_$ts.json"
            val relativePath = Environment.DIRECTORY_DOCUMENTS + "/" + appName + "/backup"

            val resolver = context.applicationContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                ?: return BackupResult.Error("Gagal membuat file backup (MediaStore menolak)")

            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toString(2).toByteArray())
            } ?: return BackupResult.Error("Gagal membuka stream file backup")

            BackupResult.Success(fileName, notes.size)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Gagal membuat backup")
        }
    }

    /** Cari file backup PALING BARU di `Documents/<App>/backup/` (sort DATE_ADDED DESC, ambil
     * baris pertama) - null kalau folder belum ada isinya sama sekali (mis. user belum pernah
     * backup) atau query gagal. Pola query PERSIS sama gayanya dgn `enforceRetention` di
     * CrashLogWriter.kt (selection RELATIVE_PATH + trailing slash), cuma sort arahnya dibalik
     * (DESC drpd ASC) krn di sini butuh yang TERBARU, bukan yang TERLAMA buat dihapus. */
    fun findLatestBackup(context: Context, appName: String): BackupFileInfo? {
        return try {
            val relativePath = Environment.DIRECTORY_DOCUMENTS + "/" + appName + "/backup"
            val resolver = context.applicationContext.contentResolver
            val uriExternal = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED
            )
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf("$relativePath/")
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            resolver.query(uriExternal, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    val date = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                    return BackupFileInfo(MediaStore.Files.getContentUri("external", id), name, date)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Baca file backup dari [uri], parse balik jadi [Note] (invers dari [notesToJson]), lalu
     * `upsert` satu2 ke DB (REPLACE by id - restore ke note yang KEBETULAN masih ada/id sama
     * akan ketimpa versi backup, itu memang perilaku restore yang diharapkan). Parsing pakai
     * `opt*` (bukan `get*`) di semua field selain `id` - defensif thd backup lama/field hilang,
     * gak langsung crash cuma krn 1 field gak ketemu. */
    suspend fun restore(context: Context, uri: Uri): RestoreResult {
        return try {
            val text = context.applicationContext.contentResolver.openInputStream(uri)
                ?.use { it.bufferedReader().readText() }
                ?: return RestoreResult.Error("Gagal membaca file backup")

            val json = JSONObject(text)
            val notesArray = json.getJSONArray("notes")
            val dao = AppDatabase.getInstance(context).noteDao()

            var restored = 0
            var lockedPlaceholder = 0
            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                val isLocked = obj.optBoolean("isLocked", false)
                if (isLocked) lockedPlaceholder++

                val checklistArray = obj.optJSONArray("checklistItems") ?: JSONArray()
                val checklistItems = (0 until checklistArray.length()).map { idx ->
                    val itemObj = checklistArray.getJSONObject(idx)
                    ChecklistItem(
                        id = itemObj.optString("id", UUID.randomUUID().toString()),
                        text = itemObj.optString("text", ""),
                        isChecked = itemObj.optBoolean("isChecked", false)
                    )
                }

                dao.upsert(
                    Note(
                        id = obj.getString("id"),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        type = if (obj.optString("type") == "CHECKLIST") NoteType.CHECKLIST else NoteType.TEXT,
                        checklistItems = checklistItems,
                        colorIndex = obj.optInt("colorIndex", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        modifiedAt = obj.optLong("modifiedAt", System.currentTimeMillis()),
                        reminderAt = if (obj.isNull("reminderAt")) null else obj.optLong("reminderAt"),
                        isArchived = obj.optBoolean("isArchived", false),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        isLocked = isLocked,
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
                restored++
            }
            RestoreResult.Success(restored, lockedPlaceholder)
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Gagal memproses file backup")
        }
    }

    /** JSON per-note ditulis sbg OBJECT asli (checklist NESTED sbg array asli), BUKAN reuse
     * `Converters.fromChecklist` (itu utk 1 kolom Room, hasilnya string ter-encode 2x) - biar
     * file backup gampang dibaca manusia & gampang diparse balik pas fitur restore dikerjakan. */
    private fun notesToJson(notes: List<Note>): JSONObject {
        val notesArray = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject()
            obj.put("id", note.id)
            if (note.isLocked) {
                obj.put("title", "[TERKUNCI - konten tidak disertakan demi keamanan]")
                obj.put("content", "")
                obj.put("checklistItems", JSONArray())
            } else {
                obj.put("title", note.title)
                obj.put("content", note.content)
                val itemsArray = JSONArray()
                note.checklistItems.forEach { item ->
                    itemsArray.put(JSONObject().apply {
                        put("id", item.id)
                        put("text", item.text)
                        put("isChecked", item.isChecked)
                    })
                }
                obj.put("checklistItems", itemsArray)
            }
            obj.put("type", note.type.name)
            obj.put("colorIndex", note.colorIndex)
            obj.put("createdAt", note.createdAt)
            obj.put("modifiedAt", note.modifiedAt)
            obj.put("reminderAt", note.reminderAt ?: JSONObject.NULL)
            obj.put("isArchived", note.isArchived)
            obj.put("isDeleted", note.isDeleted)
            obj.put("isLocked", note.isLocked)
            obj.put("isPinned", note.isPinned)
            notesArray.put(obj)
        }
        return JSONObject().apply {
            put("app", "Jotter")
            put("backupVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("noteCount", notes.size)
            put("notes", notesArray)
        }
    }
}
