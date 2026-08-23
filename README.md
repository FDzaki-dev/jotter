# Jotter

Aplikasi catatan 100% offline dengan gaya iOS (Cupertino). Teks & checklist, 9 warna kategori, kalender + pengingat, arsip, sampah, kunci PIN/biometrik, grid/list, pencarian & pengurutan penuh.

## Build otomatis (GitHub Actions)
Push ke `main` -> workflow build & sign APK -> otomatis terbit sebagai GitHub Release (lihat sidebar repo).

## Build manual (butuh Flutter SDK)
```
flutter pub get
flutter build apk --release
```
APK: `build/app/outputs/flutter-apk/app-release.apk`

## Struktur & keputusan teknis
Lihat `PROJECT_STATE.md` (arsitektur, protected assets, pending queue) dan `CHANGELOG.md` (riwayat rilis).

## Keamanan
`android/release.keystore` & `android/key.properties` sudah di-gitignore (tidak pernah ter-commit). Kredensial signing untuk CI dikirim terpisah lewat `gh secret set` (lihat file *_secrets.txt saat setup awal).
