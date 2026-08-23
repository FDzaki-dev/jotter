# PROJECT_STATE — Jotter

## [v1_Batch1] — 2026-08-23 (TERBARU)
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
