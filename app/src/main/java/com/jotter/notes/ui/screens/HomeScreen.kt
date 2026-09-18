package com.jotter.notes.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotter.notes.backup.BackupFileInfo
import com.jotter.notes.backup.BackupManager
import com.jotter.notes.backup.RestoreResult
import com.jotter.notes.data.Note
import com.jotter.notes.data.SortMode
import com.jotter.notes.ui.components.CardDensity
import com.jotter.notes.ui.components.NoteCard
import com.jotter.notes.ui.theme.OpaqueSurfaceContainer
import com.jotter.notes.viewmodel.NotesViewModel
import com.jotter.notes.viewmodel.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Sengaja top-level (bukan `remember`/ViewModel) — deteksi restore cuma boleh jalan SEKALI per
 * proses app (bukan tiap kali HomeScreen di-compose ulang pas navigasi balik dari layar lain),
 * biar gak nge-nag user berulang kali kalau mereka pilih "Nanti" tapi belum sempat bikin catatan
 * baru. Reset otomatis tiap proses app baru (cold start) — itu memang semantik yang diinginkan. */
private var restoreCheckDoneThisProcess = false

// v2_Batch57: FIX bug nyata "background text" - `ModalBottomSheet` ("Urutkan"/"Tampilan") gak
// pernah di-set `containerColor`, jatuh ke token M3 transparan (lihat riwayat Batch56-57 di
// PROJECT_STATE.md). v2_Batch61: constant-nya (`OpaqueSheetContainer`) dikonsolidasi ke
// `OpaqueSurfaceContainer` di Color.kt (nilai sama, 0xFF242426, dipakai bareng UpdateDialog.kt).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NotesViewModel = viewModel(),
    onOpenNote: (String?) -> Unit
) {
    val notes by viewModel.activeNotes.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    // v2_Batch54: dulu toggle langsung (1 tap = ganti ke mode lawan, cuma 2 kemungkinan). Sekarang
    // 4 mode (parity video showcase ColorNote "Lihat": Daftar/Detail/Petak/Petak Besar) gak muat
    // di pola toggle sederhana - butuh sheet pilihan, pola PERSIS sama dgn showSortSheet di atas.
    var showViewSheet by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Discoverability (sisa P1.8): swipe kiri/kanan di NoteCard utk arsip/hapus selama ini
    // TANPA petunjuk visual apapun - user baru gak bakal nemu sendiri. Dismiss PERMANEN
    // (SharedPreferences, bukan per-sesi) biar gak ganggu user yang udah tau caranya.
    val context = LocalContext.current
    val uiPrefs = remember { context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE) }
    var showSwipeHint by remember { mutableStateOf(!uiPrefs.getBoolean("swipe_hint_dismissed", false)) }

    // Deteksi kandidat "app baru di-uninstall+instal ulang / DB corrupt" (Batch39/40 slice 2/2)
    // — Documents/<App>/backup TIDAK ikut kehapus pas uninstall (di luar sandbox app), beda dari
    // Room DB yang lenyap total. isEmpty() cek TOTAL notes tanpa filter (bukan activeNotes yg
    // bisa 0 padahal ada notes lain di arsip/sampah - itu false-positive).
    var restoreSuggestion by remember { mutableStateOf<BackupFileInfo?>(null) }
    LaunchedEffect(Unit) {
        if (restoreCheckDoneThisProcess) return@LaunchedEffect
        restoreCheckDoneThisProcess = true
        if (viewModel.isEmpty()) {
            val info = withContext(Dispatchers.IO) { BackupManager.findLatestBackup(context, "Jotter") }
            if (info != null) restoreSuggestion = info
        }
    }

    // Aksi Archive/Delete dari swipe sebelumnya senyap total (list berubah, tanpa umpan balik) —
    // sekarang tiap aksi tampilkan Snackbar + tombol "Urungkan" (reversible, konsisten dgn
    // semantik archiveNote/trashNote yang memang bukan operasi permanen).
    fun archiveWithUndo(note: Note) {
        viewModel.archiveNote(note.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Catatan diarsipkan",
                actionLabel = "Urungkan",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.unarchiveNote(note.id)
        }
    }

    fun deleteWithUndo(note: Note) {
        viewModel.trashNote(note.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Catatan dipindah ke sampah",
                actionLabel = "Urungkan",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreNote(note.id)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text("Catatan") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { showSortSheet = true }) { Icon(Icons.Default.Sort, "Urutkan") }
                    IconButton(onClick = { showViewSheet = true }) {
                        Icon(viewModeIcon(viewMode), "Tampilan")
                    }
                    IconButton(onClick = { onOpenNote(null) }) { Icon(Icons.Default.Add, "Tambah") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Cari catatan") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, "Hapus pencarian")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (showSwipeHint && notes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Geser kartu ke kanan untuk arsip, ke kiri untuk hapus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        showSwipeHint = false
                        uiPrefs.edit().putBoolean("swipe_hint_dismissed", true).apply()
                    }) {
                        Icon(Icons.Default.Close, "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        if (searchQuery.isNotEmpty()) "Tidak ada catatan yang cocok dengan \"$searchQuery\""
                        else "Belum ada catatan · ketuk + di kanan atas untuk membuat catatan baru",
                        color = androidx.compose.ui.graphics.Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else if (viewMode == ViewMode.GRID || viewMode == ViewMode.GRID_LARGE) {
                // v2_Batch55: FIX laporan user - video showcase ke-2 (hasil nyata mode "Petak"
                // ColorNote, bukan cuma menu-nya) confirm 3 KOLOM, bukan 2 spt asumsi Batch54.
                // "Petak Besar" = 2 kolom (tile lebih besar/lebar) - penamaan "besar" = kartu lebih
                // besar, bukan lebih banyak baris teks dijejalkan ke kartu sempit (itu kesalahan
                // Batch54 yang bikin readability buruk).
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (viewMode == ViewMode.GRID_LARGE) 2 else 3),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onTap = { onOpenNote(note.id) },
                            onArchive = { archiveWithUndo(note) },
                            onDelete = { deleteWithUndo(note) },
                            density = if (viewMode == ViewMode.GRID_LARGE) CardDensity.GRID_LARGE else CardDensity.GRID
                        )
                    }
                }
            } else {
                // "Daftar" (LIST) & "Detail" - sama2 1 kolom, beda cuma COMPACT (0 preview) vs
                // DETAIL (preview penuh + tanggal, perilaku lama, TIDAK berubah dari Batch54).
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onTap = { onOpenNote(note.id) },
                            onArchive = { archiveWithUndo(note) },
                            onDelete = { deleteWithUndo(note) },
                            density = if (viewMode == ViewMode.LIST) CardDensity.COMPACT else CardDensity.DETAIL
                        )
                    }
                }
            }
        }
    }

    restoreSuggestion?.let { info ->
        AlertDialog(
            onDismissRequest = { restoreSuggestion = null },
            title = { Text("Pulihkan Catatan?") },
            text = {
                val dateStr = java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                    .format(java.util.Date(info.dateAddedSeconds * 1000L))
                Text("Jotter tidak menemukan catatan tersimpan di perangkat ini, tapi ada file backup dari $dateStr. Ini bisa terjadi kalau aplikasi baru saja dipasang ulang. Mau pulihkan catatan dari backup itu?")
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = info.uri
                    restoreSuggestion = null
                    scope.launch {
                        val result = BackupManager.restore(context, uri)
                        val message = when (result) {
                            is RestoreResult.Success -> {
                                val base = "Berhasil memulihkan ${result.restoredCount} catatan"
                                if (result.lockedPlaceholderCount > 0) {
                                    "$base (${result.lockedPlaceholderCount} di antaranya catatan terkunci — isinya placeholder, bukan konten asli)"
                                } else base
                            }
                            is RestoreResult.Error -> "Pulihkan gagal: ${result.message}"
                        }
                        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
                    }
                }) { Text("Pulihkan") }
            },
            dismissButton = {
                TextButton(onClick = { restoreSuggestion = null }) { Text("Nanti") }
            }
        )
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }, containerColor = OpaqueSurfaceContainer) {
            Column(Modifier.padding(16.dp)) {
                Text("Urutkan berdasarkan", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Waktu Diubah" to SortMode.MODIFIED,
                    "Waktu Dibuat" to SortMode.CREATED,
                    "Abjad" to SortMode.ALPHABETICAL,
                    "Warna" to SortMode.COLOR
                ).forEach { (label, mode) ->
                    val isActive = sortMode == mode
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = { if (isActive) Icon(Icons.Default.Check, "Aktif") },
                        modifier = Modifier.clickable {
                            viewModel.setSortMode(mode)
                            showSortSheet = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Diurutkan berdasarkan $label", duration = SnackbarDuration.Short)
                            }
                        }
                    )
                }
            }
        }
    }

    // v2_Batch54: sheet pemilih mode tampilan - pola identik showSortSheet di atas (Column+ListItem),
    // 4 opsi sesuai menu "Lihat" di video showcase ColorNote (Daftar/Detail/Petak/Petak Besar).
    if (showViewSheet) {
        ModalBottomSheet(onDismissRequest = { showViewSheet = false }, containerColor = OpaqueSurfaceContainer) {
            Column(Modifier.padding(16.dp)) {
                Text("Tampilan", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Daftar" to ViewMode.LIST,
                    "Detail" to ViewMode.DETAIL,
                    "Petak" to ViewMode.GRID,
                    "Petak Besar" to ViewMode.GRID_LARGE
                ).forEach { (label, mode) ->
                    val isActive = viewMode == mode
                    ListItem(
                        leadingContent = { Icon(viewModeIcon(mode), null) },
                        headlineContent = { Text(label) },
                        trailingContent = { if (isActive) Icon(Icons.Default.Check, "Aktif") },
                        modifier = Modifier.clickable {
                            viewModel.setViewMode(mode)
                            showViewSheet = false
                        }
                    )
                }
            }
        }
    }
}

/** v2_Batch54: icon representatif tiap mode - dipakai baik utk icon tombol app-bar (mode aktif)
 * maupun leading-icon tiap baris di sheet "Tampilan". ViewList/GridView sudah dipakai project ini
 * sebelumnya (toggle 2-mode lama, terverifikasi ada) - ViewHeadline & Apps murni ikon Material
 * standar lama (bukan tebakan versi/rename baru), reuse import wildcard `filled.*` yang sudah ada. */
// v2_Batch61: FIX temuan Batch58 - "Petak" (GRID, 3 kolom kecil) & "Petak Besar" (GRID_LARGE,
// 2 kolom besar) kepasang ikon kebalik (GridView = kesan 2x2 besar, Apps = kesan 3x3 kecil).
// Ditukar biar ikon match jumlah kolom sebenarnya.
private fun viewModeIcon(mode: ViewMode): androidx.compose.ui.graphics.vector.ImageVector = when (mode) {
    ViewMode.LIST -> Icons.Default.ViewHeadline
    ViewMode.DETAIL -> Icons.Default.ViewList
    ViewMode.GRID -> Icons.Default.Apps
    ViewMode.GRID_LARGE -> Icons.Default.GridView
}
