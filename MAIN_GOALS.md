# MAIN_GOALS — Jotter
Update terakhir: v2_Batch47 (2026-08-28) — status disinkronkan ulang thd bukti nyata di riwayat batch (bukan cuma "ditulis ulang, belum dites" blanket dari v2_Batch1).

Legenda: ✅ Terverifikasi di HP | 🔧 Diperbaiki/ditulis ulang, belum dikonfirmasi | ❓ Terimplementasi, belum pernah dites | ⚠️ Deviasi diketahui

## CORE FEATURES
| Goal | Status | Catatan |
|---|---|---|
| Teks & Checklist note | ❓ | Editor aktif dipakai user sejak v2_Batch5 (note tampil di home screen, screenshot device) & berulang di batch2 lanjutan (lock-bypass Batch13, reminder-picker Batch29 — dua2nya ditemukan LEWAT buka/edit note beneran) — tapi belum ada konfirmasi eksplisit teks/checklist murni (ketik, edit, centang item) bebas bug |
| 9 warna kategori | ❓ | Hex asli iOS system color, belum dites |
| Kalender + reminder | 🔧 | Real device test nemu bug (sheet reminder gak bisa discroll, Batch29) — sudah difix, BELUM dikonfirmasi ulang pasca-fix |
| Sort + search | ❓ | Belum dites |
| Archive & Trash | ❓ | Belum dites |
| Lock — PIN | 🔧 | Real device test nemu bug KEAMANAN KRITIS (note terkunci bisa dibuka tanpa PIN sama sekali, Batch13) — sudah difix, BELUM ada konfirmasi eksplisit final pasca-fix |
| Lock — Biometric | 🔧 | Sama dgn Lock—PIN di atas (1 gerbang verifikasi yang sama) |
| Grid/List toggle | ❓ | Belum dites |
| In-app updater (cek/unduh/pasang) | ❓ | Kode lengkap end-to-end (infra Batch8 → logic Batch9 → UI Batch10), belum pernah dites di HP. Beda dgn "Lihat Rilis di GitHub" (shortcut browser, Batch28) — itu bukan alur auto-update-nya sendiri |
| Backup & Restore data | 🔧 | Siklus real device intensif Batch39-46: auto-detect restore (MediaStore) terbukti gak reliable di device user (OEM/XOS quirk, dikonfirmasi via tes terkontrol backup→hapus data→restore Batch44) → fallback SAF manual dibangun (Batch42) tapi sempat crash (fix Batch43) → dialog konfirmasi ditambah semua aksi backup/restore (Batch46). Backup Data (tulis) sendiri belum ada laporan gagal |
| **Pin catatan ke status bar** | 🔧 | Slice 1/3 (data layer) selesai v2_Batch36. Slice 2/3 (notification layer) & 3/3 (tombol pin di UI) BELUM dikerjakan — lihat Pending Queue `PROJECT_STATE.md` |

## UI/UX (iOS template, versi Compose)
| Goal | Status | Catatan |
|---|---|---|
| Rounded corner card | ✅ | Dikonfirmasi via screenshot device asli v2_Batch5: "dark theme + rounded card sesuai desain iOS-look" |
| Large Title collapse | ❓ | Material3 LargeTopAppBar, belum discroll-test |
| Swipe gestures | 🔧 | Material3 SwipeToDismissBox (native, ganti dari flutter_slidable) |
| Back gesture & tombol | 🔧 | Navigation Compose + BackHandler (OnBackPressedDispatcher asli) — root cause bug Flutter (#138624) tidak relevan lagi di arsitektur ini. Belum ada konfirmasi device eksplisit utk arsitektur Kotlin ini |
| Font SF-style | ⚠️ | Tetap Roboto (sistem default), sama seperti sebelumnya |

## Rekomendasi tes berikutnya (prioritas: fitur dgn fix terbaru, belum reconfirm)
1. **Backup & Restore** — coba "Pilih File Backup Manual" end-to-end (fix crash Batch43) + pastikan semua dialog konfirmasi baru muncul (Batch46)
2. **Settings** — scroll penuh sampai bawah (fix Batch45)
3. **Lock PIN/Biometric** — buka note terkunci, pastikan gerbang verifikasi beneran muncul tiap kali (fix keamanan kritis Batch13, belum reconfirm eksplisit)
4. **Reminder picker sheet** — pastikan bisa discroll penuh (fix Batch29, belum reconfirm eksplisit)
5. Baru lanjut fitur yang sama sekali belum dites (9 warna, sort/search, archive/trash, grid/list, in-app updater, pin status bar slice 2-3)

## Referensi Kompetitor — ColorNote & Google Keep
_Digabung permanen 2026-08-28 dari 2 file riset user (`fitur_unggulan_colornote.md` + `perbandingan_colornote_google_keep.md` — isi kedua identik, file kedua cuma tambah tabel perbandingan; digabung jadi 1 section, file asal tidak disimpan terpisah lagi)._

ColorNote = acuan desain awal Jotter (warna kategori, checklist, kalender+reminder — lihat juga catatan treatment kartu warna di riwayat batch). Pilar fitur ColorNote vs status di Jotter:
- **Warna kategori + filter** → Jotter ✅ ada (9 warna)
- **Text note + checklist tanpa batas** → Jotter ✅ ada
- **Kalender + reminder waktu, reminder harian, pin ke status bar** → Jotter ✅ kalender+reminder waktu ada; **pin ke status bar** kini 🔧 sedang dikerjakan (lihat tabel CORE FEATURES di atas — dipromosikan dari referensi jadi goal resmi mulai v2_Batch36)
- **Master Password + cloud backup/sync** → Jotter ✅ PIN/biometric ada; **cloud backup SENGAJA TIDAK ADA** (app 100% offline by design, lihat README)
- **Sticky widget, auto-link teks, aplikasi ringan** → widget home-screen & auto-link **belum jadi goal**; ringan sudah otomatis dari native Kotlin

### vs Google Keep (tabel riset — BUKAN backlog otomatis)
| Fitur | ColorNote | Google Keep | Status di Jotter |
|---|---|---|---|
| Kalender bawaan | ✅ | ❌ (lewat Google Calendar) | ✅ ada |
| Kunci catatan | ✅ Master Password | ❌ | ✅ PIN/biometric |
| Label/tag kata | ❌ (cuma warna) | ✅ label + warna | ❌ cuma warna |
| Kolaborasi real-time | ❌ | ✅ | ❌ di luar scope (offline-only) |
| Lampiran media (foto/suara/gambar tangan) | ❌ (teks+checklist saja) | ✅ | ❌ di luar scope saat ini |
| Lintas platform | Terbatas Android | Android/iOS/Web/Chrome | Android-only (native Kotlin) |
| Pengingat lokasi | ❌ (waktu saja) | ✅ | ❌ waktu saja |
| Bobot aplikasi | Sangat ringan | Lebih berat | Native Kotlin — ringan by design |

**Catatan**: tabel di atas murni referensi riset. Item manapun (mis. label/tag, lampiran media, pengingat lokasi) baru resmi jadi goal kalau user eksplisit minta ditambahkan ke tabel CORE FEATURES di atas — tidak otomatis masuk pending queue.
