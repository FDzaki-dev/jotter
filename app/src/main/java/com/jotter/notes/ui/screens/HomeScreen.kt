package com.jotter.notes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotter.notes.data.Note
import com.jotter.notes.data.SortMode
import com.jotter.notes.ui.components.NoteCard
import com.jotter.notes.viewmodel.NotesViewModel
import com.jotter.notes.viewmodel.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NotesViewModel = viewModel(),
    onOpenNote: (String?) -> Unit
) {
    val notes by viewModel.activeNotes.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                    IconButton(onClick = {
                        viewModel.setViewMode(if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
                    }) {
                        Icon(if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView, "Tampilan")
                    }
                    IconButton(onClick = { onOpenNote(null) }) { Icon(Icons.Default.Add, "Tambah") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
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
            } else if (viewMode == ViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onTap = { onOpenNote(note.id) },
                            onArchive = { archiveWithUndo(note) },
                            onDelete = { deleteWithUndo(note) }
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onTap = { onOpenNote(note.id) },
                            onArchive = { archiveWithUndo(note) },
                            onDelete = { deleteWithUndo(note) }
                        )
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("Urutkan berdasarkan", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Waktu Diubah" to SortMode.MODIFIED,
                    "Waktu Dibuat" to SortMode.CREATED,
                    "Abjad" to SortMode.ALPHABETICAL,
                    "Warna" to SortMode.COLOR
                ).forEach { (label, mode) ->
                    TextButton(onClick = { viewModel.setSortMode(mode); showSortSheet = false }) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
