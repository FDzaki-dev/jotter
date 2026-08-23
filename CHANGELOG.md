# CHANGELOG

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
