# CHANGELOG

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
