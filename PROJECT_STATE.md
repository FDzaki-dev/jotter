# PROJECT_STATE — Jotter

## [v1_Batch7] — 2026-08-23 (TERBARU)
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
