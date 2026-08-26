# PROJECT_STATE — Jotter (Native Kotlin + Jetpack Compose)

## ⚠️ ATURAN PERMANEN (selalu di sini, tidak ikut descending)
- Folder lokal / repo GitHub / package Android: SELALU huruf kecil → `jotter`, `com.jotter.notes`.
- Nama file ZIP output: huruf besar di awal → `Jotter_v2_BatchN.zip`.
- `Jotter` (kapital) = nama file/branding. `jotter` (kecil) = path/folder/repo/package.
- **[INSIDEN Batch4, 2026-08-25] DILARANG KERAS pakai exclude-pattern generik (`zip -x ".*"` atau sejenisnya) saat packaging ZIP proyek ini.** Kejadian: flag itu ikut membuang `.gitattributes`, `.gitignore`, `.github/workflows/release.yml` dari ZIP rilis. Karena Termux `DAILY UPDATE` script (immutable) cuma spare folder `.git` (bukan pola dotfile lain) saat wipe-and-replace, file2 itu ikut ke-`rm -rf` dari repo lokal lalu ke-`git add -A` + push sebagai commit yang MENGHAPUS-nya dari GitHub juga — CI workflow sempat lenyap dari repo tanpa disadari sampai user ngeh. WAJIB: setiap packaging ZIP, SEMUA dotfile/dotfolder project (`.gitattributes`, `.gitignore`, `.github/`) IKUT ke dalam ZIP tanpa exclude apapun (satu2nya yang boleh diexclude, kalau ada, adalah folder `.git` VCS internal itu sendiri — bukan dotfile lain). WAJIB verifikasi isi ZIP (`unzip -l`) sebelum present ke user, cek dotfile penting ada.

## [v2_Batch29] — 2026-08-26 (TERBARU)
Laporan bug via screenshot: "tab reminder untuk catatan mengalami distorsi & gak fleksibel, scrollable sama sekali". Dikonfirmasi dari screenshot — clock dial `TimePicker` kepotong di tepi bawah layar (angka "7"/"5" gak keliatan penuh), gak ada cara scroll buat jangkau sisanya.
- **Root cause**: `ReminderPickerSheet` (di `NoteEditorScreen.kt`) numpuk `DatePicker` (kalender penuh, ~600dp) + `TimePicker` (clock dial, ~300-400dp) dalam 1 `Column` polos TANPA `verticalScroll` sama sekali, dibungkus `ModalBottomSheet`. Total tinggi konten (Row tombol + DatePicker + TimePicker) gampang lebih tinggi drpd viewport layar HP manapun — `ModalBottomSheet` sendiri gak otomatis bikin isinya scrollable, itu tanggung jawab composable pemanggil. Hasilnya: konten yang overflow ke bawah viewport kepotong gitu aja, gak bisa dijangkau (persis sesuai laporan "gak scrollable sama sekali").
- **Fix**: `NoteEditorScreen.kt` — `Column` di `ReminderPickerSheet` ditambah `.verticalScroll(rememberScrollState())`, plus `Spacer(16.dp)` tambahan di paling bawah (jarak napas biar baris terakhir clock dial gak mepet tepi bawah sheet). Import baru: `androidx.compose.foundation.verticalScroll`, `androidx.compose.foundation.rememberScrollState` (paket foundation standar Compose, 0 dependency baru).
- **TIDAK ada perubahan logic** — `datePickerState`/`timePickerState`/tombol Hapus-Pengingat/Selesai semua persis sama, murni fix layout/scroll. 0 resiko regresi ke fitur Reminder itu sendiri (`ReminderScheduler`, Batch15 Snackbar feedback, dst — semua di luar scope perubahan ini).
- 1 file diubah (`NoteEditorScreen.kt`), 1 fix spesifik, jauh di bawah batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, konfirmasi `verticalScroll`/`rememberScrollState` terpakai). **Prioritas tes manual di device**: buka sheet pengingat (ikon lonceng di editor) → pastikan sekarang bisa discroll sampai ke bagian bawah clock dial, semua angka (termasuk 5/6/7 yang sebelumnya kepotong) bisa dijangkau & ditap.
- **Pending Queue**: kosong lagi setelah ini.

## [v2_Batch28] — 2026-08-26
User share screenshot: build #48 (Batch27) sukses di GitHub Actions, 3 APK ABI ke-generate normal. Lanjut dgn PERMINTAAN FITUR BARU (bukan dari backlog `AUDIT_ISSUES.md` — backlog itu sudah kosong sejak Batch27): **tambah shortcut menuju halaman GitHub Release APK**.
- **`SettingsScreen.kt`** (diubah): ListItem baru "Lihat Rilis di GitHub" persis di bawah "Cek Pembaruan" (section "PEMBARUAN APLIKASI") — tap buka browser (`Intent.ACTION_VIEW` + `Uri.parse(...)`) langsung ke `https://github.com/FDzaki-dev/jotter/releases/latest` (GitHub otomatis redirect ke tag rilis terbaru). Beda fungsi dgn "Cek Pembaruan" yang sudah ada: itu alur OTOMATIS in-app (cek API → download → install via `FileProvider`), ini alur MANUAL (kalau user mau lihat changelog lengkap/semua rilis lama/download manual lewat browser sendiri — fallback kalau in-app updater lagi bermasalah, atau sekadar mau intip Release page spt di screenshot yang dikirim).
- **`UpdateChecker.kt`** (diubah, 1 baris visibility): `private const val REPO` → `const val REPO` (TANPA ubah value/logic apapun) — biar `SettingsScreen.kt` bisa reuse `UpdateChecker.REPO` sbg satu2nya sumber kebenaran nama repo, drpd hardcode string `"FDzaki-dev/jotter"` kedua kalinya yang berisiko kelupaan disinkron kalau repo pernah di-rename kelak.
- Icon baru dipakai: `Icons.Default.OpenInNew` (dari `material-icons-extended`, dependency ini SUDAH ada dari awal di `app/build.gradle.kts` — dicek eksplisit sebelum pakai, 0 dependency baru ditambahkan).
- 2 file diubah (`SettingsScreen.kt`, `UpdateChecker.kt`), 1 task ("shortcut GitHub Release"), dalam batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang di ke-2 file, grep konfirmasi `REPO` masih dipakai persis sama di pemanggilan API internal `UpdateChecker` — cuma visibility yang berubah, bukan value/logic, 0 resiko regresi ke fitur in-app updater Batch8-10). **Prioritas tes manual**: tap "Lihat Rilis di GitHub" di Settings → browser harus kebuka ke halaman rilis terbaru repo GitHub yang benar.
- **Pending Queue**: kosong lagi setelah ini — item apapun selanjutnya menunggu instruksi/temuan baru dari user.

## [v2_Batch27] — 2026-08-26
Lanjut pending queue — item TERAKHIR di seluruh backlog `AUDIT_ISSUES.md`: **swatch warna individual `FlowRowColors` gak punya label aksesibilitas** (temuan Batch22, sisi lain dari fix tombol color-picker TopAppBar yang sudah dikerjakan batch itu).
- **`NoteEditorScreen.kt`** (diubah, 1 file): tiap lingkaran warna di bottom sheet color-picker sebelumnya cuma `Box` + `.clickable {}` polos — TalkBack cuma bacain "Button, unlabeled" tanpa tau itu warna apa/lagi dipilih atau bukan. Diganti `.selectable(selected = selected, onClick = {...}, role = Role.RadioButton)` (bukan sekadar nempel `.semantics{contentDescription=...}` di atas `clickable` lama — `selectable` adalah primitive Compose yang memang didesain utk item dlm grup pilihan-tunggal, otomatis nyiapin semantic `role`+`selected` yang bisa diumumkan TalkBack: "Warna Merah, dipilih, tombol radio" dst) + `.semantics { contentDescription = "Warna ${option.label}" }` (pakai field `NoteColorOption.label` yang TERNYATA SUDAH ADA dari awal di `Color.kt` — "Merah"/"Oranye"/dst — cuma belum pernah dipakai buat aksesibilitas, cuma buat internal). `FlowRow` induk ditambah `.selectableGroup()` supaya TalkBack ngumumin sbg 1 grup radio (\"1 of 9\" dst), bukan 9 tombol lepas gak berhubungan.
- Import `androidx.compose.foundation.clickable` dihapus (1 satu2nya pemakaian di file ini sudah diganti `selectable`, 0 sisa referensi — dicek via grep). Tambah `androidx.compose.foundation.selection.selectable`, `.selectableGroup`, `androidx.compose.ui.semantics.Role`. `semantics`/`contentDescription` sudah lama ada (dipakai fix Batch22 di tombol yang sama filenya, style pattern konsisten).
- 1 file diubah, 1 task (item terakhir aksesibilitas P1.8-lanjutan), jauh di bawah batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, grep konfirmasi `clickable` benar2 0 pemakaian tersisa sebelum importnya dihapus, konfirmasi `selectable`/`Role`/`selectableGroup` semua terpakai).
- **🎉 SELURUH backlog UX/UI POLISH `AUDIT_ISSUES.md` (P1.5 → P1.6 → P1.8 → P1.9 → P2.10 → P2.11 → P2.12 → P2.13 → swatch aksesibilitas) KINI LENGKAP/CLOSED SEMUA.** Begitu juga seluruh item Logic/Keamanan P0-P1 (fix keamanan kritis Batch13, PIN/biometrik Batch14/16, dst).
- **Pending Queue KOSONG** — tidak ada item backlog tersisa dari audit awal. Item apapun selanjutnya butuh instruksi/temuan baru dari user (fitur baru, bug report baru, atau audit ulang dari awal kalau user mau).

## [v2_Batch26] — 2026-08-26
Lanjut pending queue — **P2.13: kurangi info developer-facing di footer Settings**.
- **`SettingsScreen.kt`** (diubah, 1 file): footer paling bawah sebelumnya "Jotter v2.0 (Native Kotlin) · 100% offline · Log crash tersimpan di Documents/Jotter/logs" — 2 detail developer-facing dibuang: label stack teknis `(Native Kotlin)` (gak relevan/membingungkan bagi user awam kenapa app perlu nyebut bahasa pemrogramannya) dan path file internal mentah `Documents/Jotter/logs` (info debug, bukan sesuatu yang perlu dilihat user biasa tiap buka Pengaturan — sesuai wording asli `AUDIT_ISSUES.md`: "Kurangi info developer-facing di Settings (path log internal)").
- Diganti "Jotter · 100% offline, semua catatan tersimpan di perangkat ini" — bagian "100% offline" SENGAJA dipertahankan (bukan dev-facing, ini justru sinyal kepercayaan/privasi yang genuinely berguna buat user notes app), cuma diperjelas dgn bahasa awam ("semua catatan tersimpan di perangkat ini") drpd jargon. Nomor versi `v2.0` juga gak hilang infonya — sudah ditampilkan terpisah di baris "Cek Pembaruan" (`currentVersionName` dinamis dari `PackageManager`, lebih akurat drpd hardcode `v2.0` di footer yang gak auto-update tiap rilis).
- **Fitur Crash Logger (MediaStore, wajib per Feature Lock) SENDIRI TIDAK disentuh sama sekali** — ini murni soal 1 baris teks footer yang nampilin path-nya ke user, bukan soal logger-nya berhenti jalan.
- 1 file diubah, 1 task (P2.13 penuh), jauh di bawah batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, grep konfirmasi 0 referensi tersisa ke path lama/label tech-stack di seluruh `app/src/main/java`).
- **P2.13 kini CLOSED.**
- **Sisa Pending Queue**: swatch warna individual `FlowRowColors` (aksesibilitas — tiap lingkaran warna di bottom sheet color-picker `NoteEditorScreen.kt` kemungkinan besar gak ada `contentDescription`/label nama warna per index, temuan Batch22, belum pernah dicek/dieksekusi). Ini SATU-SATUNYA item tersisa di seluruh pending queue — kalau sudah dikerjakan, backlog UX/UI polish dari `AUDIT_ISSUES.md` (P1.5 s/d P2.13) akan LENGKAP semua.

## [v2_Batch25] — 2026-08-26
Lanjut pending queue — **P2.12: color/border treatment**.
- **`NoteCard.kt`** (diubah, 1 file): border seragam 1.5dp + dot kecil 10dp di header diganti **accent bar kiri 4dp (warna kategori penuh) + tint background** (`lerp(JotterSurface, accentColor, 0.14f)` — fungsi standar Compose, bukan alpha-composite manual). Alasan: spec awal ("Kategorisasi Warna: 9 pilihan warna dasar") ngambil inspirasi ColorNote yang kartu-nya BENERAN berwarna, bukan cuma outline tipis di atas kartu netral gelap — treatment lama under-represent maksud fitur ini. Dot header dihapus (jadi redundan skrg warna sudah jadi identitas visual dominan kartu, bukan cuma aksen kecil).
- **Detail teknis**: struktur `NoteCardContent` diubah dari `Column` tunggal jadi `Row(height=IntrinsicSize.Min) { Box(accent bar, fillMaxHeight) ; Column(weight=1f, isi lama) }` — `IntrinsicSize.Min` WAJIB di sini (pola standar Compose) supaya accent bar bisa `fillMaxHeight()` ngikutin tinggi natural Column di sebelahnya (row wrap-content, bukan constraint tetap). Clip 16dp radius sekarang di level `Row` terluar (bukan `Column` dalam) — otomatis accent bar ikut kepotong rounded di sudut kiri-atas/kiri-bawah tanpa perlu shape terpisah utk Box-nya.
- **Reminder badge (warna overdue/lock-masking, Batch17) & typography/spacing (P2.10/P2.11) TIDAK disentuh** — di luar scope warna/border, dicek eksplisit tetap sama persis kode sebelumnya, cuma dipindah 1 level nesting lebih dalam (`Column` extra) tanpa perubahan isi.
- Import dibersihkan: `BorderStroke`, `.border(...)`, `CircleShape` (semua cuma dipakai treatment lama, sekarang 0 pemakaian) dihapus; `androidx.compose.ui.graphics.lerp` ditambah.
- 1 file diubah, 1 task (P2.12 penuh), jauh di bawah batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung kurawal 27/27 & kurung biasa 103/103 seimbang via script, grep konfirmasi 0 pemakaian tersisa utk 3 import yang dihapus). **Prioritas tes visual manual**: pastikan accent bar keliatan proporsional (gak terlalu tipis/tebal) di kartu grid sempit, tint background gak bikin teks abu-abu (`Color.Gray` content/checklist) jadi kurang kontras di atas background yang sudah gak polos hitam lagi — terutama utk warna terang (Kuning/Toska) yang tint 14%-nya paling kentara.
- **P2.12 kini CLOSED.**
- **Sisa Pending Queue**: P2.13 kurangi info developer-facing di footer Settings, swatch warna individual `FlowRowColors` (aksesibilitas, temuan Batch22).

## [v2_Batch24] — 2026-08-26
Lanjut pending queue — ambil **P2.11: spacing/proporsi NoteCard Grid**, sekalian terapkan token `titleMedium`/`bodySmall` baru (Batch23) ke title/content — persis seperti disarankan di catatan Batch23.
- **`NoteCard.kt`** (diubah, 1 file): title (baik state normal maupun "Catatan Terkunci") sekarang pakai `MaterialTheme.typography.titleMedium` (17sp SemiBold) ganti hardcode `fontWeight = FontWeight.SemiBold` polos (defaultnya ikut ukuran ambient LocalTextStyle, tidak konsisten dgn skala Jotter). Content teks biasa, teks item checklist, dan placeholder "•••••••" note terkunci sekarang pakai `MaterialTheme.typography.bodySmall` (13sp) — sebelumnya tanpa `style` sama sekali (ikut default Material3 generik, sama masalahnya dgn yang dibereskan P2.10 di `Theme.kt` tapi belum ikut nyampe ke pemakaian di kartu ini).
- **Spacing/proporsi**: padding internal kartu `12.dp` → `14.dp` (nafas lebih lega). Spacer setelah header row (baris ikon warna/lock/reminder) `6.dp` → `8.dp` — mengimbangi title yang sekarang render lebih besar (17sp drpd sebelumnya ikut ambient ~14-16sp). Item checklist dikasih `verticalArrangement = Arrangement.spacedBy(2.dp)` (sebelumnya rapat tanpa jarak eksplisit antar baris item).
- **Reminder badge (`labelSmall`) & warna overdue/lock-masking dari Batch17 TIDAK disentuh** — di luar scope P2.11 (itu urusan P1.8/warna, bukan spacing/tipografi), dicek eksplisit tetap konsisten dgn kode sebelumnya.
- `FontWeight` import jadi tidak terpakai lagi (2 pemakaian satu2nya di file ini sudah diganti `style=`) — dihapus, cegah lint warning unused-import.
- 1 file diubah, 1 task (P2.11 penuh, sesuai cakupan yang sudah digariskan Batch23), jauh di bawah batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung kurawal 26/26 & kurung biasa 103/103 seimbang via script, `FontWeight` dikonfirmasi tidak dipakai lagi sebelum dihapus). **Prioritas tes visual manual**: grid Home — pastikan title gak kepotong/wrap aneh di kartu sempit dgn ukuran 17sp yang lebih besar drpd sebelumnya, spacing kerasa lebih lega tapi gak longgar berlebihan.
- **P2.11 kini CLOSED.**
- **Sisa Pending Queue**: P2.12 color/border treatment, P2.13 kurangi info developer-facing di footer Settings, swatch warna individual `FlowRowColors` (aksesibilitas, temuan Batch22).

## [v2_Batch23] — 2026-08-26
Lanjut pending queue — masuk P2 (murni polish, gak ada lagi item logic/keamanan tersisa). Ambil **P2.10: perkuat typography hierarchy**.
- **Root cause ditemukan**: `JotterTypography` di `Theme.kt` cuma nge-define 5 dari 15 role Material3 (`headlineLarge`, `titleLarge`, `bodyLarge`, `bodyMedium`, `labelSmall`). 10 role SISANYA diam2 jatuh ke default generik Material3 (Roboto standar Android) — efeknya NYATA & gak keliatan dari baca kode screen manapun: `LargeTopAppBar` (dipakai `HomeScreen.kt`) pas state EXPANDED render title pakai role `headlineMedium`, dan SEMUA `Button`/`TextButton` di seluruh app (mis. sort-sheet di `HomeScreen.kt`) render teksnya pakai role `labelLarge` — keduanya gak pernah disentuh skala custom Jotter, jadi diam2 beda size/weight/font dari 5 role yang sudah di-branding.
- **`Theme.kt`** (diubah, 1 file): lengkapi 10 role yang hilang (`displayLarge/Medium/Small`, `headlineMedium/Small`, `titleMedium/Small`, `bodySmall`, `labelLarge/Medium`) jadi 1 skala turun yang koheren, interpolasi dari 5 nilai lama yang TIDAK DIUBAH SAMA SEKALI (`headlineLarge`, `titleLarge`, `bodyLarge`, `bodyMedium`, `labelSmall` persis sama). **TIDAK sentuh `fontFamily`** (di luar scope "hierarchy" - itu soal ukuran/weight relatif antar level, bukan ganti jenis huruf/branding font, yg juga akan jadi refactor lebih besar & berresiko drpd 1 file token definition).
- **Keputusan scope**: NoteCard title/content sendiri (di `NoteCard.kt`) SENGAJA belum disentuh biar size-nya eksplisit beda dari ambient default — itu overlap sama **P2.11 (spacing/proporsi NoteCard)** yang MEMANG scope-nya spesifik NoteCard, dibiarkan jadi 1 paket kerjaan terpisah biar gak tumpang tindih 2 tiket sekaligus dalam 1 batch.
- 1 file diubah (`Theme.kt`), 1 task (P2.10 penuh), jauh di bawah batas micro-batch, **0 file screen disentuh** (murni token-level, resiko regresi visual minim krn additive-only — 5 role lama gak berubah nilai).
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, semua `TextStyle(...)` pakai constructor signature yang sama persis dgn 5 yang sudah ada, konfirmasi gak ada duplikat parameter). **Prioritas tes visual manual di device**: cek tombol (TextButton sort-sheet) & title "Catatan" pas scroll (LargeTopAppBar collapse/expand) — pastikan look lebih koheren, gak ada teks yang keliatan aneh/kegedean/kekecilan drpd sebelumnya.
- **P2.10 kini CLOSED.**
- **Sisa Pending Queue**: P2.11 spacing/proporsi NoteCard Grid (kandidat lanjutan yg bisa sekalian terapkan `titleMedium`/`bodySmall` baru ke title/content NoteCard), P2.12 color/border treatment, P2.13 kurangi info developer-facing di footer Settings. Swatch warna individual `FlowRowColors` (temuan Batch22, aksesibilitas, belum dieksekusi).

## [v2_Batch22] — 2026-08-26
Lanjut pending queue — item terakhir P1.8: **tombol color-picker di TopAppBar `NoteEditorScreen.kt` gak punya `contentDescription` sama sekali** (gap aksesibilitas, ditemukan Batch17).
- **`NoteEditorScreen.kt`** (diubah, 1 file): tombol color-picker isinya `Box` polos (bukan `Icon`, jadi gak ada parameter `contentDescription` langsung spt tombol Pengingat/Kunci di sebelahnya yang sudah dikasih label sejak lama) — ditambah `Modifier.semantics { contentDescription = "Pilih warna catatan" }`. Sekarang TalkBack bacain "Pilih warna catatan" pas fokus ke tombol ini, sebelumnya senyap/gak kebaca sama sekali.
- **Ditemukan tapi SENGAJA belum disentuh (di luar scope 1-line fix ini)**: swatch warna individual di dalam `FlowRowColors` (bottom sheet color-picker) kemungkinan besar punya gap serupa (tiap lingkaran warna kemungkinan juga gak ada label "Warna Merah"/"Warna Biru" dst) — tapi itu N-item (butuh nama warna per index dari `NoteColors`), scope-nya beda & lebih besar drpd 1 tombol TopAppBar. Dicatat sbg kandidat terpisah, BUKAN diasumsikan otomatis sama pentingnya/segera dikerjakan.
- 1 file diubah, 1 fix spesifik, jauh di bawah batas micro-batch.
- **P1.8 (discoverability, mencakup swipe-hint Batch20 + color-picker label batch ini) kini LENGKAP/CLOSED.**
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, import `semantics`/`contentDescription` baru dari `androidx.compose.ui.semantics.*` — paket standar Compose, bukan dependency baru).
- **Sisa Pending Queue murni P2 (visual polish, gak ada lagi item logic/keamanan/discoverability tersisa)**: P2.10 typography hierarchy, P2.11 spacing/proporsi NoteCard Grid, P2.12 color/border treatment, P2.13 kurangi info developer-facing di footer Settings (masih tampilkan path log internal "Documents/Jotter/logs"). Swatch warna individual (temuan baru batch ini, lihat poin di atas) — kandidat P1.8-lanjutan kalau user mau lanjutkan aksesibilitas lebih dalam drpd langsung ke P2.

## [v2_Batch21] — 2026-08-26
Lanjut pending queue — **P1.9: checklist add/remove feedback + perbesar target tombol hapus item** (`NoteEditorScreen.kt`, diubah, 1 file).
- **Hapus item checklist**: sebelumnya senyap total (item lenyap, 0 jejak/cara balikin). Sekarang pola IDENTIK `archiveWithUndo`/`deleteWithUndo` HomeScreen (Batch12/20): hapus dulu (list tetap responsif), Snackbar "Item dihapus" + tombol "Urungkan" — undo masukin balik item PERSIS di index semula (`removedIndex` ditangkap SEBELUM filter, `coerceIn` jaga2 kalau list berubah pas snackbar masih tampil), bukan di-append ke akhir list (biar urutan checklist gak berantakan). Reuse `scope`+`snackbarHostState` yang sudah ada di file ini sejak Batch15 — **0 state/wiring baru**.
- **Tambah item checklist**: SENGAJA TIDAK diberi Snackbar tambahan — item baru langsung nongol keliatan di list, itu sendiri sudah feedback yang cukup & non-ambigu (beda kasus dgn hapus yang destruktif/perlu jalan keluar kalau ke-tap gak sengaja). Nambah toast di tiap ketikan item baru cuma bikin berisik/keseringan interupsi kalau user lagi buru2 isi banyak item.
- **Perbesar target tombol hapus item**: icon "X" sebelumnya `18.dp` (lebih kecil dari icon checkbox sebelahnya yang default `24.dp`) — dibesarkan jadi `22.dp`, lebih konsisten & gampang dilihat/ditarget. `contentDescription` juga ditambah ("Hapus item", "Tambah item" — sebelumnya `null` semua, gap aksesibilitas kecil ikut kebenerin sekalian krn di baris yang sama).
- **Catatan teknis**: `IconButton` Material3 SUDAH otomatis kasih minimum touch-target 48dp terlepas dari ukuran icon visualnya (`minimumInteractiveComponentSize()` bawaan) — jadi target tap SECARA TEKNIS sudah accessible sejak awal; perubahan batch ini murni soal AFFORDANCE VISUAL (kelihatan lebih gampang ditarget, bukan cuma "beneran" bisa ditarget).
- 1 file diubah, 1 task (P1.9 lengkap), jauh di bawah batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, `removedIndex`/`item` captured correctly di closure sebelum filter, konfirmasi tidak ada import baru yang dibutuhkan — `SnackbarDuration`/`SnackbarResult`/`scope.launch` semua sudah ada dari Batch15).
- **P1.9 kini CLOSED.**
- **Sisa Pending Queue**: color-picker tooltip/`contentDescription` (P1.8 sisa, minor aksesibilitas), P2.10 typography hierarchy, P2.11 spacing/proporsi NoteCard Grid, P2.12 color/border treatment, P2.13 kurangi info developer-facing di footer Settings.

## [v2_Batch20] — 2026-08-26
User konfirmasi build #40 sukses (nama APK unik per Assets, gak ada lagi collision). Kembali ke pending queue app (P1.8/P1.9/P2, sempat diselak Batch18/19 yang soal workflow) — ambil item prioritas yang disarankan Batch17: **swipe-actions discoverability**.
- **`HomeScreen.kt`** (diubah, 1 file): tambah banner info statis (TANPA animasi, sesuai constraint verdict UX/UI) "Geser kartu ke kanan untuk arsip, ke kiri untuk hapus" — muncul di atas list/grid HANYA kalau ada catatan (`notes.isNotEmpty()`, gak ada gunanya banner kalau list kosong) DAN belum pernah di-dismiss. Tombol ✕ di banner dismiss PERMANEN (disimpan `SharedPreferences("ui_prefs")`, bukan per-sesi — sekali ditutup, gak muncul lagi selamanya, gak ganggu user yang udah paham).
- **Scope sengaja dibatasi cuma `HomeScreen.kt`** (bukan juga `FilteredNotesScreen.kt` yang notabene pakai swipe-action sama persis) — ini layar utama/pertama yang dilihat user baru, dampak discoverability paling besar di sini. `FilteredNotesScreen.kt` (tab Arsip/Sampah) ditinggal dulu, dicatat sbg kandidat kalau user mau banner yang sama di sana juga.
- Sisa P1.8 yang BELUM disentuh batch ini: tooltip/`contentDescription` color-picker di `NoteEditorScreen.kt` (gap aksesibilitas minor, ditemukan Batch17).
- 1 file diubah, 1 sub-task spesifik dari P1.8, jauh di bawah batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, semua import baru — `background`, `RoundedCornerShape`, `clip`, `LocalContext`, `Context` — memang dipakai & belum ada sebelumnya di file ini, 0 import nganggur).
- **Sisa Pending Queue**: color-picker tooltip (P1.8 sisa), P1.9 checklist add/remove feedback + perbesar target tombol hapus item, P2.10 typography hierarchy, P2.11 spacing/proporsi NoteCard Grid, P2.12 color/border treatment, P2.13 kurangi info developer-facing di footer Settings.

## [v2_Batch19] — 2026-08-26
User konfirmasi Batch18 jalan bagus di production (screenshot Release page build #39: body + changelog otomatis tampil rapi). Lanjut temuan baru dari user, masih tema sama ("output kurang informatif"): **nama file APK di Assets section IDENTIK di setiap rilis** (`app-arm64-v8a-release.apk` dst, sama persis tiap build) — biang kerok kalau user download APK dari beberapa rilis berbeda, browser/Downloads folder jadi isinya "app-arm64-v8a-release.apk", "app-arm64-v8a-release (1).apk", dst — gak bisa dibedain APK versi/build mana tanpa buka & cek dulu.
- **`.github/workflows/release.yml`** (diubah, edit parsial, lanjutan tema Batch18):
  1. Step baru **"Rename APK outputs"** (persis setelah "Build release APK", sebelum "Build summary"): rename tiap `app-<abi>-release.apk` jadi `Jotter-v<versionName>-build<run_number>-<abi>.apk` (contoh nyata: `Jotter-v2.0.0-build39-arm64-v8a.apk`) — ABI di-extract dari nama file lama pakai `sed`, gak hardcode daftar ABI (otomatis ngikut kalau nanti ABI split di `build.gradle.kts` berubah, 0 dependency ke tempat lain).
  2. Step "Build summary" & "Publish GitHub Release": glob pattern disesuaikan dari `app-*-release.apk` → `Jotter-v*.apk` (nama file sudah beda sejak step Rename di atas). Body release: daftar APK di teks sekarang pakai expression dinamis yang sama persis dgn yang dipakai step Rename (`Jotter-v${{...app_version}}-build${{...run_number}}-<abi>.apk`) — jadi nama yang ditampilkan di body PASTI sama persis dgn nama asset asli, gak ada resiko typo/beda antara teks deskripsi vs file sungguhan.
- **Dicek eksplisit, TIDAK ADA regresi ke `ReleaseUpdater.kt` (fitur updater Batch9+)**: fungsi pencocokan asset di sana cuma cek `name.endsWith(".apk") && name.contains(supportedAbi)` — substring ABI (`arm64-v8a`/`armeabi-v7a`/`x86_64`) tetap ada verbatim di nama baru, cuma nambah prefix/versi di depannya. **0 file Kotlin disentuh, murni workflow-level.**
- 1 file diubah (`.github/workflows/release.yml`), 1 task spesifik, jauh di bawah batas micro-batch.
- Belum diverifikasi CI (perlu push sungguhan + cek Assets section beneran ke-rename, gak bisa disimulasikan tanpa akses GitHub Actions runner).

## [v2_Batch18] — 2026-08-26
Pending queue baru dari user: **perbaiki output workflow GitHub yang serampangan & kurang informatif**. Konfirmasi via baca `.github/workflows/release.yml`: keluhan paling tepat sasaran = halaman GitHub Release yang dihasilkan tiap build SELAMA INI kosong total (cuma nama `Jotter build-YYYYMMDD-N`, tanpa body/deskripsi apapun) + tab Summary tiap run di Actions juga kosong (gak ada info apa2 selain raw gradle log yang panjang).
- **`.github/workflows/release.yml`** (diubah, edit parsial — bukan rewrite total, sesuai Protected Files, izin eksplisit dari task user ini):
  1. Step "Determine version identifiers": tambah output `commit_msg` (pesan commit singkat, pakai heredoc `<<EOF` di `$GITHUB_OUTPUT` — cara aman GitHub utk value yang berpotensi ada karakter aneh, bukan cuma `echo "x=$Y"` biasa).
  2. Step baru **"Build summary"** (setelah build sukses, sebelum upload/release): tulis tabel markdown (versi, commit, daftar APK per-ABI + ukuran file `du -h`) ke `$GITHUB_STEP_SUMMARY` — sekarang buka tab "Summary" tiap run langsung keliatan ringkas, gak perlu scroll raw log gradle yang ratusan baris. Otomatis ke-skip kalau build gagal (default behavior Actions: step tanpa `if:` di-skip kalau step sebelumnya fail — konsisten sama step "Upload failure logs" yang sudah lebih dulu pakai pola `if: failure()`).
  3. Step "Publish GitHub Release": `name:` diganti jadi lebih manusiawi (`"Jotter 2.0.0 · build #18"` drpd `"Jotter build-20260826-18"`), tambah `body:` terstruktur (commit + build number + daftar 3 APK per-ABI dgn saran "gak yakin pakai arm64-v8a"), tambah `generate_release_notes: true` (GitHub otomatis nambahin changelog commit sejak rilis terakhir DI BAWAH body custom di atas — bukan gantiin, `softprops/action-gh-release` append keduanya).
- **TIDAK disentuh** (di luar scope "informativeness", bagian logic/keamanan yang sudah WAJIB per Feature Lock): urutan step, "Stale Run Guard" (logic + posisinya persis sama), cara decode keystore, artifact naming utk failure log (`logs_fail_...` — sudah dipakai alur debugging Batch4, kalau diubah bisa bikin bingung riwayat lama).
- Filename asset APK (`app-<abi>-release.apk`) & format `tag_name` (`build-YYYYMMDD-N`) TIDAK berubah — dicek eksplisit gak ada regresi ke `ReleaseUpdater.kt` (Batch9+) yang parse `tag_name` utk equality-check & cocokin nama asset ke ABI device.
- 1 file diubah (`.github/workflows/release.yml`), 1 task, jauh di bawah batas micro-batch.
- Belum diverifikasi CI (perlu push sungguhan buat lihat hasil Summary tab + Release page beneran, gak bisa disimulasikan di sandbox tanpa akses GitHub Actions).

## [v2_Batch17] — 2026-08-26
Lanjut pending queue Batch16: **P1.8 (discoverability)** — mulai dari bagian "reminder tampilkan status/tanggal lebih informatif" (sub-item paling self-contained, 1 file, tanpa nyenggol swipe-hint/color-picker yg beda scope/file).
- **`NoteCard.kt`** (diubah, 1 file): badge pengingat di kartu (Home/Arsip/Sampah — semua reuse komponen ini, otomatis ke-cover 1 tempat) sebelumnya CUMA ikon lonceng polos tanpa info apapun. Sekarang tampilkan teks tanggal/jam di sebelah ikon: `"14:30"` kalau pengingat hari ini, `"26 Agu 14:30"` kalau bukan (fungsi baru `formatReminderBadge()`, format singkat sengaja dipilih krn kartu grid 2-kolom sempit). Pengingat yang SUDAH LEWAT (`reminderAt < now`) ikon+teks jadi merah (`0xFFFF3B30`, warna sama dgn aksi Hapus/delete-swipe yg sudah dipakai di file yg sama) — tidak lewat tetap abu2 netral spt sebelumnya.
- **Invariant masking TETAP dijaga**: utk note yang `isLocked`, badge SENGAJA tetap ikon polos tanpa tanggal/jam (persis perilaku lama) — tanggal/jam spesifik dianggap metadata baru yang berpotensi bocorkan konteks note terkunci kalau ditampilkan, jadi sengaja tidak diberi treatment baru yang sama dengan note biasa. Ini konsisten dgn prinsip yang sudah berulang kali ditegaskan sejak Batch1/11/13 (jangan ada kebocoran baru lewat jalur UI manapun).
- Import baru: `java.util.Calendar` (pola sama persis dgn yang sudah dipakai `CalendarScreen.kt`, 0 pendekatan baru). `java.text.SimpleDateFormat`/`java.util.Locale` dipakai fully-qualified inline (juga sama gaya `CalendarScreen.kt` baris formatting bulan) — tidak nambah import lagi.
- 1 file diubah, 1 sub-task spesifik dari P1.8, jauh di bawah batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang; perlu tes visual manual di device utk pastikan badge tanggal gak overflow/kepotong di lebar kartu grid 2-kolom pada device layar kecil).
- **Sisa P1.8**: swipe-actions discoverability (belum ada hint visual "geser kartu utk arsip/hapus" bagi user baru — kandidat: banner info singkat di `HomeScreen.kt`/`FilteredNotesScreen.kt`, TANPA animasi berlebihan sesuai constraint verdict UX/UI) + color-picker discoverability (tombol lingkaran warna di TopAppBar `NoteEditorScreen.kt` tidak punya `contentDescription`/tooltip apapun — gap aksesibilitas kecil yang ketemu saat audit batch ini, belum dieksekusi). P1.9 checklist add/remove feedback + perbesar target tombol hapus item. P2.10–2.13 visual polish. Prioritas next-batch disarankan: swipe-actions discoverability (paling terasa dampaknya utk user baru drpd color-picker tooltip yg lebih minor).

## [v2_Batch16] — 2026-08-26
Lanjut item pending Batch14/15 (nonaktifkan PIN padahal ada note terkunci). Ditanya ke user dulu (4 opsi: blokir total / dialog konfirmasi / perkuat pesan / auto-buka kunci semua) krn ini keputusan desain, bukan sekadar eksekusi — user jawab **"apapun, asalkan gak merugikan/menyulitkan user itu sendiri"**.
- **Keputusan**: pilih **"perkuat pesan"** (opsi paling minim friksi, TIDAK blokir apapun, TIDAK ubah data note otomatis). Alasan milih ini drpd 3 opsi lain thd kriteria user:
  - Blokir total → jelas "menyulitkan" (maksa user buka kunci semua note dulu sebelum bisa matiin PIN).
  - Dialog konfirmasi → nambah 1 tap ekstra tiap kali, friksi kecil tapi tetap friksi yg gak perlu kalau cukup diinfokan lewat pesan.
  - Auto-buka kunci semua catatan → berpotensi "merugikan" dari sisi keamanan (note yg sengaja dikunci user jadi kebuka otomatis tanpa aksi eksplisit per-note dari user) — dianggap lebih beresiko drpd cuma info pesan.
  - Perkuat pesan → 0 friksi (PIN tetap langsung nonaktif spt sebelumnya), 0 resiko keamanan tambahan (isLocked note TIDAK disentuh), user cukup dikasih tau jalan keluarnya kalau nanti nemu note yg gak bisa dibuka.
- **`SettingsScreen.kt`** (diubah, 1 file): Snackbar toggle PIN mati diperkuat jadi "Kunci PIN dinonaktifkan. Kalau ada catatan yang masih terkunci, atur PIN baru lagi untuk membukanya." (durasi Long, sebelumnya cuma "Kunci PIN dinonaktifkan" tanpa konteks apapun). Kalimat sengaja dibuat kondisional ("kalau ada...") biar tetap akurat & tidak membingungkan baik utk user yang punya note terkunci maupun yang tidak — TIDAK perlu query count note terkunci (gak nambah dependency `NoteRepository` ke `SettingsViewModel` yg selama ini cuma bungkus `AuthManager`+updater, sesuai Zero-Unnecessary-Refactor).
- 1 file diubah, 1 task spesifik, jauh di bawah batas micro-batch.
- **Item pending queue Batch14/15 ini sekarang CLOSED** (bukan bug lagi, keputusan desain sudah dieksekusi sesuai preferensi user).
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang).
- **Sisa Pending Queue**: P1.8 discoverability swipe actions (hint visual "swipe utk arsip/hapus"), P1.9 checklist add/remove feedback + perbesar target tombol hapus item, P2.10 typography hierarchy, P2.11 spacing/proporsi NoteCard Grid, P2.12 color/border treatment, P2.13 kurangi info developer-facing di footer Settings (masih tampilkan path log internal "Documents/Jotter/logs"). Semua item logic/keamanan yg tercatat sejauh ini (P0, P1.4, P1.5, P1.6, warning-PIN) SUDAH selesai — sisa murni polish UX/UI kosmetik P1.8/P1.9/P2. Prioritas next-batch disarankan: P1.8 (discoverability) — paling terasa dampaknya utk user baru drpd item visual-only P2.

## [v2_Batch15] — 2026-08-26
Lanjut instruksi "next" — ambil opsi pertama dari 2 kandidat pending queue Batch14: **selesaikan P1.5 penuh** (bagian Reminder + Lock di `NoteEditorScreen.kt`, satu2nya sisa dari tema "action feedback" yang dimulai Batch12/14). Item warning-PIN (kandidat kedua Batch14, soal note terkunci permanen tidak bisa dibuka kalau PIN dihapus) SENGAJA belum disentuh — beda tema/file scope, ditinggal di pending queue.
- **`NoteEditorScreen.kt`** (diubah, 1 file): tambah `SnackbarHostState` (reuse `val scope = rememberCoroutineScope()` yang ternyata SUDAH ada di kode tapi selama ini tidak terpakai sama sekali — dead code sejak entah batch mana, sekarang akhirnya dipakai, 0 deklarasi baru utk scope) + wired ke `Scaffold(snackbarHost = ...)`.
  - Toggle ikon Kunci (per-note, di TopAppBar): sekarang Snackbar "Catatan dikunci" / "Catatan tidak lagi dikunci" (sebelumnya senyap total, cuma icon Lock/LockOpen yg berubah).
  - `ReminderPickerSheet` — "Selesai" (set/ubah waktu): Snackbar "Pengingat diatur — aktif setelah catatan disimpan" (disclaimer disengaja: `ReminderScheduler.schedule()` PADA KENYATAANNYA baru benar2 dipanggil dari `NotesViewModel.saveNote()`, BUKAN saat milih waktu — kalau Snackbar cuma bilang "Pengingat diatur" tanpa disclaimer, user bisa salah kira alarm sudah aktif walau belum sempat disimpan/keluar dari editor).
  - "Hapus Pengingat": Snackbar "Pengingat dihapus dari catatan ini" HANYA muncul kalau `note.reminderAt` sebelumnya memang tidak null (dicek dgn `hadReminder` sebelum di-null-kan) — cegah pesan palsu/membingungkan kalau user pencet "Hapus Pengingat" padahal note itu belum pernah punya pengingat sama sekali (tombolnya sengaja selalu tampil di sheet, tidak conditional).
- **Tidak ada import baru** — `androidx.compose.material3.*` (utk `SnackbarHostState`/`SnackbarHost`/`SnackbarDuration`) dan `kotlinx.coroutines.launch` sudah lama ada di file ini.
- 1 file diubah, 1 tema batch ("P1.5 Reminder+Lock feedback, melengkapi Archive/Delete/Restore Batch12 + PIN/Biometrik Batch14"), jauh di bawah batas micro-batch.
- **P1.5 (Archive/Delete/Restore/Reminder/Lock, sesuai daftar asli AUDIT_ISSUES.md) kini LENGKAP di semua 5 cakupannya.**
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, konfirmasi tidak ada import yang hilang/dobel).
- **Sisa Pending Queue**: item warning-PIN dari Batch14 (nonaktifkan PIN app padahal ada note ter-lock → note itu permanen tidak bisa dibuka; butuh dialog konfirmasi + query count note `isLocked`, kemungkinan sentuh `NoteRepository`/`NotesViewModel`, desain terpisah dari batch ini). P1.8 discoverability swipe actions, P1.9 checklist add/remove feedback + perbesar target tombol hapus item, P2.10–2.13 visual polish (typography/spacing/color/footer info). Prioritas next-batch disarankan: item warning-PIN (menyentuh keamanan/data-access, lebih tinggi drpd P1.8/P1.9/P2 yang murni kosmetik) — tapi tunggu instruksi eksplisit user krn butuh keputusan desain (blocking vs cuma warning) yang belum dikonfirmasi.

## [v2_Batch14] — 2026-08-26
User konfirmasi via tes manual di device: fix Batch13 (gerbang PIN/biometrik sebelum note terkunci dibuka) **berhasil** — "note yang digembok sekarang sudah wajib melewati pengamanan sandi/biometrik, tidak seperti sebelumnya". Lanjut ke pending queue: **P1.5 Action Feedback untuk Lock** (bagian yang belum sejak Batch12 — waktu itu P1.5 baru kepakai utk Archive/Delete/Restore, bagian Lock/Reminder ditunda, lalu keburu diselak Batch13 P0).
- **`AuthManager.kt`** (diubah, edit parsial — bukan file inti/protected): tambah `biometricUnavailableReason(): String?` di samping `canUseBiometrics()` yang lama (TIDAK dihapus/diubah, masih dipakai `LockScreen.kt` — 0 risiko regresi di sana). Fungsi baru memetakan kode `BiometricManager.canAuthenticate()` ke pesan spesifik (tidak ada sensor / sensor lagi bermasalah / belum ada sidik jari-wajah terdaftar / perlu update keamanan sistem), bukan cuma boolean.
- **`SettingsScreen.kt`** (diubah): tambah `SnackbarHostState` + `rememberCoroutineScope()`, wired ke `Scaffold(snackbarHost = ...)` (pola identik `HomeScreen.kt`/`FilteredNotesScreen.kt` dari Batch12, 0 pola baru). Toggle "Kunci Aplikasi (PIN)" mati → Snackbar "Kunci PIN dinonaktifkan" (sebelumnya senyap total). Toggle "Gunakan Biometrik": nyala & berhasil → "Biometrik diaktifkan"; mati → "Biometrik dinonaktifkan"; nyala tapi GAGAL → sekarang tampilkan `auth.biometricUnavailableReason()` di Snackbar (durasi Long) — **sebelumnya toggle ini gagal 100% senyap tanpa penjelasan apapun kalau `canUseBiometrics()` false, ini persis Audit Medium #9 yang sudah RESOLVED di v1 Flutter (Batch18) tapi ternyata TIDAK ikut terbawa saat porting ke Kotlin v2 (tercatat sbg pending queue sejak v2_Batch7, belum pernah dieksekusi sampai batch ini)**.
- Toggle PIN nyala (`onOpenLockSetup()`) sengaja TIDAK diberi Snackbar di titik ini — itu cuma navigasi ke layar setup, keberhasilan/gagalnya baru diketahui di layar itu sendiri, bukan di titik toggle.
- **Item baru ditemukan, SENGAJA belum dieksekusi (di luar scope batch ini)**: mematikan PIN saat masih ada note yang di-`isLocked` menyebabkan note itu permanen tidak bisa dibuka lagi (lihat catatan edge-case Batch13 — `verifyPin()` selalu `false` tanpa PIN tersimpan). Snackbar batch ini cuma kasih tahu "PIN dinonaktifkan" SETELAH kejadian, belum benar2 mencegah/memperingatkan user SEBELUM menonaktifkan kalau ada note terkunci yang akan jadi tidak bisa diakses. Idealnya: dialog konfirmasi dgn query jumlah note `isLocked` dulu sebelum `auth.clearPin()`. Butuh keputusan desain terpisah (blocking vs cuma warning) + kemungkinan sentuh `NoteRepository`/`NotesViewModel` utk query count — dicatat sbg kandidat batch berikutnya, BUKAN diasumsikan otomatis dikerjakan.
- 2 file diubah (`AuthManager.kt`, `SettingsScreen.kt`), 1 tema batch ("lock/biometric action feedback"), dalam batas micro-batch (2 dari maks 3).
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang di ke-2 file, grep konfirmasi `canUseBiometrics()` lama tidak tersentuh/masih dipanggil `LockScreen.kt` sama seperti sebelumnya).
- **Sisa Pending Queue**: P1.5 bagian Reminder (`NoteEditorScreen.kt`, set/hapus reminder masih senyap) — belum. Item baru "warning sebelum nonaktif PIN kalau ada note terkunci" (lihat poin di atas). P1.8 discoverability swipe actions, P1.9 checklist add/remove feedback, P2.10–2.13 visual polish. Prioritas next-batch disarankan: lanjutkan P1.5 Reminder di `NoteEditorScreen.kt` (melengkapi P1.5 penuh), ATAU tangani item warning-PIN baru di atas kalau user anggap itu lebih prioritas (ini menyentuh keamanan/data-access, bukan cuma UX).

## [v2_Batch13] — 2026-08-26
🔴 **FIX KEAMANAN KRITIS (P0), laporan urgent user**: "note yang ke gembok tetap bisa dimasuki tanpa password/biometrik sama sekali". Dikonfirmasi BENAR & serius — bukan cuma soal masking tampilan, tapi PIN/biometrik ulang tidak pernah benar2 dicek sebelum konten note terkunci dirender.
- **Root cause**: `NoteEditorScreen.kt` memuat note via `viewModel.getById(noteId)` lalu LANGSUNG merender title/content/checklist penuh begitu `loaded = true` — TIDAK ADA pengecekan `note.isLocked` sama sekali di titik ini. Field `isLocked` selama ini cuma dipakai utk MASKING tampilan di `NoteCard.kt` & `CalendarScreen.kt` (list/preview) — begitu note di-tap dan masuk ke editor, gerbang itu tidak ada. Infrastruktur auth (`AuthManager.verifyPin()`/`authenticateBiometric()`, `LockScreen` mode VERIFY — bahkan judul prompt biometriknya SUDAH bertuliskan "Verifikasi untuk membuka catatan terkunci") sudah lengkap dari Batch1, tapi tidak pernah benar2 dipanggil dari alur buka-note. Kemungkinan besar regresi tersembunyi sejak awal porting Kotlin (v1 Flutter TIDAK punya per-note lock re-auth gate ini juga secara eksplisit di audit — jadi ini bukan regresi porting, tapi gap yang memang belum pernah diimplementasi end-to-end di kedua versi).
- **Fix**: `NoteEditorScreen.kt` — tambah state `unlocked` (di-`remember(noteId)`, reset tiap buka note beda) + `val requiresUnlock = !isNew && note.isLocked`. Setelah note selesai dimuat (`loaded=true`) TAPI SEBELUM `Scaffold` konten dirender sama sekali: kalau `requiresUnlock && !unlocked` → tampilkan `LockScreen(mode = LockMode.VERIFY, ...)` (compose reuse langsung, 0 duplikasi kode keypad/biometric — `LockScreen` & `NoteEditorScreen` sama2 di package `ui.screens`, tidak perlu import baru) dan `return` (konten editor TIDAK PERNAH masuk composition tree sebelum verifikasi sukses — bukan cuma disembunyikan visual). `onResult` sukses → `unlocked = true`, lanjut render editor normal; gagal/dibatalkan → `onBack()` langsung (tidak ada apapun utk di-save krn konten tidak pernah ditampilkan). `BackHandler` disesuaikan: selama gerbang belum lolos, back = `onBack()` polos (bukan `saveAndExit()`, krn tidak ada state note yg "keliatan" utk dianggap berubah).
- Note BARU (`isNew=true`) TIDAK kena gerbang ini (toggle kunci saat compose note baru tetap bebas, sesuai desain awal — belum ada konten tersimpan utk dilindungi).
- **Edge-case diketahui & SENGAJA fail-safe (bukan bug baru)**: kalau PIN aplikasi sempat dihapus dari Settings (`auth.clearPin()`) SETELAH ada note yang di-lock, note itu jadi tidak bisa dibuka SAMA SEKALI lewat PIN manapun (`AuthManager.verifyPin()` selalu `false` tanpa PIN tersimpan) — locked-out permanen, BUKAN locked-open. Ini failure mode yang benar dari sisi keamanan (deny-by-default), meski UX-nya belum ideal (tidak ada pesan spesifik "PIN aplikasi belum diatur"). Dicatat sbg item polish terpisah, bukan bagian dari fix urgent ini.
- 1 file diubah (`NoteEditorScreen.kt`), 1 task (fix keamanan spesifik), jauh di bawah batas micro-batch (sengaja tidak refactor `LockScreen.kt` sama sekali — reuse langsung tanpa modifikasi, sesuai Zero-Unnecessary-Refactor, walau berisiko sedikit duplikasi konsep drpd extract shared composable baru).
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang, konfirmasi `LockScreen`/`LockMode` 1 package jadi tidak perlu import, konfirmasi satu2nya pemanggil `NoteEditorScreen()` di `NavGraph.kt` tidak perlu berubah krn signature tidak berubah). **PRIORITAS TES MANUAL DI DEVICE SEBELUM FITUR LAIN**: buka note yang sudah di-toggle kunci → wajib muncul layar PIN/biometrik dulu, tidak boleh langsung tampil isinya.

## [v2_Batch12] — 2026-08-26
Lanjut instruksi user "next" — ambil item prioritas tertinggi dari Pending Queue Batch11: **P1.5 Action Feedback**. Aksi utama app (Archive/Delete/Restore/Unarchive) sebelumnya 100% senyap — list berubah tanpa umpan balik visual apapun, user tidak yakin aksinya benar2 kepencet/berhasil.
- **`HomeScreen.kt`** (diubah): tambah `SnackbarHostState` + `rememberCoroutineScope()`, wired ke `Scaffold(snackbarHost = ...)`. 2 helper baru `archiveWithUndo(note)` / `deleteWithUndo(note)` — jalankan aksi ViewModel LANGSUNG (optimistic, tidak nunggu Snackbar), lalu tampilkan Snackbar "Catatan diarsipkan"/"Catatan dipindah ke sampah" + tombol "Urungkan" (`SnackbarResult.ActionPerformed` → panggil `unarchiveNote`/`restoreNote`). Dipakai di kedua cabang Grid & List (2 titik pemanggilan `NoteCard`, sebelumnya manggil `viewModel.archiveNote()`/`trashNote()` langsung tanpa umpan balik apapun).
- **`FilteredNotesScreen.kt`** (diubah): pola sama — `SnackbarHostState` + helper generik `showUndo(message, onUndo)`. Restore (Sampah→aktif) & Unarchive (Arsip→aktif) & "Hapus" dari Arsip (→Sampah, masih reversible) semua dapat Snackbar+Urungkan. **Hapus Permanen SENGAJA TANPA tombol Urungkan** (irreversible, konsisten dgn dialog konfirmasi Batch11) — cuma Snackbar info singkat "Catatan dihapus permanen" setelah eksekusi benar2 jalan dari dalam `confirmButton` dialog.
- **Keputusan desain**: Snackbar dipicu SETELAH aksi ViewModel dipanggil (bukan nunggu konfirmasi DB) — konsisten dgn pola optimistic-UI yang sudah dipakai di seluruh app (Room/Flow reaktif, operasi lokal SQLite praktis instan, tidak ada risiko network). Undo memanggil operasi KEBALIKAN yang sudah ada (`unarchiveNote`/`restoreNote`/`archiveNote`/`trashNote`) — tidak ada method baru di `NotesViewModel`/`NoteRepository`, jadi 0 risiko regresi ke layer data.
- 2 file diubah (`HomeScreen.kt`, `FilteredNotesScreen.kt`), 1 tema batch ("action feedback utk aksi list utama"), dalam batas micro-batch.
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang di ke-2 file, grep pastikan tidak ada pemanggil `archiveNote`/`trashNote`/`unarchiveNote`/`restoreNote` lama yg lolos tanpa lewat helper Snackbar).
- **Sisa Pending Queue polish (AUDIT_ISSUES.md UX/UI POLISH BACKLOG)**: P1.5 utk Lock/Reminder BELUM (toggle PIN/Biometrik di `SettingsScreen.kt` & set/hapus reminder di `NoteEditorScreen.kt` masih senyap — di luar scope batch ini, beda file). P1.8 discoverability swipe actions (hint visual "swipe utk arsip/hapus" bagi user baru — belum ada), P1.9 checklist add/remove feedback + perbesar target tombol hapus item, P2.10 typography hierarchy, P2.11 spacing/proporsi NoteCard Grid, P2.12 color/border treatment, P2.13 kurangi info developer-facing di footer Settings. Prioritas next-batch disarankan: lanjutkan P1.5 utk Lock (SettingsScreen.kt) — melengkapi cakupan action-feedback yg baru separuh jalan.

## [v2_Batch11] — 2026-08-26
Lanjut instruksi user "polish UI/UX sampai matang". Audit manual `ui/screens/*.kt` + `ui/components/NoteCard.kt` (bukan cuma baca AUDIT_ISSUES.md lama yg basis-nya Flutter — verifikasi ulang thd kode Kotlin aktual). Ditemukan 2 defek UX nyata yg cukup serius utk diprioritaskan drpd item polish kosmetik P2:
- **Bug #1 (severity tinggi) — Search bar HomeScreen efektif RUSAK TOTAL**: `OutlinedTextField` di `HomeScreen.kt` di-hardcode `value = ""` (tidak pernah baca balik `viewModel.searchQuery`), padahal `onValueChange` sudah benar manggil `setSearchQuery()`. Karena Compose text field terkontrol, efeknya: user ketik apapun → langsung "hilang" visual tiap recomposition (state internal ViewModel sebenarnya ke-update & filter jalan di background, tapi UI kelihatan kosong terus/tidak responsif — dari sudut pandang user, fitur pencarian tampak sama sekali tidak berfungsi).
- **Fix #1**: `HomeScreen.kt` — `value = searchQuery` (baca dari `viewModel.searchQuery.collectAsState()`, sudah ada StateFlow-nya dari awal, cuma belum di-collect di UI). Sekalian tambah tombol clear (X) muncul saat query tidak kosong (pola search bar iOS standar) + empty-state dipisah 2 varian: query kosong → pesan lebih actionable ("ketuk + di kanan atas untuk membuat catatan baru", sebelumnya cuma "Belum ada catatan" pasif), query tidak kosong tapi hasil kosong → "Tidak ada catatan yang cocok dengan ...".
- **Bug #2 (severity tinggi, risiko kehilangan data) — Hapus Permanen di tab Sampah TANPA konfirmasi apa pun**: `FilteredNotesScreen.kt` (mode TRASH) swipe "Hapus Permanen" via `NoteCard`/`SwipeToDismissBox` langsung panggil `viewModel.permanentDelete()` dalam `confirmValueChange` — 1 gesture swipe tidak sengaja = data hilang permanen, nol kesempatan batal. (Utk mode ARCHIVE, "Hapus" cuma pindah ke Sampah — reversible, sengaja TIDAK diberi konfirmasi, itu benar apa adanya.)
- **Fix #2**: `FilteredNotesScreen.kt` — swipe "Hapus Permanen" sekarang cuma set state `pendingPermanentDelete = note` (bukan delete langsung); `AlertDialog` konfirmasi baru muncul di luar `Scaffold` (pola sama dgn `UpdateDialog` Batch10) dgn tombol "Hapus Permanen" (merah) / "Batal". SwipeToDismissBox tetap snap-back visual (confirmValueChange tetap return `false`), dialog yg benar2 eksekusi delete. **Judul note terkunci tetap disamarkan di teks dialog** ("Catatan Terkunci", bukan judul asli) — konsisten dgn invariant masking yg sudah ditanam sejak Batch1 (regresi #5/#11 versi Flutter), jangan sampai kebocoran baru muncul lewat jalur UI baru manapun.
- 2 file diubah (`HomeScreen.kt`, `FilteredNotesScreen.kt`), 1 tema batch ("perbaikan defek UX kritis ditemukan saat audit polish"), dalam batas micro-batch (2 dari maks 3, sengaja tidak dipaksa ke-3 krn scope sudah lengkap & fokus).
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — sanity-check manual: kurung seimbang di ke-2 file, grep silang pastikan tidak ada pemanggil lama yang masih asumsikan search value kosong/delete langsung).
- **Pending queue polish lanjutan (dari AUDIT_ISSUES.md UX/UI POLISH BACKLOG, masih relevan konsepnya meski basis lama Flutter)**: P1.5 action feedback (toast/snackbar Archive/Delete/Restore/Reminder/Lock — saat ini aksi2 itu senyap, tidak ada umpan balik visual selain list berubah), P1.8 discoverability color picker & swipe actions (belum ada hint visual "swipe utk arsip/hapus" bagi user baru) + reminder butuh tampilan status/tanggal lebih informatif, P1.9 checklist feedback add/remove item + perbesar target tombol hapus item, P2.10 typography hierarchy, P2.11 spacing/proporsi NoteCard Grid, P2.12 color/border treatment, P2.13 kurangi info developer-facing di footer Settings (saat ini masih tampilkan path log internal "Documents/Jotter/logs" ke user awam). Prioritas next-batch disarankan: P1.5 (action feedback) — paling terasa dampaknya krn menyentuh hampir semua aksi utama app.

## [v2_Batch10] — 2026-08-26
Lanjut in-app updater sesuai pending queue Batch9: **UI wiring** (3 file — persis batas micro-batch, sesuai perkiraan Batch9), fitur "in-app updater" sekarang lengkap end-to-end (infra Batch8 → logic Batch9 → UI Batch10).
- **`SettingsViewModel.kt`** (diubah): tambah `UpdaterUiState` (sealed class: `Idle`/`Checking`/`UpToDate`/`Available`/`NoMatchingAsset`/`Downloading`/`ReadyToInstall`/`CheckError`/`DownloadError`) + `StateFlow<UpdaterUiState>` baru. Fungsi baru: `checkForUpdate()` (no-op kalau sedang Checking/Downloading — cegah double-tap), `startDownload(asset, tagName)` (progres `ReleaseDownloader` dipost langsung ke StateFlow dari thread IO — aman krn `MutableStateFlow.value` thread-safe, TAPI dithrottle per persen bulat, bukan tiap chunk 8KB, biar recomposition gak banjir untuk APK puluhan MB), `markInstalled(tagName)` (panggil `UpdateChecker.markTagAsInstalled()`), `dismissUpdaterDialog()` (diabaikan selama Downloading — dialog progres non-dismissable).
- **`UpdateDialog.kt`** (baru, `ui/components/`): dialog rounded/minimal (cupertino-look) yang render 1 `AlertDialog` sesuai `UpdaterUiState` — Available (info versi+ukuran+tombol unduh), NoMatchingAsset, Downloading (progress bar tak-bisa-ditutup + persen+ukuran, fallback indeterminate spinner kalau `totalBytes` belum diketahui), ReadyToInstall (tombol Pasang), UpToDate, CheckError, DownloadError. Idle/Checking sengaja tidak render apapun (Checking ditampilkan inline, bukan dialog).
- **`SettingsScreen.kt`** (diubah): section baru "PEMBARUAN APLIKASI" — row "Cek Pembaruan" (tampilkan versi terpasang dari `PackageManager`, trailing spinner kecil saat Checking, klik → `viewModel.checkForUpdate()`). `UpdateDialog` dipanggil di luar `Scaffold` (overlay), `onInstall` membangun `Intent(ACTION_VIEW)` + `FileProvider.getUriForFile()` (authority `${applicationId}.fileprovider`, sudah didaftarkan Batch8) + `FLAG_GRANT_READ_URI_PERMISSION` + `FLAG_ACTIVITY_NEW_TASK`, `startActivity()`, lalu `viewModel.markInstalled(tagName)`.
- **Keputusan desain dicatat**: `markInstalled()` dipanggil setelah `startActivity()` intent instal TIDAK THROW (install intent sukses DIMULAI) — bukan menunggu konfirmasi instalasi benar2 selesai dari OS (butuh `BroadcastReceiver` baru + touch `AndroidManifest.xml` di luar scope batch UI-only ini; kalau nanti dibutuhkan presisi 100%, ajukan sbg task terpisah).
- 3 file diubah/baru (`SettingsViewModel.kt`, `UpdateDialog.kt` baru, `SettingsScreen.kt`), 1 task, sesuai batas micro-batch (persis 3 sesuai perkiraan Batch9).
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK — cuma sanity-check manual: kurung kurawal/kurung biasa seimbang di ke-3 file, grep silang pastikan tidak ada pemanggil lama/bentrok nama). Cek utama saat CI: `LinearProgressIndicator(progress = { ... })` (API lambda M3, bukan varian `Float` yang sudah deprecated) match versi compose-bom `2025.06.00` yang dipakai project; `ListItem` `supportingContent` parameter sudah tersedia stabil di M3 versi ini (dipakai pola sama seperti composable lain di project).
- **Fitur "in-app updater" (Batch8→9→10) kini LENGKAP end-to-end secara kode.** Pending: verifikasi run CI + tes manual di device fisik (cek update dari rilis lama → baru, konfirmasi dialog+progress+install prompt muncul benar).

## [v2_Batch9] — 2026-08-25 (TERBARU)
Lanjut in-app updater sesuai rencana Batch8: **logic downloader** (2 file baru, package `com.jotter.notes.updater`), belum wiring UI (Batch10, masih pending).
- **`UpdateChecker.kt`** (baru): hit GitHub Releases API (`GET /repos/FDzaki-dev/jotter/releases/latest`) pakai OkHttp, timeout 15s + `followRedirects(true)`. Parse `tag_name` + daftar aset `.apk` pakai `org.json` bawaan Android (gak nambah dependency JSON baru). Pilih aset sesuai ABI device lewat `Build.SUPPORTED_ABIS` (urut prioritas device, cocokkan substring nama file — pola CI `app-<abi>-release.apk`). Bandingkan `tag_name` vs tag terakhir sukses-install (SharedPreferences `jotter_updater_prefs`) pakai **equality**, bukan ordering — sesuai keputusan desain Batch8. Expose `markTagAsInstalled(tag)` untuk dipanggil UI (Batch10) setelah install intent selesai — sengaja TIDAK otomatis dipanggil setelah download saja, supaya "sukses install" beneran dikonfirmasi alurnya di Batch10, bukan diasumsikan dari sisi downloader.
- **`ReleaseDownloader.kt`** (baru): download APK ke `cacheDir/updates/` (folder yang sama sudah diexpose FileProvider di Batch8). WAJIB streaming chunk-by-chunk pakai Okio (`source.read(sink.buffer, 8KB) + emitCompleteSegments()` loop) — bukan `body.bytes()`/`readBytes()` penuh ke RAM, sesuai Feature Lock Anti-OOM. Timeout eksplisit (connect 15s/read+write 30s) + `followRedirects(true)`. Callback progres `(bytesRead, totalBytes)` dipanggil dari thread IO — caller (ViewModel, Batch10) yang WAJIB post ke state UI dengan cara thread-safe. Cache lama di `updates/` dibersihkan tiap mulai download baru biar gak numpuk.
- 2 file kode diubah (baru), sesuai batas micro-batch (masih ada 1 slot tersisa tapi sengaja gak dipakai — Batch10 UI wiring butuh fokus penuh 3 file terpisah: kemungkinan `SettingsScreen.kt` + `SettingsViewModel.kt` + mungkin 1 komponen dialog baru).
- Belum diverifikasi CI/compile (sandbox tanpa Android SDK/OkHttp jar — cuma sanity-check sintaks manual: kurung kurawal/kurung biasa seimbang). Cek utama saat CI: import OkHttp/Okio Batch8 sudah benar match API yang dipakai (`OkHttpClient.Builder`, `Request.Builder`, `body.source()`, `File.sink().buffer()`).
- Pending queue: **Batch10** — wiring UI di `SettingsScreen.kt` (tombol "Cek Pembaruan" + progress bar + versi terpasang) + trigger install intent (`ACTION_VIEW` + FileProvider URI + `FLAG_GRANT_READ_URI_PERMISSION`) + panggil `markTagAsInstalled()` di titik yang tepat.

## [v2_Batch8] — 2026-08-25
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
