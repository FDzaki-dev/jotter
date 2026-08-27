# MAIN_GOALS — Jotter
Update terakhir: v2_Batch1 (2026-08-25) — RESET status verifikasi karena rewrite total ke Native Kotlin. Status "terverifikasi" versi Flutter TIDAK otomatis berlaku di sini (kode beda total, biar pun fiturnya sama).

Legenda: ✅ Terverifikasi di HP | 🔧 Diperbaiki/ditulis ulang, belum dikonfirmasi | ❓ Terimplementasi, belum pernah dites | ⚠️ Deviasi diketahui

## CORE FEATURES
| Goal | Status | Catatan |
|---|---|---|
| Teks & Checklist note | ❓ | Ditulis ulang native, belum pernah dites di HP |
| 9 warna kategori | ❓ | Hex asli iOS system color, belum dites |
| Kalender + reminder | ❓ | Grid hand-rolled + AlarmManager native, belum dites. Boot receiver reminder belum lengkap |
| Sort + search | ❓ | Belum dites |
| Archive & Trash | ❓ | Belum dites |
| Lock — PIN | ❓ | Ditulis ulang (EncryptedSharedPreferences), status ✅ versi Flutter TIDAK carry over otomatis |
| Lock — Biometric | ❓ | Ditulis ulang pakai BiometricPrompt asli (bukan lewat plugin) - secara arsitektur harusnya lebih reliable, tapi tetap belum dites |
| Grid/List toggle | ❓ | Belum dites |
| In-app updater (cek/unduh/pasang) | ❓ | Kode lengkap end-to-end (infra Batch8 → logic Batch9 → UI Batch10), belum pernah dites di HP |

## UI/UX (iOS template, versi Compose)
| Goal | Status | Catatan |
|---|---|---|
| Rounded corner card | 🔧 | Diimplementasi (RoundedCornerShape 16dp) |
| Large Title collapse | ❓ | Material3 LargeTopAppBar, belum discroll-test |
| Swipe gestures | 🔧 | Material3 SwipeToDismissBox (native, ganti dari flutter_slidable) |
| **Back gesture & tombol** | 🔧 | **INI YANG DIPERBAIKI DI BATCH INI** - Navigation Compose + BackHandler (OnBackPressedDispatcher asli), root cause bug Flutter (#138624) tidak relevan lagi di arsitektur ini |
| Font SF-style | ⚠️ | Tetap Roboto (sistem default), sama seperti sebelumnya |

## Rekomendasi tes berikutnya (prioritas: fitur yang jadi alasan pivot)
1. **Back gesture** (swipe dari tepi) di layar note editor
2. **Tombol back** (ikon panah pojok kiri) di layar note editor
3. Baru lanjut ke fitur lain (note, checklist, warna, dst) kalau #1 dan #2 sudah beneran jalan

## Referensi Kompetitor — ColorNote & Google Keep
_Digabung permanen 2026-08-28 dari 2 file riset user (`fitur_unggulan_colornote.md` + `perbandingan_colornote_google_keep.md` — isi kedua identik, file kedua cuma tambah tabel perbandingan; digabung jadi 1 section, file asal tidak disimpan terpisah lagi)._

ColorNote = acuan desain awal Jotter (warna kategori, checklist, kalender+reminder — lihat juga catatan treatment kartu warna di riwayat batch). Pilar fitur ColorNote vs status di Jotter:
- **Warna kategori + filter** → Jotter ✅ ada (9 warna)
- **Text note + checklist tanpa batas** → Jotter ✅ ada
- **Kalender + reminder waktu, reminder harian, pin ke status bar** → Jotter ✅ kalender+reminder waktu ada; **pin ke status bar TIDAK ada** (belum jadi goal)
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
