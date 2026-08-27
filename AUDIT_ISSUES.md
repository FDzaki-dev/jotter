# AUDIT_ISSUES — Jotter
> 🗄️ **ARCHIVED (2026-08-28)** — Seluruh isi file ini historis, bukan backlog aktif. Section 1 = audit kode **Flutter v1** (`lib/**/*.dart`), sudah tidak ada sejak rewrite total ke Native Kotlin (v2, mulai v2_Batch1). Section 2 = UX/UI Polish Backlog carry-over ke Kotlin — **100% CLOSED sejak v2_Batch27**. Status fitur terkini → `MAIN_GOALS.md`. Task/pending queue aktif → `PROJECT_STATE.md`.

---

## Section 1 — Audit Flutter v1 (2026-08-24, basis v1_Batch9) — kode sudah tidak ada
12 temuan dari static review `lib/**/*.dart` era Flutter. Semua Critical + sebagian High/Medium sempat di-fix DI KODE FLUTTER (v1_Batch11-18) sebelum rewrite total ke Kotlin — preseden desainnya ikut terbawa ke v2, tapi sudah diverifikasi ulang independen di kode Kotlin (lihat riwayat v2_BatchX di `PROJECT_STATE.md`), bukan carry-over kode.

| # | Severity | Ringkasan | Status era Flutter (v1) |
|---|---|---|---|
| 1 | 🔴 Critical | Notifikasi reminder bocorkan isi note terkunci | ✅ RESOLVED v1_Batch11 |
| 2 | 🔴 Critical | Reminder tak dibatalkan saat note dihapus | ✅ RESOLVED v1_Batch12 |
| 3 | 🔴 Critical | Reminder hilang setelah restart HP | ✅ RESOLVED* v1_Batch13 (reschedule-on-open, bukan native boot receiver) |
| 4 | 🔴 Critical | Notification ID dari hashCode berisiko out-of-range | ✅ RESOLVED v1_Batch14 |
| 5 | 🟠 High | Judul note terkunci tidak tersamarkan | ✅ RESOLVED v1_Batch15 |
| 6 | 🟠 High | Tab Kalender tidak reaktif (data basi) | ✅ RESOLVED v1_Batch17 |
| 7 | 🟠 High | `modifiedAt` berubah tanpa edit (no dirty-check) | Belum di-fix di Flutter — jadi P1.4 di Section 2, resolved di v2 |
| 8 | 🟡 Medium | `TextEditingController` tak pernah `dispose()` | Belum di-fix di Flutter (moot — widget ini gak ada lagi di v2 Compose) |
| 9 | 🟡 Medium | Toggle Biometrik gagal senyap tanpa pesan error | ✅ RESOLVED v1_Batch18 |
| 10 | 🟡 Medium | Potensi crash use-after-dispose (edge-case) | Belum di-fix di Flutter (moot — lifecycle Compose beda total) |
| 11 | ⚪ Low | README manual-build gak sinkron CI (fat APK vs split-per-abi) | Dok-only, moot (README v2 sudah akurat thd `release.yml` terkini) |
| 12 | ⚪ Low | Stale-run-guard diletakkan setelah step build (boros menit CI) | Trade-off yang sama juga ada di `release.yml` v2 — diketahui & diterima sejak awal, bukan bug |

## Section 2 — UX/UI Polish Backlog (verdict eksternal, ditanam v1_Batch16) — 100% CLOSED
Constraint asli: surgical micro-fix only, dilarang refactor besar. Carry-over konsep ke v2 Kotlin, dikerjakan v2_Batch20–27.

- **P0 (Logic/Keamanan)** — 3/3 closed: dirty-state editor, Calendar sync (= High #6), Lock/Biometric feedback (= Medium #9).
- **P1 (UX)** — 6/6 closed: action feedback toast/snackbar, konfirmasi permanent-delete, empty states, discoverability color-picker & swipe, checklist add/remove feedback.
- **P2 (Polish)** — 4/4 closed: typography hierarchy, spacing/proportion NoteCard, color/border treatment, footer Settings bersih dari info developer-facing.

Detail implementasi tiap item: lihat entri `v2_Batch20` s/d `v2_Batch27` di `PROJECT_STATE.md`.
