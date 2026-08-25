# Jotter (Native Kotlin + Jetpack Compose)

Aplikasi catatan 100% offline, gaya iOS. Rewrite total dari versi Flutter (lihat PROJECT_STATE.md untuk alasan). Teks & checklist, 9 warna kategori, kalender + pengingat, arsip, sampah, kunci PIN/biometrik, grid/list, pencarian & pengurutan penuh.

## Build otomatis (GitHub Actions)
Push ke `main` -> workflow build & sign APK (native Gradle, tanpa Flutter SDK) -> otomatis terbit sebagai GitHub Release.

## Build manual (butuh Android SDK + Gradle terinstall - proyek ini TIDAK menyertakan gradlew, lihat PROJECT_STATE.md)
```
gradle :app:assembleRelease
```
APK per-arsitektur: `app/build/outputs/apk/release/`

## Struktur & keputusan teknis
Lihat `PROJECT_STATE.md` (arsitektur, alasan pivot dari Flutter, protected assets, pending queue) dan `CHANGELOG.md`.

## Keamanan
`release.keystore` & `key.properties` di-gitignore. Kredensial signing sama dengan versi Flutter sebelumnya (di-reuse, sudah ada di GitHub Secrets - tidak perlu di-set ulang).
