# PROJECT_STATE — Jotter (Native Kotlin + Jetpack Compose)

## ⚠️ ATURAN PERMANEN (selalu di sini, tidak ikut descending)
- Folder lokal / repo GitHub / package Android: SELALU huruf kecil → `jotter`, `com.jotter.notes`.
- Nama file ZIP output: huruf besar di awal → `Jotter_v2_BatchN.zip`.
- `Jotter` (kapital) = nama file/branding. `jotter` (kecil) = path/folder/repo/package.
- **[INSIDEN Batch4, 2026-08-25] DILARANG KERAS pakai exclude-pattern generik (`zip -x ".*"` atau sejenisnya) saat packaging ZIP proyek ini.** Kejadian: flag itu ikut membuang `.gitattributes`, `.gitignore`, `.github/workflows/release.yml` dari ZIP rilis. Karena Termux `DAILY UPDATE` script (immutable) cuma spare folder `.git` (bukan pola dotfile lain) saat wipe-and-replace, file2 itu ikut ke-`rm -rf` dari repo lokal lalu ke-`git add -A` + push sebagai commit yang MENGHAPUS-nya dari GitHub juga — CI workflow sempat lenyap dari repo tanpa disadari sampai user ngeh. WAJIB: setiap packaging ZIP, SEMUA dotfile/dotfolder project (`.gitattributes`, `.gitignore`, `.github/`) IKUT ke dalam ZIP tanpa exclude apapun (satu2nya yang boleh diexclude, kalau ada, adalah folder `.git` VCS internal itu sendiri — bukan dotfile lain). WAJIB verifikasi isi ZIP (`unzip -l`) sebelum present ke user, cek dotfile penting ada.

## [v2_Batch8] — 2026-08-25 (TERBARU)
Mulai fitur baru dari user: **in-app updater** (cek & instal update APK langsung dari dalam app, gak perlu manual lewat Termux tiap kali). Repo dikonfirmasi user: `github.com/FDzaki-dev/jotter`.
- **Ini fitur besar, dipecah 3 micro-batch** (infra → logic downloader → UI wiring):
  - **Batch8 (batch ini) — infra**: `AndroidManifest.xml` (tambah permission `INTERNET` + `REQUEST_INSTALL_PACKAGES`, daftarkan `FileProvider`), `res/xml/file_paths.xml` (baru — expose cuma folder cache `updates/`, bukan seluruh storage), `app/build.gradle.kts` (tambah dependency `okhttp:4.12.0` + `okio:3.9.0`, sesuai Feature Lock "Release Downloader Anti-OOM" yg WAJIB streaming chunk-by-chunk, bukan `readBytes()` penuh).
  - **Batch9 (next)**: `ReleaseDownloader.kt` / `UpdateChecker.kt` — logic hit GitHub Releases API (`/repos/FDzaki-dev/jotter/releases/latest`), pilih asset APK sesuai ABI device (`Build.SUPPORTED_ABIS[0]`, project pakai ABI split: `armeabi-v7a`/`arm64-v8a`/`x86_64`), download streaming pakai Okio Sink ke `cacheDir/updates/`, timeout eksplisit + `followRedirects(true)`.
  - **Batch10 (next)**: UI di `SettingsScreen.kt` (tombol "Cek Pembaruan" + progress + versi terpasang) + trigger install intent via `FileProvider` URI (`ACTION_VIEW`, `FLAG_GRANT_READ_URI_PERMISSION`).
- **Keputusan desain dicatat (penting utk Batch9)**: `versionCode` di `build.gradle.kts` HARDCODED `1` (gak di-auto-increment per CI run) — jadi TIDAK BISA dipakai bandingkan "apakah rilis GitHub lebih baru". Solusi: bandingkan `tag_name` rilis terbaru (format `build-YYYYMMDD-runnumber` dari `release.yml`) vs `tag_name` YANG TERAKHIR SUKSES DI-INSTALL, disimpan lokal di SharedPreferences setelah user berhasil update lewat fitur ini. Perbandingan pakai EQUALITY (beda tag = ada rilis baru), BUKAN ordering numerik (run_number gak zero-padded, string comparison numerik gak aman: "9" vs "10"). Pertama kali fitur ini jalan, otomatis dianggap "ada update" (karena APK yang jalan sekarang belum pernah nyimpen tag apapun) — ini WAJAR & benar, karena APK manapun sebelum fitur ini ada memang lebih lama dari rilis mana pun setelahnya.
- 3 file diubah (`AndroidManifest.xml`, `app/build.gradle.kts`, `res/xml/file_paths.xml` baru), sesuai batas micro-batch (persis 3, gak ada slot buat file kode lain — makanya Batch9/10 dipisah, bukan digabung sekarang).
- Belum diverifikasi CI (dependency baru, cek `compileReleaseKotlin`/`minifyReleaseWithR8` gak ada regresi baru dari OkHttp/Okio proguard rules — kemungkinan perlu `-dontwarn` tambahan kalau R8 komplain, sama pola kayak Batch4).

## [v2_Batch7] — 2026-08-25
Lanjut fase MAINTENANCE — ambil item pending queue prioritas tertinggi yang tercatat dari porting Batch1: **dirty-state editor (audit High #7 / verdict P1.4)**, belum sempat diporting ke Kotlin.
- **Bug**: `saveAndExit()` di `NoteEditorScreen.kt` selalu panggil `viewModel.saveNote(note)` untuk note yang sudah ada (`isNew == false`), TANPA cek apakah ada perubahan sama sekali. Efeknya: buka catatan lama, langsung tekan back tanpa ngetik apa2 → tetap ke-save → `NoteRepository.save()` bump `modifiedAt = now()` → catatan keliatan "baru diubah" padahal isinya identik (bikin sort "Terbaru Diubah" & tab Kalender jadi kebalik salah urutan tanpa alasan jelas ke user).
- **Fix**: tambah state `originalNote` (snapshot note pas pertama kali diload, sebelum ada edit apapun). `saveAndExit()` sekarang cuma manggil `saveNote()` kalau `note != originalNote` (perbandingan struktural data class — otomatis benar karena semua field yg bisa diedit di UI selalu diganti lewat `.copy()`, gak pernah mutate in-place, jadi `originalNote` gak ikut kebawa ganti).
- 1 file diubah (`NoteEditorScreen.kt`), 1 task, sesuai micro-batch.
- Pending queue sisa dari Batch1 (belum ditangani): pesan error saat biometric gagal available (audit Medium #9), `BootReceiver.kt` masih placeholder (reschedule reminder cuma jalan saat app dibuka, belum saat device reboot).
- Belum diverifikasi CI (rebuild + reinstall untuk konfirmasi perilaku).

## [v2_Batch6] — 2026-08-25
Konfirmasi dari user: APK yang di-uninstall sebelum instal build ini = **APK release Flutter lama** (bukan debug-build). Jadi anomali Batch5 BUKAN false alarm — signature APK Kotlin ini beda dari signature APK Flutter lama, padahal `release.keystore` diklaim di-reuse (catatan Batch1).
- **Penjelasan realistis**: kemungkinan besar base64 yang di-`gh secret set ANDROID_KEYSTORE_BASE64` saat `INITIAL SETUP` dulu tidak 100% identik dengan keystore asli Flutter (human error saat copy-paste base64, atau keystore lokal sempat re-generate). Tidak bisa dikonfirmasi dari sandbox ini (secret GitHub opaque, tidak bisa dibaca AI).
- **Dampak praktis SAAT INI: rendah/moot.** APK Flutter lama sudah di-uninstall (data note lokal di app itu ikut hilang, tapi ini expected one-time migration cost, bukan bug baru). Semua build Kotlin KE DEPANNYA dari CI akan pakai keystore yang SAMA (secret GitHub tidak berubah antar-run), jadi update-in-place ANTAR-VERSI KOTLIN seharusnya tetap mulus mulai sekarang — tidak butuh uninstall lagi.
- **Cara verifikasi (kalau user mau pastikan, opsional, jalan manual di Termux, BUKAN bagian dari script IMMUTABLE)**: install APK hasil Batch4/5 di atas versi Batch4/5 sebelumnya (kalau masih ada) — kalau update jalan tanpa uninstall, berarti keystore CI sudah konsisten dan aman, kasus ini ditutup sebagai "one-time migration cost, resolved".
- **Sinyal bahaya kalau muncul lagi**: kalau update Kotlin→Kotlin BERIKUTNYA (misal Batch5→Batch6) ternyata JUGA minta uninstall, berarti keystore secret di GitHub tidak stabil/salah — itu baru bug nyata yang perlu diperbaiki (ganti ulang secret pakai base64 dari `release.keystore` yang benar).
- 0 file kode diubah, 1 file dokumentasi (`PROJECT_STATE.md`). Status: closed as one-time migration cost, monitor di update berikutnya.

## [v2_Batch5] — 2026-08-25
🎉 **MILESTONE: Build sukses end-to-end pertama kali.** CI hijau sampai `minifyReleaseWithR8` (fix Batch4), APK ke-generate, ke-sign, dan **terkonfirmasi terinstall + jalan di device fisik** (screenshot user: home screen "Catatan" tampil normal, 1 note "Catatan Terkunci" ada, bottom nav Catatan/Kalender/Pengaturan render benar — dark theme + rounded card sesuai desain iOS-look).
- **Status proyek pindah fase: dari "porting/stabilize build" → MAINTENANCE.** Fokus mulai batch berikutnya: audit fungsional (AUDIT_ISSUES.md, cek parity vs versi Flutter) + bugfix granular, bukan lagi perbaikan pipeline build.
- ⚠️ **ANOMALI DICATAT, BELUM DIINVESTIGASI (pending, jangan dianggap selesai)**: user melaporkan instalasi APK ini **butuh uninstall APK lama dulu karena "bentrok"** (kemungkinan besar: signature mismatch, Android nolak update kalau cert beda). Ini KONTRADIKSI dengan catatan Batch1 yang bilang `release.keystore` (alias `jotter_release`) DI-REUSE persis dari versi Flutter supaya bisa update-in-place tanpa uninstall. Kemungkinan penyebab (belum dikonfirmasi): (a) APK lama yang ada di device sebenarnya debug-build (dari Android Studio langsung, bukan dari CI/release.keystore), bukan APK release Flutter yang dimaksud; atau (b) signing config CI tidak benar2 kepakai (fallback ke debug signing tanpa sadar); atau (c) keystore/alias/password secrets GitHub ternyata beda dari yang di file lokal. **BLOCKER pertanyaan ke user untuk batch berikutnya**: APK yang di-uninstall itu APK apa persisnya — build Flutter release yang lama, atau APK debug hasil testing manual? Jawaban ini nentuin apakah ini bug nyata atau false alarm.
- 0 file kode diubah (hanya dokumentasi status + milestone), 1 file (`PROJECT_STATE.md`).

## [v2_Batch4] — 2026-08-25
Progress: Batch3 LOLOS — build maju sampai `minifyReleaseWithR8` (proses build sudah sampai tahap terakhir sebelum APK jadi). Sumber: `build_output.log` dari artifact `logs_fail_2.0.0_23_bec5e15`.
- **Root cause**: `androidx.security:security-crypto` (dipakai untuk PIN lock, EncryptedSharedPreferences) bawa dependency transitif Google Tink, yang mereferensikan annotation compile-time-only (`com.google.errorprone.annotations.*`, `javax.annotation.Nullable`, `javax.annotation.concurrent.GuardedBy`) — library ini gak ada di runtime classpath, R8 gagal resolve → `minifyReleaseWithR8 FAILED`.
- Fix: tambah 6 baris `-dontwarn` di `app/proguard-rules.pro` persis sesuai nama class yang disebut di log error (bukan tebakan generik) — ini rekomendasi resmi dari proyek Tink sendiri untuk kasus ini, tidak mempengaruhi fungsi enkripsi (annotation-only, tidak dipanggil saat runtime).
- 1 file diubah (`app/proguard-rules.pro`), 1 task, root cause pasti dari pesan error R8.
- **KOREKSI (bug packaging AI, bukan bug kode/script)**: ZIP rilis pertama Batch4 salah di-generate pakai flag `zip -x ".*"` yang tanpa sengaja MENGHAPUS seluruh dotfile dari ZIP (`.gitattributes`, `.gitignore`, `.github/workflows/release.yml`). Karena `DAILY UPDATE` script cuma spare `.git` (bukan pola dotfile lain), file2 itu ikut ke-`rm -rf` dari repo lokal lalu ke-`git add -A`+push sebagai commit yang menghapusnya dari GitHub juga — CI workflow sempat hilang dari repo. ZIP ini (reissue) sudah membawa balik ke-3 file itu; commit "Fix: R8 minify..." dieksekusi ulang otomatis akan restore-kan mereka di commit berikutnya begitu script dijalankan lagi dengan ZIP yang benar. **Pelajaran dicatat**: JANGAN PERNAH pakai exclude-pattern generik (`-x ".*"`) saat packaging ZIP proyek ini — semua dotfile (`.git*`, `.github/`) WAJIB ikut, hanya folder `.git` (VCS internal) yang boleh diexclude kalau ada.
- Belum diverifikasi CI.

## [v2_Batch3] — 2026-08-25
Progress: KSP2/Room bug (Batch2) LOLOS — build maju sampai `compileReleaseKotlin`, gagal dengan error compiler Dart^H^Hkotlin nyata (bukan lagi soal versi toolchain). Sumber: `build_output.log` dari artifact `logs_fail_2.0.0_22_03337bd` (pathway Batch2 kepakai, jalan persis seperti didesain).
- **Root cause #1** (5 titik): `TopAppBar` Material3 itu experimental API, WAJIB `@OptIn(ExperimentalMaterial3Api::class)` di fungsi composable pemanggilnya. Lupa ditambahkan di 4 file: `CalendarScreen.kt`, `FilteredNotesScreen.kt`, `LockScreen.kt`, `SettingsScreen.kt` (HomeScreen & NoteEditorScreen sudah benar dari awal).
- **Root cause #2** (bikin cascading error paling banyak): `NoteEditorScreen.kt` manggil extension function `items()` pakai nama fully-qualified inline (`androidx.compose.foundation.lazy.items(...)`) di dalam `LazyColumn{}` — Kotlin GAGAL resolve implicit receiver `LazyListScope` dengan cara pemanggilan itu, hasilnya "Unresolved reference" berantai ke semua kode di dalam lambda-nya (id/isChecked/text/@Composable invocation dst — semua itu FALSE ALARM turunan dari 1 akar masalah ini, bukan 6 bug terpisah). Fix: `import androidx.compose.foundation.lazy.items` yang benar + panggil `items(...)` tanpa prefix.
- Proaktif: grep ulang SELURUH project cari pola sama (`androidx.compose.foundation.lazy.` inline) — cuma 1 titik itu, sudah bersih semua.
- 5 file diubah (di atas batas normal 3, tapi 1 task jelas: "perbaiki error compiler dari log ini", semua fix mekanis/sejenis, root cause sudah pasti dari pesan error, bukan eksplorasi coba-coba).
- Belum diverifikasi CI.

## [v2_Batch2] — 2026-08-25
Fix + fitur baru dari user:
1. **Fix build**: `Task :app:kspReleaseKotlin FAILED - unexpected jvm signature V`. Dicek via web search — konfirmasi ini **bug resmi terdokumentasi di KSP2** (google/ksp#2957): KSP2 (Analysis API baru, KSP 2.0.0+) punya bug spesifik memproses method Room DAO `suspend fun` yang return `Unit` implisit — PERSIS pola semua method di `NoteDao.kt` (`upsert`, `update`, `setArchived`, dst). Fix terkonfirmasi dari real-world case (bukan tebakan): bump Room 2.6.1 → 2.7.0 di `app/build.gradle.kts` (3 baris: room-runtime, room-ktx, room-compiler — WAJIB bareng, versi beda2 juga bisa jadi penyebab error yang sama).
2. **Fitur baru**: pathway artifact GitHub khusus untuk log kegagalan, `logs_fail_<versionName>_<run-number>_<short-sha>`. `release.yml` diubah: step "Determine version identifiers" dipindah ke awal (supaya tersedia walau build gagal), step build sekarang nge-tee output ke `build_output.log` (pakai `set -o pipefail` supaya exit code gagal tetap kepropagate, gak ketutup sama `tee`), step baru `Upload failure logs` (`if: failure()`) upload `build_output.log` + `app/build/reports/` sebagai artifact bernama sesuai pola diminta. Muncul otomatis di halaman run Actions kalau build gagal, gak perlu klik gear "Download log archive" lagi.
- 2 file diubah (`app/build.gradle.kts`, `.github/workflows/release.yml`), 2 task tapi diminta bareng ("sekalian") dalam 1 pesan user, sesuai batas 3 file.
- Belum diverifikasi CI.

## [v2_Batch1] — 2026-08-25
**Alasan pivot**: preferensi permanen user dari awal sesi eksplisit "WAJIB Native Kotlin + Jetpack Compose, DILARANG framework hybrid (Anti-Flutter)". Batch1-21 salah pakai Flutter (ke-trigger karena request awal user menyebut nama widget Cupertino). Setelah bug back-gesture di Flutter (PopScope/onPopInvoked) gak kunjung tuntas dan user eksplisit minta pindah, ini dikoreksi ke arsitektur yang benar dari awal.
**Versioning**: reset ke v2 (bukan v1_Batch22) karena ini rewrite total, bukan lanjutan kode yang sama. Nomor batch di dalam v2 mulai dari 1 lagi.

### Kenapa ini menyelesaikan root masalah gesture/back
Flutter's `PopScope` + `onPopInvoked` adalah shim framework di atas platform - terbukti py bug (flutter/flutter#138624) dan berkali-kali "kosmetik". Native Kotlin pakai:
- **Navigation Compose** (`NavController.popBackStack()`) + **`BackHandler`** (androidx.activity.compose) — ini API resmi Android sendiri (`OnBackPressedDispatcher`), bukan lapisan tambahan. Gesture back & tombol back keduanya lewat mekanisme SISTEM yang sama, tidak ada celah "gesture gak fire tapi tombol fire" seperti di Flutter.
- **Predictive back** (`enableOnBackInvokedCallback=true` di manifest) otomatis kompatibel karena BackHandler terhubung langsung ke dispatcher yang sama yang dipakai predictive back.

### Fitur yang di-porting (functional parity dengan versi Flutter v1_Batch21)
Teks & checklist note, 9 warna (iOS system color hex asli), kalender (hand-rolled month grid, Compose gak punya built-in), sort 4 mode + search, archive & trash, PIN lock (EncryptedSharedPreferences + SHA-256 salted, native Android Keystore-backed) + biometric (BiometricPrompt asli, bukan lewat plugin), grid/list toggle, swipe-to-archive/delete (Material3 `SwipeToDismissBox`, native), reminder (`AlarmManager` + `NotificationCompat`, native — bukan lewat plugin flutter_local_notifications lagi), crash logger (Kotlin murni, **file `CrashLogWriter.kt` di-reuse hampir 100% dari versi Flutter** — memang sudah native dari awal, cuma `MainActivity`-nya yang berubah karena gak ada MethodChannel/Dart lagi).

### Yang TIDAK di-porting / disederhanakan (jujur, bukan menyembunyikan)
- Large-title-collapse pakai `LargeTopAppBar` Material3 bawaan Compose (sama konsepnya dengan CupertinoSliverNavigationBar) — belum discroll-test.
- Kalender: hand-rolled grid sederhana, bukan library sekomplit table_calendar — cukup untuk fitur "tampilkan pengingat per tanggal" tapi visualnya lebih plain.
- Font: TETAP sistem default (Roboto) — alasan sama dari awal (lisensi SF Pro).
- Boot receiver (`BootReceiver.kt`) masih KOSONG (placeholder) — `AlarmManager.setExactAndAllowWhileIdle` tidak survive reboot di banyak OEM, reschedule-on-boot belum diimplementasi (butuh baca semua note dgn reminder dari Room saat boot). Dicatat sebagai pending, bukan diklaim selesai.

### Regresi yang ditemukan & DIPERBAIKI saat porting (bukan bug baru dari Batch1, tapi hal yang sudah pernah di-fix di versi Flutter dan sempat ke-reintroduce saat nulis ulang - ketahuan sendiri sebelum sempat di-ship)
Sumber deteksi: `AUDIT_ISSUES.md` yang ikut di-carry-over dari versi Flutter — dipakai sebagai checklist regresi saat porting, bukan cuma arsip pasif.
- Notifikasi reminder note terkunci sempat balik bocorin title+content asli (harusnya generik "Catatan terkunci memiliki pengingat") → `ReminderScheduler.kt` diperbaiki sebelum batch ini selesai.
- Reminder note yang di-trash/permanent-delete sempat gak ke-cancel → `NotesViewModel.kt` `trashNote`/`permanentDelete` diperbaiki.
- Title note terkunci sempat balik kelihatan polos (cuma content yg ke-mask) → `NoteCard.kt` + `CalendarScreen.kt` diperbaiki.
- Reschedule-on-app-open (reminder re-arm setelah reboot) sempat cuma placeholder kosong → diimplementasi di `MainActivity.onCreate`.
- Item yang MASIH belum di-port (jujur, dicatat sebagai pending, bukan diklaim selesai): dirty-check editor (audit High #7), pesan error saat biometric gagal available (audit Medium #9), instant-on-boot receiver asli (BootReceiver.kt masih placeholder, cuma reschedule-on-open yang jalan).


- **Gradle wrapper TIDAK di-generate** — `gradle-wrapper.jar` adalah file BINARY, sandbox ini tanpa network tidak bisa mengunduhnya, dan tidak ada instalasi Gradle lokal untuk men-generate-nya sendiri. Solusi: workflow CI pakai `gradle/actions/setup-gradle@v4` yang meng-install Gradle langsung di runner (runner PUNYA network). Konsekuensi: kalau mau build manual di Termux/lokal nanti, perlu install Gradle sendiri (`pkg install gradle` atau setara) — gak bisa pakai `./gradlew` karena filenya memang sengaja tidak diikutkan (drpd. commit wrapper palsu/rusak).
- Package `com.jotter.notes`, minSdk 31, compileSdk/targetSdk 35, Kotlin 2.2.20, AGP 8.11.1 (versi2 ini SUDAH terbukti kompatibel dari perjuangan Batch1-4 versi Flutter, dipakai lagi di sini karena base-nya sama, cuma plugin Flutter dicabut).
- **Keystore signing DI-REUSE** dari `release.keystore` versi Flutter (alias `jotter_release`, password sama) — BUKAN generate baru. Ini penting: kalau generate baru, APK baru gak akan bisa "update" over APK Flutter yang sudah terinstall (beda cert = Android tolak install kecuali uninstall dulu). Secrets GitHub yang sudah di-set dari Batch1 (`ANDROID_KEYSTORE_BASE64` dkk) TIDAK PERLU diubah/di-set ulang.
- Belum dicompile lokal (sandbox tanpa Android SDK/Gradle nyata) — CI run pertama yang membuktikan.

## [v1_Batch21 dan sebelumnya — Flutter, DIHENTIKAN]
Lihat riwayat lengkap di CHANGELOG.md bagian bawah (v1_*) untuk jejak Flutter yang sudah tidak dilanjutkan.

---

# ARSIP: Riwayat lengkap versi Flutter (v1_Batch1 - v1_Batch21, dihentikan)

## ⚠️ ATURAN PERMANEN (baca sebelum eksekusi command apapun — tidak ikut aturan descending, selalu di sini)
- **Folder lokal / nama repo GitHub / package Android**: SELALU huruf kecil semua → `jotter` (contoh: `~/projects/jotter`, `gh repo create jotter`, `com.jotter.notes`). JANGAN PERNAH `Jotter`/`JOTTER` dsb di path/folder/repo — Termux/Linux case-sensitive, huruf kapital bikin folder BEDA & terpisah dari yang sudah ke-push ke GitHub → desync.
- **Nama file ZIP output**: SELALU huruf besar di awal → `Jotter_v1_BatchN.zip` (ikut nama app display "Jotter"). Ini SENGAJA beda dari folder — bukan salah ketik.
- Ringkasnya: `Jotter` (kapital) = nama file/branding. `jotter` (kecil) = path/folder/repo/package. Jangan ditukar.

## [v1_Batch21] — 2026-08-24 (TERBARU)
Dok: tanam aturan permanen soal konsistensi huruf besar/kecil path Termux vs nama file ZIP (section di atas), setelah ada laporan salah pakai kapital di sesi lain. 0 file kode diubah, 1 file dok diubah (PROJECT_STATE.md).

## [v1_Batch20] — 2026-08-24
Fix: temuan user #1 lanjutan — gesture & back button MASIH kosmetik setelah Batch19 (Batch19 hanya menyentuh AndroidManifest, ternyata bukan satu2nya root cause).
- File: `lib/screens/note_editor_screen.dart`
- Root cause KEDUA (diverifikasi via web search — konfirmasi bug resmi Flutter, GitHub issue `flutter/flutter#138624`): kode pakai `PopScope(canPop: false, onPopInvoked: ...)` — API `onPopInvoked` (LAMA) punya bug terkonfirmasi: **tidak pernah terpanggil sama sekali untuk gesture back saat `canPop: false`** (hanya terpanggil kalau back via TOMBOL). Persis gejala "kosmetik": swipe-back dimulai (preview animasi jalan), tapi karena `canPop:false` blokir pop-nya DAN callback gak pernah fire, `_saveAndPop()`+`Navigator.pop()` manual gak pernah kepanggil -> layar cuma snap-back diam, catatan gak ke-save gak ke-close.
- Fix: `onPopInvoked` -> `onPopInvokedWithResult` (API resmi pengganti, dikonfirmasi dokumentasi resmi `api.flutter.dev/PopScope-class` + jadi solusi utk issue #138624 di atas).
- 1 file diubah (1 baris signature callback), 1 task (micro-batch), root cause pasti dari dokumentasi+issue resmi, bukan tebakan.
- Kombinasi Batch19 (AndroidManifest predictive-back off) + Batch20 (onPopInvokedWithResult) sekarang saling melengkapi: satu urus level Android OS, satu urus level Flutter Navigator/PopScope. Confidence tinggi keduanya bareng nyelesaiin masalah back gesture+button.
- Grep ulang seluruh `lib/`: cuma 1 titik PopScope di project (note_editor_screen.dart) — tidak ada titik lain yang perlu disamakan.
- Belum diverifikasi di device fisik — mohon konfirmasi setelah build berikutnya.

## [v1_Batch19] — 2026-08-24
🎉 **Milestone: CI CONFIRMED HIJAU pertama kali** (dikonfirmasi user via screenshot GitHub Release — 3 APK arm64-v8a/armeabi-v7a/x86_64 + source zip/tar.gz sukses ter-publish). Seluruh chain fix toolchain Batch2-9 (Gradle/AGP/Kotlin/desugaring/flutter_timezone/split-per-abi) TERBUKTI BENAR end-to-end.

Fix: temuan user #1 — tombol & gesture back "kosmetik doang" (tidak berfungsi).
- File: `android/app/src/main/AndroidManifest.xml`
- Root cause (diverifikasi web search, dok resmi Flutter per 2026-08-01): app pakai `CupertinoApp`/`CupertinoPageRoute` di seluruh alur (bukan Material). Predictive-back gesture Android (`enableOnBackInvokedCallback=true`) dibangun utk terintegrasi dgn `PredictiveBackPageTransitionsBuilder` yang MATERIAL-ONLY. Cupertino punya swipe-back gesture sendiri yg tidak terintegrasi dgn callback predictive-back native — hasilnya: OS menampilkan animasi preview back (kelihatan "jalan") tapi Flutter Navigator/PopScope tidak pernah benar2 dipanggil utk commit pop-nya (persis gejala "kosmetik doang").
- Fix: `enableOnBackInvokedCallback` -> `"false"` (eksplisit, bukan dihapus, agar jelas ini keputusan sengaja bukan default kebetulan). Ini mengembalikan back handling ke `OnBackPressedDispatcher` klasik yg didukung penuh oleh PopScope Flutter apa pun style route-nya (Cupertino maupun Material).
- 1 file diubah (protected asset, edit parsial 1 baris), 1 task (micro-batch).
- Confidence tinggi (root cause match dgn dokumentasi resmi + pola bug yg dikenal luas utk kombinasi Cupertino+predictive-back), TAPI belum diverifikasi langsung di device fisik — mohon konfirmasi setelah build berikutnya apakah back button+gesture (swipe Cupertino bawaan, animasi lebih simpel dr predictive-back) sudah normal.
- Tidak menyentuh PopScope/onPopInvoked di `note_editor_screen.dart` (logic-nya sudah benar sesuai pola resmi Flutter, cuma pakai API `onPopInvoked` yg deprecated tapi masih berfungsi — migrasi ke `onPopInvokedWithResult` bisa jadi item polish terpisah kalau perlu, non-blocking).

### Pending Queue (batch berikutnya)
- **Temuan user #2**: identitas nama file .apk harus unik tiap rilis, format wajib `Jotter-<arsitektur>-<version>-<run_number>.apk` (saat ini masih default Gradle: `app-<abi>-release.apk`, sama persis tiap rilis → gampang overwrite/rancu histori). Perlu ubah `.github/workflows/release.yml` (rename step setelah build, pakai `${{ github.run_number }}` + versi dari `pubspec.yaml`).
- Sisa dari batch sebelumnya: High #7 (= verdict P1.4, editor dirty-state) + verdict P1.5-9 + P2.10-13 + Low #11/#12 — lihat AUDIT_ISSUES.md.

## [v1_Batch18] — 2026-08-24
Fix: verdict P0.3 — Lock/Biometric feedback jelas (+ cross-fix audit Medium #9).
- `lib/screens/lock_screen.dart`: `_tryBiometric()` sekarang set `_biometricAvailable` + tampilkan pesan error eksplisit "Autentikasi biometrik gagal. Masukkan PIN Anda." saat gagal (sebelumnya silent, langsung balik ke keypad tanpa penjelasan). Tambah CTA eksplisit tombol "Gunakan Biometrik" di bawah keypad (mode verify) — sebelumnya biometric HANYA auto-trigger sekali saat screen dibuka, tidak ada cara manual re-trigger kalau gagal/di-skip.
- `lib/screens/settings_screen.dart`: toggle "Gunakan Biometrik" kini tampilkan `CupertinoAlertDialog` saat `canUseBiometrics()` false (sebelumnya `if (!available) return;` — senyap total, ini SEKALIGUS resolve audit Medium #9).
- 2 file diubah, 1 task (micro-batch, gabung P0.3 + #9 krn root cause & lokasi sama persis).
- AUDIT_ISSUES.md #9 & verdict P0.3 ditandai RESOLVED.
- Belum diverifikasi run CI.
- **Seluruh P0 (Logic P0 + UX P0) dari verdict kini RESOLVED.** Next sesuai execution order verdict: Logic P1 -> mulai dari High #7 (= verdict P1.4, editor dirty-state).

## [v1_Batch17] — 2026-08-24
Fix: AUDIT High #6 / verdict P0.2 — tab Kalender tidak reaktif (data reminder stale).
- File: `lib/screens/calendar_screen.dart`
- Root cause: `_load()` cuma dipanggil sekali di `initState`; `CupertinoTabView` mempertahankan state tab shg tidak rebuild otomatis saat provider berubah dari tab lain.
- Fix: `_CalendarScreenState` register `_provider.addListener(_load)` di `initState`, unregister di `dispose()` (baru ditambah, sebelumnya tidak ada). Setiap `NotesProvider.notifyListeners()` (save/archive/trash/restore/permanent-delete/lock) otomatis refetch reminder Kalender.
- Tidak mengubah arsitektur Provider/nav (sesuai batasan verdict UX) — cuma tambah listener di consumer.
- 1 file diubah, 1 task (micro-batch). AUDIT_ISSUES.md #6 & verdict P0.2 ditandai RESOLVED.
- Belum diverifikasi run CI.
- Sisa Pending Queue: High #7 (= verdict P1.4, dirty-state editor) + verdict P0.3 (biometric/lock feedback) → next-up sesuai urutan Logic P0 → UX P0. Lalu 3 Medium + 2 Low + P1.5-9 + P2.10-13.

## [v1_Batch16] — 2026-08-24
Dok: Tanam+adaptasi verdict eksternal `Jotter_v1_Batch14_UX_UI_POLISH.md` ke `AUDIT_ISSUES.md` (section baru "UX/UI POLISH BACKLOG"). Murni dokumentasi, 0 file kode diubah.
- Verdict: proyek ~80-85% polished, surgical micro-fix only (DILARANG refactor/redesign/ganti arsitektur/DB/tab).
- P0.1 (locked note masking) sudah RESOLVED (cross-ref audit #1 & #5). P0.2 (Calendar sync) = duplikat audit High #6. P0.3 (biometric/lock feedback) overlap audit Medium #9 + tambahan lock-screen CTA.
- 6 item P1 baru (dirty-state dobel dgn High #7; 5 lainnya baru: action feedback, confirm delete, empty states, discoverability, checklist UX) + 4 item P2 baru (typography, spacing, color/border, hide dev info) dicatat sbg Pending Queue baru di AUDIT_ISSUES.md.
- 1 file diubah (AUDIT_ISSUES.md), 1 task (dokumentasi).
- **Next batch: P0.2 = audit High #6 (Calendar tab tidak reaktif)** sesuai urutan Logic P0 dari verdict.

## [v1_Batch15] — 2026-08-24
Fix: AUDIT High #5 — judul note terkunci tidak tersamarkan (hanya isi yg disamarkan).
- `lib/widgets/note_card.dart`: title diganti "Catatan Terkunci" saat `note.isLocked` (Home grid/list).
- `lib/screens/calendar_screen.dart`: title item reminder di tab Kalender diganti "Catatan Terkunci" saat `isLocked`.
- 2 file diubah, 1 task (micro-batch). AUDIT_ISSUES.md #5 ditandai RESOLVED.
- Belum diverifikasi run CI.
- Sisa Pending Queue: High #6 (tab Kalender tidak reaktif) & #7 (modifiedAt berubah tanpa edit) + 3 Medium + 2 Low — lihat AUDIT_ISSUES.md.

## [v1_Batch14] — 2026-08-24
Fix: AUDIT Critical #4 — notification ID dari `note.id.hashCode` berisiko out-of-range.
- File: `lib/services/notification_service.dart`
- Root cause: `String.hashCode` bawaan Dart implementation-defined, tidak dijamin muat 32-bit int Android.
- Fix: tambah `_stableNotificationId()` — hash FNV-1a 32-bit manual di atas byte UTF-8 id, di-mask `& 0x7FFFFFFF` -> selalu positif & muat int32. Dipakai konsisten di `scheduleReminder()` DAN `cancelReminder()` (wajib sama, krn cancel harus match ID yg dipakai saat schedule).
- 1 file diubah, 1 task (micro-batch). AUDIT_ISSUES.md #4 ditandai RESOLVED.
- Belum diverifikasi run CI.
- **Seluruh 4 Critical dari audit Batch10 kini RESOLVED.** Sisa Pending Queue: 3 High + 3 Medium + 2 Low — lihat AUDIT_ISSUES.md, mulai dari High berikutnya.

## [v1_Batch13] — 2026-08-24
Fix: AUDIT Critical #3 — reminder hilang setelah restart HP (no reschedule).
- `lib/providers/notes_provider.dart`: tambah method `rescheduleAllReminders()` — ambil semua note ber-reminder dari repo, panggil `scheduleReminder()` ulang utk masing2.
- `lib/app.dart` (`_AppEntryState._init()`): panggil `rescheduleAllReminders()` sesaat setelah `NotificationService().init()`, tiap kali app dibuka.
- 2 file diubah, 1 task (micro-batch). AUDIT_ISSUES.md #3 ditandai RESOLVED (dengan catatan residual di bawah).
- **Batasan yang tersisa (disengaja, bukan bug)**: reminder di-re-arm saat APP DIBUKA, BUKAN instan saat boot HP. Fix native penuh (RECEIVE_BOOT_COMPLETED + BroadcastReceiver Kotlin + headless Dart execution/android_alarm_manager_plus) butuh perubahan protected assets (AndroidManifest.xml) + dependency baru + native receiver file — di luar scope 1 task micro-batch ini. Jika reminder presisi-tanpa-buka-app dibutuhkan, ajukan sbg task terpisah.
- Belum diverifikasi run CI.
- Sisa Pending Queue: Critical #4 (hashCode ID out-of-range) + 3 High + 3 Medium + 2 Low — lihat AUDIT_ISSUES.md.

## [v1_Batch12] — 2026-08-24
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
