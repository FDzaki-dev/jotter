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
