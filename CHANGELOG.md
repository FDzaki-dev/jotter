# CHANGELOG

## [v2_Batch61] - 2026-09-18
### Fixed
- Ikon mode tampilan "Petak" dan "Petak Besar" (di menu Tampilan) kini sesuai - sebelumnya ikonnya tertukar antara keduanya

## [v2_Batch60] - 2026-09-16
### Fixed
- REVERT DARURAT: perubahan transisi di versi sebelumnya (v2_Batch59) ternyata menyebabkan tampilan dua layar bertumpuk penuh saat berpindah tab - dikembalikan ke transisi instan (v2_Batch58) yang sudah terbukti tidak bermasalah

## [v2_Batch59] - 2026-09-14
### Changed
- Transisi berpindah tab dan kembali dari layar catatan kini pakai fade halus singkat (bukan instan kaku seperti sebelumnya), tanpa memunculkan lagi masalah tampilan bertumpuk

## [v2_Batch58] - 2026-09-14
### Fixed
- Dialog pembaruan aplikasi ("Sudah Terbaru" dan lainnya di bagian Pembaruan/Download) kini punya latar solid - sebelumnya tembus pandang di tema gradasi
- Dialog konfirmasi lain (Pulihkan dari Backup, Matikan Kunci PIN, Backup Sekarang, Pulihkan dari File) juga diperbaiki dengan masalah yang sama
- Transisi berpindah tab (Catatan/Kalender/Pengaturan) dan kembali dari layar catatan kini instan - sebelumnya sempat menampilkan konten layar sebelumnya bertumpuk transparan selama animasi

## [v2_Batch57] - 2026-09-14
### Fixed
- Sheet "Tampilan" dan "Urutkan" (muncul dari beranda) kini punya latar solid - sebelumnya nyaris tembus pandang di tema gradasi (Aurora/Senja/Samudra) sehingga tulisan menu bertumpuk dengan konten di belakangnya dan sulit dibaca

## [v2_Batch56] - 2026-09-14
### Fixed
- Latar kartu note kini selalu 100% opak (bug di versi sebelumnya membuat latar kartu tembus pandang di tema gradasi Aurora/Senja/Samudra, sehingga warna dan kontras jadi tidak konsisten)
- Kontras teks sekunder (isi catatan, tanggal, badge pengingat) ditingkatkan - sebelumnya memakai abu-abu yang didesain untuk latar terang, tidak cocok untuk aplikasi bertema gelap
- Judul catatan kini selalu putih terang, tidak lagi tergantung warna bawaan yang tidak konsisten
- Warna kategori per-catatan diperkuat lagi agar lebih mudah dibedakan

## [v2_Batch55] - 2026-09-14
### Fixed
- Mode tampilan Petak: kolom dikoreksi dari 2 menjadi 3 sesuai referensi asli
- Mode tampilan Petak Besar: kolom dikoreksi menjadi 2 (kartu lebih besar, bukan lebih banyak teks)
- Kartu di mode Petak/Petak Besar disederhanakan (judul + isi saja) - sebelumnya kelebihan muatan (tanggal, badge waktu pengingat) sehingga sesak dan sulit dibaca
- Warna latar kartu kini mengikuti tema aktif (sebelumnya selalu memakai basis warna gelap default, tidak konsisten di tema non-default)
- Kontras warna kategori per-catatan diperkuat agar lebih mudah dibedakan

## [v2_Batch54] - 2026-09-14
### Added
- Beranda: tanggal terakhir diubah kini tampil di setiap kartu catatan (sebelumnya tidak pernah dirender)
- Beranda: mode tampilan diperluas dari 2 (Daftar/Petak) menjadi 4 (Daftar, Detail, Petak, Petak Besar) - dipilih lewat sheet "Tampilan"
- Kartu checklist yang seluruh itemnya tercentang kini tampil abu-abu + coret (strikethrough) dengan badge centang
### Changed
- README.md: deskripsi fitur beranda diperbarui (grid/list -> 4 mode tampilan + tanggal per-kartu)

## [v2_Batch53] - 2026-08-29
### Fixed
- Theme.kt: tambah override `surfaceContainer*` (Lowest/Low/-/High/Highest) di color scheme tema gradasi - token ini sebelumnya gak disentuh & fallback ke baseline M3 gelap solid, menyebabkan bottom navigation bar (dan ModalBottomSheet) tetap hitam pekat gak nembus gradient di tema Aurora/Senja/Samudra

## [v2_Batch25] - 2026-08-26
### Changed
- NoteCard.kt: replace uniform border + header dot with left accent-color bar (4dp) + 14% background tint (lerp) - stronger per-note color identity matching original ColorNote-style spec (P2.12 color/border treatment)

## [v2_Batch24] - 2026-08-26
### Changed
- NoteCard.kt: apply titleMedium/bodySmall typography tokens (P2.10) to title/content/checklist text; card padding 12dp->14dp, header spacing 6dp->8dp, checklist item spacing added (P2.11 spacing/proportion)

## [v2_Batch3] - 2026-08-25
### Fixed
- CalendarScreen.kt, FilteredNotesScreen.kt, LockScreen.kt, SettingsScreen.kt: add missing @OptIn(ExperimentalMaterial3Api::class) for TopAppBar
- NoteEditorScreen.kt: fix fully-qualified items() call not resolving LazyListScope receiver (root cause of 6 cascading "Unresolved reference" errors) - proper import instead

## [v2_Batch2] - 2026-08-25
### Fixed
- app/build.gradle.kts: Room 2.6.1 -> 2.7.0 (confirmed google/ksp#2957 - KSP2 bug with Room suspend-Unit DAO methods)
### Added
- release.yml: failure-log artifact pathway `logs_fail_<version>_<run-number>_<sha>` via actions/upload-artifact, auto-uploads on build failure

## [v2_Batch1] - 2026-08-25 — ARSITEKTUR PIVOT
### Changed
- REWRITE TOTAL: Flutter -> Native Kotlin + Jetpack Compose (koreksi ke preferensi permanen "Anti-Flutter" yang terlewat di awal)
- Semua fitur v1_Batch21 di-porting: notes (teks/checklist), 9 warna, kalender, sort+search, archive/trash, PIN+biometric lock, grid/list, swipe actions, reminder, crash logger
- Root cause bug gesture/back (flutter/flutter#138624) TIDAK ADA lagi di arsitektur ini - Navigation Compose + BackHandler pakai OnBackPressedDispatcher asli Android, bukan shim framework
- Keystore signing di-reuse (bukan baru) - APK native ini bisa update over install Flutter lama
- CI: ganti dari Flutter toolchain ke native Gradle (gradle/actions/setup-gradle, tanpa Flutter SDK setup)

### Known limitations (lihat PROJECT_STATE.md)
- Boot receiver reminder belum diimplementasi penuh (placeholder)
- Kalender hand-rolled (bukan library sekomplit sebelumnya)
- Belum di-compile/verifikasi CI (initial commit arsitektur baru)

---
# ARSIP: CHANGELOG lengkap versi Flutter (v1_Batch1 - v1_Batch21, dihentikan)

## [v1_Batch21] - 2026-08-24
### Docs
- PROJECT_STATE.md: tambah section ATURAN PERMANEN - jotter (lowercase) untuk path/folder/repo/package, Jotter (capital) untuk nama file ZIP

## [v1_Batch20] - 2026-08-24
### Fixed
- note_editor_screen.dart: onPopInvoked -> onPopInvokedWithResult (confirmed Flutter bug flutter/flutter#138624 - old API never fires for gesture-triggered back when canPop=false, causing swipe-back to silently do nothing)

## [v1_Batch19] - 2026-08-24
### Fixed
- AndroidManifest.xml: enableOnBackInvokedCallback true→false — back button/gesture jadi non-fungsional ("kosmetik") krn konflik predictive-back Android (Material-only) dgn CupertinoPageRoute yang dipakai app ini
### Milestone
- CI confirmed GREEN pertama kali (3 APK + source archives berhasil publish ke GitHub Release)
### Queued
- Penamaan APK unik per rilis (Jotter-<arsitektur>-<version>-<run_number>.apk) — next batch

## [v1_Batch18] - 2026-08-24
### Fixed
- lock_screen.dart + settings_screen.dart: feedback jelas saat biometric gagal/tidak tersedia (dialog di Settings, error message + tombol "Gunakan Biometrik" di Lock Screen) (verdict P0.3 + AUDIT Medium #9 resolved)

## [v1_Batch17] - 2026-08-24
### Fixed
- calendar_screen.dart: tab Kalender kini reaktif — otomatis refresh reminder saat ada perubahan note dari tab lain (listener ke NotesProvider) (AUDIT High #6 / verdict P0.2 resolved)

## [v1_Batch16] - 2026-08-24
### Added
- AUDIT_ISSUES.md: tanam verdict eksternal UX/UI Polish (P0/P1/P2), cross-ref ke item audit existing, sisanya jadi Pending Queue baru. Dokumentasi murni.

## [v1_Batch15] - 2026-08-24
### Fixed
- note_card.dart + calendar_screen.dart: judul note terkunci kini ikut disamarkan jadi "Catatan Terkunci" (sebelumnya cuma isi yg disamarkan, judul polos) (AUDIT High #5 resolved)

## [v1_Batch14] - 2026-08-24
### Fixed
- notification_service.dart: notification ID pakai hash FNV-1a 32-bit manual (bukan String.hashCode bawaan Dart) — jamin selalu muat int32 Android (AUDIT Critical #4 resolved, seluruh 4 Critical kini RESOLVED)

## [v1_Batch13] - 2026-08-24
### Fixed
- notes_provider.dart + app.dart: reminder di-reschedule ulang tiap app dibuka (mitigasi hilangnya alarm setelah reboot HP; AUDIT Critical #3 resolved via reschedule-on-open)

## [v1_Batch12] - 2026-08-24
### Fixed
- notes_provider.dart + note_repository.dart: reminder alarm dibatalkan saat note di-trash / permanent-delete / trash dikosongkan (AUDIT Critical #2 resolved)

## [v1_Batch11] - 2026-08-24
### Fixed
- notification_service.dart: scheduleReminder() no longer leaks locked note title/content into notification tray (AUDIT Critical #1 resolved)

## [v1_Batch10] - 2026-08-24
### Added
- AUDIT_ISSUES.md: audit inspeksi mendalam, 12 cacat tercatat (4 Critical, 3 High, 3 Medium, 2 Low). Belum ada fix kode - dokumentasi murni.

## [v1_Batch9] - 2026-08-24
### Fixed
- MainActivity.kt: FlutterActivity -> FlutterFragmentActivity (local_auth's BiometricPrompt requires a FragmentActivity host; biometric toggle was a no-op without this)

## [v1_Batch8] - 2026-08-23
### Fixed
- release.yml: --split-per-abi flag - stops shipping a fat APK with all CPU architectures bundled, ~3x size reduction per download

## [v1_Batch7] - 2026-08-23
### Fixed
- android/app/build.gradle.kts: enable core library desugaring + add desugar_jdk_libs dependency (required by flutter_local_notifications)

## [v1_Batch6] - 2026-08-23
### Fixed
- pubspec.yaml: flutter_timezone 1.0.8 -> 5.1.0 (1.0.8 used removed Flutter v1 plugin embedding API)
- notification_service.dart: adapt to flutter_timezone 5.x API change (getLocalTimezone() now returns TimezoneInfo, not String)
- Confirmed via pub.dev docs (web search), not guesswork

## [v1_Batch5] - 2026-08-23
### Fixed
- lib/app.dart: import material.dart for Material/MaterialType (Cupertino builder wrapper)
- lib/services/notification_service.dart: restore required uiLocalNotificationDateInterpretation param for zonedSchedule
- Toolchain (Gradle/AGP/Kotlin) now passes; these are the first real Dart compiler errors reached

## [v1_Batch4] - 2026-08-23
### Fixed
- CI build failure: bump Kotlin 1.9.24 -> 2.2.20 (Flutter stable 3.47.1 requires Kotlin >= 2.2.20)

## [v1_Batch3] - 2026-08-23
### Fixed
- CI build failure: bump AGP 8.3.2 -> 8.11.1 (Flutter stable 3.47.1 requires AGP >= 8.11.1)

## [v1_Batch2] - 2026-08-23
### Fixed
- CI build failure: bump Gradle wrapper 8.6 -> 8.14 (Flutter stable 3.47.1 requires Gradle >= 8.14.0)

## [v1_Batch1] - 2026-08-23
### Added
- Initial release: catatan teks & checklist, 9 warna, kalender+pengingat, arsip, sampah, kunci PIN/biometrik, grid/list toggle, pencarian & 4 mode urutan
- Cupertino UI penuh (large title, frosted tab bar, swipe actions, blur bawaan)
- Crash logger native (Application uncaught handler) + Dart (FlutterError/runZonedGuarded) -> MediaStore, retensi FIFO 50 log
- CI: GitHub Actions build+sign+release otomatis dgn stale-run guard (anti-desync)
- Signing keystore (PKCS12) di-generate, credentials dikirim via GitHub Secrets
