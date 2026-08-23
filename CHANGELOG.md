# CHANGELOG

## [v1_Batch1] - 2026-08-23
### Added
- Initial release: catatan teks & checklist, 9 warna, kalender+pengingat, arsip, sampah, kunci PIN/biometrik, grid/list toggle, pencarian & 4 mode urutan
- Cupertino UI penuh (large title, frosted tab bar, swipe actions, blur bawaan)
- Crash logger native (Application uncaught handler) + Dart (FlutterError/runZonedGuarded) -> MediaStore, retensi FIFO 50 log
- CI: GitHub Actions build+sign+release otomatis dgn stale-run guard (anti-desync)
- Signing keystore (PKCS12) di-generate, credentials dikirim via GitHub Secrets
