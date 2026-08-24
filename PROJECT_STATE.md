# PROJECT_STATE — Jotter

## [v1_Batch12] — 2026-08-24 (TERBARU)
Fix: AUDIT Critical #2 — reminder tidak dibatalkan saat note dihapus.
- `lib/providers/notes_provider.dart`: `trashNote()` & `permanentDelete()` kini panggil `NotificationService().cancelReminder(id)` sebelum operasi DB.
- `lib/repositories/note_repository.dart`: `emptyTrash()` kini query dulu semua id note terhapus, cancel reminder masing2, baru delete massal.
- 2 file diubah, 1 task (micro-batch). AUDIT_ISSUES.md #2 ditandai RESOLVED.
- Catatan: `emptyTrash()` belum di-wire ke UI (tombol "Kosongkan Sampah" belum ada di `filtered_notes_screen.dart`) — di luar scope task ini, tetap dicatat di Pending Queue kalau user mau ditambahkan.
- Belum diverifikasi run CI.
- Sisa Pending Queue: Critical #3 (no reschedule setelah reboot), #4 (hashCode ID out-of-range) + 3 High + 3 Medium + 2 Low — lihat AUDIT_ISSUES.md.

## [v1_Batch11] — 2026-08-24
Fix: AUDIT Critical #1 — reminder notifikasi membocorkan isi note terkunci.
- File: `lib/services/notification_service.dart` (`scheduleReminder()`)
- Root cause: title/body notifikasi diisi `note.title`/`note.content` mentah tanpa cek `note.isLocked`.
- Fix: jika `note.isLocked == true` -> title dipaksa "Jotter", body dipaksa generik "Anda memiliki catatan terkunci yang perlu diperiksa" (note checklist tetap pakai body generik lama, tidak berubah).
- 1 file kode diubah, 1 task (micro-batch). AUDIT_ISSUES.md #1 ditandai RESOLVED.
- Belum diverifikasi run CI.
- Sisa Pending Queue: Critical #2 (reminder tak dibatalkan saat note dihapus), #3 (no reschedule setelah reboot), #4 (hashCode ID out-of-range) + 3 High + 3 Medium + 2 Low — lihat AUDIT_ISSUES.md.

## [v1_Batch10] — 2026-08-24
Dok: Audit inspeksi mendalam seluruh kode (lib/**, protected assets, CI workflow) → `AUDIT_ISSUES.md` (baru).
- 12 cacat tercatat: 4 Critical (reminder bocor isi note terkunci lewat notifikasi; reminder tidak dibatalkan saat note dihapus; reminder hilang stlh reboot HP - no reschedule; notif ID dari `.hashCode` berisiko out-of-range), 3 High, 3 Medium, 2 Low/dok.
- BELUM ADA FIX kode diterapkan batch ini — murni dokumentasi (sesuai instruksi user: "dokumentasi dulu"). Semua 12 item jadi Pending Queue, diprioritaskan mulai dari Critical di batch berikutnya (micro-batching: 1 task/batch).
- 1 file baru (AUDIT_ISSUES.md), 0 file kode diubah

## [v1_Batch9] — 2026-08-24
Fix: Biometric toggle nyala tapi authenticate() gak pernah benar2 muncul/berhasil ("kosmetik doang").
- Root cause: `MainActivity.kt` extends `FlutterActivity` biasa. Plugin `local_auth` di Android pakai `BiometricPrompt` yang WAJIB `FragmentActivity` sebagai host — dengan `FlutterActivity` biasa, panggilan biometric gagal secara diam2 (canCheckBiometrics/authenticate return false, tidak throw), makanya toggle-nya kelihatan "jalan" tapi prompt gak pernah muncul.
- Fix: `MainActivity.kt` -> extends `FlutterFragmentActivity` (bukan `FlutterActivity`). MethodChannel crash-logger yang sudah ada tidak terpengaruh (FlutterFragmentActivity punya override point configureFlutterEngine yang sama).
- 1 file diubah, 1 task, root cause pasti (requirement resmi terdokumentasi local_auth), bukan tebakan
- PIN lock TIDAK terpengaruh bug ini (PIN pakai flutter_secure_storage murni, tidak butuh FragmentActivity) — kalau PIN sudah jalan normal, ini murni soal biometric

## [v1_Batch8] — 2026-08-23
Fix: APK bengkak >50MB — bukan bug build, tapi `flutter build apk --release` default menghasilkan "fat APK" berisi native library utk SEMUA arsitektur CPU sekaligus (armeabi-v7a + arm64-v8a + x86_64 digabung jadi satu file).
- Diubah: `.github/workflows/release.yml` -> tambah flag `--split-per-abi`, sekarang menghasilkan 3 APK terpisah per arsitektur, masing2 ~1/3 ukuran fat APK
- Rilis GitHub sekarang akan berisi 3 file: app-arm64-v8a-release.apk (dipakai 95%+ HP modern), app-armeabi-v7a-release.apk (HP 32-bit lama), app-x86_64-release.apk (emulator)
- Rekomendasi: install app-arm64-v8a-release.apk kecuali yakin HP masih 32-bit
- Minify+shrinkResources sudah aktif dari awal (tidak berubah) — kontributor size utama memang fat-APK, bukan itu
- 1 file diubah, 1 task
- Belum diverifikasi run CI (perbaikan kompilasi & desugaring dari Batch7 juga masih menunggu konfirmasi hijau)

## [v1_Batch7] — 2026-08-23
Progress: flutter_timezone compile LOLOS (fix Batch6 berhasil). Build maju lebih jauh lagi sampai task `:app:checkReleaseAarMetadata`.
- Root cause: `flutter_local_notifications` mewajibkan core library desugaring diaktifkan (requirement standar & terdokumentasi resmi untuk plugin ini) — belum diaktifkan di project.
- Fix: `android/app/build.gradle.kts` -> `compileOptions.isCoreLibraryDesugaringEnabled = true` + dependency `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")`
- 1 file diubah, 1 task
- Belum diverifikasi compile penuh — cek run CI berikutnya

## [v1_Batch6] — 2026-08-23
Progress: Kedua fix Dart di Batch5 LOLOS kompilasi (Material import + notification param, tidak muncul lagi di log). Error baru: dependency pihak ketiga usang.
- Root cause: `flutter_timezone: ^1.0.8` resolve ke versi lama yang pakai Flutter plugin embedding v1 (`Registrar`/`messenger`) yang sudah DIHAPUS dari Flutter modern -> `flutter_timezone:compileReleaseKotlin` gagal compile.
- Fix: `pubspec.yaml` -> `flutter_timezone: ^5.1.0` (versi resolved terkonfirmasi tersedia dari log pub get sebelumnya)
- Konsekuensi breaking-change: dicek via web search ke dokumentasi resmi -> `FlutterTimezone.getLocalTimezone()` di versi 5.x mengembalikan objek `TimezoneInfo` (BUKAN `String` lagi seperti 1.0.8). `lib/services/notification_service.dart` disesuaikan pakai `tzInfo.identifier`.
- 2 file diubah (pubspec.yaml + notification_service.dart), masih 1 task: "perbaiki error compile flutter_timezone dari log ini"
- Toolchain (Gradle/AGP/Kotlin) & 2 fix Batch5 TIDAK disentuh lagi — sudah lolos
- Belum diverifikasi compile penuh — cek run CI berikutnya

## [v1_Batch5] — 2026-08-23
Progress: Gradle/AGP/Kotlin phase kini LOLOS (cuma warning, tidak lagi blocking). Build maju sampai tahap kompilasi Dart (kernel_snapshot_program) dan gagal di 2 error compiler nyata — bukan lagi soal versi toolchain. Sumber: `0_build.txt` (CI run setelah Batch4).
- Fix 1: `lib/app.dart` -> `Material`/`MaterialType` tidak terdefinisi karena hanya import cupertino.dart (tidak otomatis meng-export widget Material). Ditambahkan `import 'package:flutter/material.dart' show Material, MaterialType;`
- Fix 2: `lib/services/notification_service.dart` -> `zonedSchedule` di flutter_local_notifications 17.2.4 (versi resolved CI) TERNYATA masih mewajibkan parameter `uiLocalNotificationDateInterpretation` (dugaan sebelumnya di Batch1 bahwa param ini sudah dihapus di v17 TERBUKTI SALAH). Parameter dikembalikan.
- 2 file diubah (dalam batas 3 file, 1 task: "perbaiki error compiler dari log ini")
- Toolchain versi (Gradle 8.14 / AGP 8.11.1 / Kotlin 2.2.20) TIDAK diubah lagi — sudah lolos tahap itu
- Belum diverifikasi compile penuh — cek run CI berikutnya. Tidak ada error compiler lain yang tercatat di log ini di luar 2 di atas.

## [v1_Batch4] — 2026-08-23
Fix: CI gagal lagi — Gradle 8.14 & AGP 8.11.1 sekarang hanya warning (bukan blocker), TAPI Kotlin 2.0.0 < minimum Flutter 2.2.20. Sumber: log `0_build.txt` (CI run setelah Batch3).
- Diubah: `android/settings.gradle.kts` -> Kotlin 1.9.24 -> 2.2.20 (1 file, sesuai batch limit)
- Warning aktif (belum blocking, diabaikan dulu): Gradle disarankan naik ke 9.1.0+, AGP disarankan naik ke 9.0.1+ — akan ditangani jika Flutter benar2 men-drop dukungan versi saat ini
- Belum diverifikasi compile — cek run CI berikutnya

## [v1_Batch3] — 2026-08-23
Fix: CI gagal lagi di "Build release APK" — Gradle 8.14 sukses diterapkan, TAPI AGP 8.3.2 < minimum Flutter 8.11.1. Sumber: log `0_build.txt` (CI run setelah Batch2).
- Diubah: `android/settings.gradle.kts` -> AGP 8.3.2 -> 8.11.1 (1 file, sesuai batch limit)
- Gradle 8.14 TIDAK diubah lagi (sudah cukup, warning "upgrade to 9.1.0 soon" hanya deprecation notice bukan error, akan ditangani terpisah jika benar2 di-drop)
- Belum diverifikasi compile — cek run CI berikutnya
- Pending: jika AGP 8.11.1 masih trigger "AGP 9+ new DSL" warning/error, kemungkinan perlu migrasi ke DSL baru — batch berikutnya

## [v1_Batch2] — 2026-08-23
Fix: CI gagal di step "Build release APK" — Flutter stable resolve ke 3.47.1 yang butuh Gradle >= 8.14.0, wrapper masih 8.6. Sumber: analisa `Jotter_secrets.txt`-independen log GitHub Actions (`8_Build release APK.txt`) yang diupload user.
- Diubah: `android/gradle/wrapper/gradle-wrapper.properties` -> distributionUrl gradle-8.14-all.zip (1 file, sesuai batch limit)
- Belum diverifikasi compile (masih sandbox tanpa Flutter SDK) — cek run CI berikutnya
- Pending jika masih gagal: AGP 8.3.2 mungkin perlu naik juga (AGP punya batas atas versi Gradle yang didukung) — akan ditangani batch berikutnya jika muncul error baru terkait AGP/Gradle compatibility

## [v1_Batch1] — 2026-08-23
Status: Initial build lengkap. BELUM di-compile lokal (sandbox tanpa Flutter SDK/network) — validasi pertama terjadi di GitHub Actions CI saat push ke main.

### Arsitektur
- State management: Provider (ChangeNotifier)
- DB: sqflite (SQLite lokal), tabel `notes` tunggal, checklist item disimpan sbg JSON di kolom `checklistItems`
- Auth: PIN 4-digit (SHA-256 salted, flutter_secure_storage/Android Keystore) + biometric opsional (local_auth)
- Reminder: flutter_local_notifications + timezone (inexact scheduling — tanpa permission SCHEDULE_EXACT_ALARM)
- Crash logger: Dart (FlutterError/runZonedGuarded) + native (Application.setDefaultUncaughtExceptionHandler) -> MethodChannel -> MediaStore (Documents/Jotter/logs), retensi FIFO 50 log, tanpa legacy permission
- UI: CupertinoApp, CupertinoSliverNavigationBar (large title collapse), CupertinoTabScaffold (frosted/blur tab bar bawaan Flutter), flutter_slidable (swipe), table_calendar
- minSdk 31 / targetSdk 35 / compileSdk 35 — tanpa backward-compat < Android 12 (sesuai instruksi)
- Package: com.jotter.notes | AGP 8.3.2 | Kotlin 1.9.24 | Gradle wrapper 8.6

### Protected Assets — status ✅ lengkap
AndroidManifest.xml · android/build.gradle.kts · android/app/build.gradle.kts · settings.gradle.kts · MainActivity.kt · MainApplication.kt · DB schema (database_helper.dart) · android/release.keystore · .gitignore · .gitattributes · .github/workflows/release.yml

### Known limitations / Pending Queue (batch berikutnya)
- **→ Lihat `AUDIT_ISSUES.md` untuk 12 cacat hasil audit Batch10 (4 Critical/3 High/3 Medium/2 Low), belum ada yang di-fix.**
- Font: pakai system default (Roboto), BUKAN SF Pro — lisensi Apple tidak mengizinkan redistribusi di Android. Alternatif: font "Inter" (open-license, mirip SF Pro).
- Ikon launcher: placeholder vector sederhana (bentuk notepad), belum artwork final.
- Konten catatan terkunci: saat ini digerbang PIN/biometric di level UI, BELUM dienkripsi AES at-rest.
- Belum ada l10n (UI Bahasa Indonesia hardcoded).
- Nama constant CupertinoIcons.* dipilih dari memori training — jalankan `flutter analyze` setelah pull pertama; kemungkinan 1-2 nama ikon perlu disesuaikan (cosmetic only, tidak pengaruh ke data/arsitektur).
- Versi AGP/Kotlin/Gradle dipilih kombinasi stabil yang diketahui kompatibel per Jan 2026 — cek log CI pertama, sesuaikan jika Flutter stable terbaru butuh versi lebih baru.

### Keystore & Secrets
- android/release.keystore (PKCS12) di-generate saat build ini via keytool asli, valid 10.000 hari (s/d 2054)
- Alias: jotter_release
- Base64 + password + alias dikirim di file terpisah `Jotter_secrets.txt` (BUKAN di dalam ZIP ini)
- android/key.properties sudah berisi kredensial asli untuk build lokal — di-gitignore, tidak akan ter-commit
