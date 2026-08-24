# AUDIT_ISSUES — Jotter

## [Audit Inspeksi Mendalam] — 2026-08-24 (basis: v1_Batch9, commit belum diverifikasi CI)
Metode: static review manual seluruh `lib/**/*.dart` (1665 baris), seluruh Protected Assets Android/Gradle, `.github/workflows/release.yml`, `.gitignore/.gitattributes`. Tidak ada Flutter SDK di sandbox → tidak bisa `flutter analyze`/compile asli, semua temuan dari pembacaan kode + 1 verifikasi API pihak-ketiga via web search.

---

### 🔴 CRITICAL

**1. ✅ RESOLVED (v1_Batch11) — Reminder notifikasi membocorkan isi note yang dikunci (PIN/biometric bypass via notifikasi)**
- File: `lib/services/notification_service.dart:48-63`
- `scheduleReminder()` mengisi title notifikasi = `note.title` dan body = `note.content` (utk note teks) TANPA cek `note.isLocked`. Note terkunci tetap kirim notifikasi berisi judul+isi asli ke tray/lockscreen HP.
- Note checklist sedikit lebih aman (body generik "Anda memiliki checklist..."), tapi title tetap bocor.
- Dampak: fitur kunci PIN/biometrik jebol total lewat notifikasi untuk note teks yang dikunci + reminder aktif.

**2. ✅ RESOLVED (v1_Batch12) — Reminder tidak dibatalkan saat note dihapus**
- File: `lib/providers/notes_provider.dart` (`trashNote`, `permanentDelete`) + `lib/repositories/note_repository.dart:84` (`emptyTrash`)
- Tidak ada satupun pemanggilan `NotificationService().cancelReminder()` di path hapus/sampah/empty-trash. Hanya dipanggil dari `note_editor_screen.dart:40-42` saat note disimpan.
- Dampak: note yang sudah dihapus/permanent-delete masih bisa memicu notifikasi reminder "hantu".

**3. ✅ RESOLVED* (v1_Batch13) — Reminder hilang permanen setelah restart HP (tidak ada reschedule)**
*_Resolved via reschedule-on-app-open (bukan instant-on-boot native receiver — lihat catatan residual di PROJECT_STATE v1_Batch13)._
- Cek: tidak ada `RECEIVE_BOOT_COMPLETED` di `AndroidManifest.xml`, tidak ada BroadcastReceiver, dan `getNotesWithReminders()` (`note_repository.dart:45`) hanya dipakai `calendar_screen.dart:29` untuk tampilan kalender — bukan untuk re-arm alarm saat start.
- `flutter_local_notifications` pakai AlarmManager yang di-clear OS setiap reboot. Tanpa reschedule saat app start, semua reminder yang sudah dijadwalkan otomatis hangus tiap kali HP restart, tanpa pemberitahuan ke user.

**4. ✅ RESOLVED (v1_Batch14) — Notification ID dari `note.id.hashCode` berisiko out-of-range**
- File: `lib/services/notification_service.dart:49,67`
- `String.hashCode` Dart (VM/AOT, umumnya 64-bit APK arm64) tidak dijamin muat di 32-bit int Android (`NotificationManager` butuh Java `int`). Ini anti-pattern yang sudah dikenal luas di ekosistem flutter_local_notifications. Berisiko exception/ID tidak konsisten pada sebagian device — perlu diverifikasi dengan test nyata di CI/device.

---

### 🟠 HIGH

**5. ✅ RESOLVED (v1_Batch15) — Judul note terkunci TIDAK tersamarkan (hanya isi yang disamarkan)**
- File: `lib/widgets/note_card.dart:62-67` — body diganti "•••••••" saat `isLocked`, tapi `note.title` tetap dirender polos di baris 62-64 tanpa syarat.
- Sama di `lib/screens/calendar_screen.dart:91` (`title: Text(n.title...)`) — daftar reminder kalender juga menampilkan judul note terkunci tanpa masking.
- Dampak: judul note terkunci (yang seringkali berisi info sensitif) terlihat bebas di grid/list Home dan tab Kalender tanpa perlu buka kunci sama sekali.

**6. ✅ RESOLVED (v1_Batch17) — Tab Kalender tidak reaktif — data reminder basi (stale)**
- File: `lib/screens/calendar_screen.dart:22-31` — `_load()` hanya dipanggil sekali di `initState`. `CupertinoTabView` mempertahankan state tab (tidak dispose saat pindah tab), dan `CalendarScreen` tidak `watch` `NotesProvider` sama sekali (beda dgn `HomeScreen` yg reaktif).
- Dampak: tambah/edit/hapus reminder dari tab Catatan tidak muncul di tab Kalender sampai app di-restart penuh.

**7. `modifiedAt` ikut berubah hanya dengan membuka lalu menutup note (tanpa edit apapun)**
- File: `lib/screens/note_editor_screen.dart:33-44` — `_saveAndPop()` selalu memanggil `saveNote()` untuk note existing, tidak ada dirty-check (bandingkan state awal vs akhir).
- Dampak: sort "Waktu Diubah" berantakan hanya krn user intip note tanpa mengubah apapun; reminder juga ikut di-cancel+reschedule ulang tiap kali note dibuka (boros, walau tidak merusak data).

---

### 🟡 MEDIUM

**8. `TextEditingController` tidak pernah di-`dispose()` — di SELURUH project (0 hasil grep)**
- `lib/screens/home_screen.dart:18` (`_searchController`), `lib/screens/note_editor_screen.dart:19-21` (`_titleCtrl`, `_contentCtrl`, `_newItemCtrl`).
- Tidak ada override `dispose()` di kedua State class ini. Memory leak terakumulasi setiap kali buka/tutup note atau rebuild search field.

**9. Toggle "Gunakan Biometrik" gagal senyap tanpa pesan error**
- File: `lib/screens/settings_screen.dart:56-62` — jika `canUseBiometrics()` `false`, switch cuma `return` tanpa dialog penjelasan ke user kenapa toggle tidak menyala.

**10. Potensi crash `use-after-dispose` (minor/edge-case)**
- File: `lib/screens/filtered_notes_screen.dart:41-44`, `lib/screens/calendar_screen.dart:48-51` — `_load()` dipanggil setelah `await Navigator.push(...)` tanpa recheck `mounted` tepat sebelumnya (guard `mounted` yang ada hanya menjaga sebelum `push`, bukan sesudah).

---

### ⚪ LOW / Dokumentasi

**11. README manual-build tidak sinkron dgn CI** — `README.md` mencontohkan `flutter build apk --release` (fat APK), padahal `release.yml` (Batch8) sudah pakai `--split-per-abi`. Build manual lokal akan hasilkan APK gemuk yang berbeda dari rilis GitHub.

**12. Stale-run-guard di `release.yml` diletakkan SETELAH step build** (baris build → baru guard). Tidak melanggar aturan ("abort sebelum rilis" tetap terpenuhi), tapi boros menit CI karena build APK penuh dulu baru dibatalkan kalau run basi.

---

### 📌 Catatan proses (bukan bug kode, tapi status risiko)
- **Belum ada satupun run CI hijau yang terkonfirmasi.** Setiap entri Batch2–Batch9 di `PROJECT_STATE.md` diakhiri "belum diverifikasi compile penuh". Seluruh chain fix toolchain (Gradle 8.14/AGP 8.11.1/Kotlin 2.2.20/desugaring/flutter_timezone 5.x) masih teoritis sampai ada 1 run CI yang benar-benar hijau end-to-end.
- Verifikasi via web search (hari ini): parameter `uiLocalNotificationDateInterpretation` (Batch5) baru dihapus plugin di v18.0.0+; pubspec pin `^17.2.2` → resolve tetap di jalur 17.x → parameter itu masih valid, keputusan Batch5 **konsisten benar** selama dependency tidak di-bump ke `^18.x` tanpa menghapus parameter tsb bersamaan.

---

## Ringkasan jumlah
| Severity | Jumlah |
|---|---|
| Critical | 4 (4 RESOLVED) |
| High | 3 |
| Medium | 3 |
| Low/Dok | 2 |

---

## UX/UI POLISH BACKLOG (sumber: verdict eksternal `Jotter_v1_Batch14_UX_UI_POLISH.md`, ditanam v1_Batch16)
Status keseluruhan menurut verdict: ~80-85% polished, TIDAK perlu refactor/redesign besar — surgical micro-fix only. Constraint eksplisit: DILARANG refactor architecture, ganti Provider/nav, Cupertino→Material, redesign Home/Editor, ganti DB schema, animasi berlebihan, ubah struktur tab, atau fitur baru yang tidak perlu.

### P0 — WAJIB
- **P0.1 Locked note masking (title+content)** — ✅ SUDAH RESOLVED lewat audit #1 (v1_Batch11, notif) + #5 (v1_Batch15, title Home+Kalender). Diverifikasi ulang: `filtered_notes_screen.dart` (Arsip/Sampah) reuse `NoteCard` yg sudah masking → ikut ter-cover, tidak ada celah baru.
- **P0.2 Calendar synchronization (stale setelah create/edit/delete reminder)** — ✅ RESOLVED v1_Batch17 (= audit High #6).
- **P0.3 Lock/Biometric feedback jelas (termasuk state/CTA lock screen)** — sebagian DUPLIKAT audit Medium #9 (toggle biometric senyap), diperluas: lock screen (`lock_screen.dart`) juga perlu state/CTA lebih jelas (belum ada entri audit sebelumnya utk lock screen CTA — item baru, gabung ke #9 saat eksekusi).

### P1 — PRIORITAS (item baru, belum ada di AUDIT_ISSUES sebelumnya)
- P1.4 Editor dirty-state (jangan save kalau tidak ada perubahan) — OVERLAP dgn audit High #7 (modifiedAt berubah tanpa edit), root cause sama (`note_editor_screen.dart` tidak ada dirty-check). Gabung jadi 1 eksekusi dgn #7.
- P1.5 Action feedback (Archive/Delete/Restore/Reminder/Lock butuh toast/snackbar singkat) — baru.
- P1.6 Konfirmasi sebelum permanent delete (irreversible) — baru.
- P1.7 Empty states lebih actionable (konteks + next action, terutama Home) — baru.
- P1.8 Discoverability color picker & swipe actions + reminder tampilkan status/tanggal lebih informatif — baru.
- P1.9 Checklist: perjelas feedback add/remove item + perbesar target tombol `X` — baru.

### P2 — POLISH (item baru)
- P2.10 Perkuat typography hierarchy.
- P2.11 Rapikan spacing/proportion NoteCard (terutama Grid).
- P2.12 Haluskan color/border treatment.
- P2.13 Kurangi info developer-facing di Settings (path log internal).

**Execution order (dari verdict):** Logic P0 → UX P0 → Logic P1 → UX P1 → visual polish → FREEZE.
**Next batch:** lanjut ke P0.2 (= audit High #6, Calendar sync) sesuai urutan Logic P0.

