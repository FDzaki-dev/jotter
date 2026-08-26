package com.jotter.notes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotter.notes.data.Note
import com.jotter.notes.ui.components.NoteCard
import com.jotter.notes.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

enum class FilteredMode { ARCHIVE, TRASH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredNotesScreen(
    mode: FilteredMode,
    viewModel: NotesViewModel = viewModel(),
    onOpenNote: (String) -> Unit,
    onBack: () -> Unit
) {
    val isArchive = mode == FilteredMode.ARCHIVE
    val notes by (if (isArchive) viewModel.archivedNotes else viewModel.trashedNotes).collectAsState()
    var pendingPermanentDelete by remember { mutableStateOf<Note?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Umpan balik Snackbar utk aksi Restore/Unarchive/Hapus(→sampah) — sebelumnya senyap total.
    // Tombol "Urungkan" hanya utk aksi reversible; hapus permanen (di bawah) sengaja TANPA undo.
    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Urungkan",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isArchive) "Arsip" else "Sampah") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") } }
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (isArchive) "Arsip kosong" else "Sampah kosong", color = Color.Gray)
            }
        } else {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        archiveLabel = if (isArchive) "Buka Arsip" else "Pulihkan",
                        archiveIcon = if (isArchive) Icons.Default.Unarchive else Icons.Default.RestoreFromTrash,
                        archiveColor = if (isArchive) Color(0xFF007AFF) else Color(0xFF34C759),
                        deleteLabel = if (isArchive) "Hapus" else "Hapus Permanen",
                        onTap = { onOpenNote(note.id) },
                        onArchive = {
                            if (isArchive) {
                                viewModel.unarchiveNote(note.id)
                                showUndo("Catatan dikeluarkan dari arsip") { viewModel.archiveNote(note.id) }
                            } else {
                                viewModel.restoreNote(note.id)
                                showUndo("Catatan dipulihkan") { viewModel.trashNote(note.id) }
                            }
                        },
                        onDelete = {
                            if (isArchive) {
                                viewModel.trashNote(note.id)
                                showUndo("Catatan dipindah ke sampah") { viewModel.restoreNote(note.id) }
                            } else {
                                pendingPermanentDelete = note
                            }
                        }
                    )
                }
            }
        }
    }

    // Hapus permanen (Sampah) tidak bisa dibatalkan — WAJIB konfirmasi eksplisit dulu, tidak
    // boleh langsung jalan dari gesture swipe. Judul note terkunci tetap disamarkan di sini,
    // konsisten dgn masking di NoteCard/CalendarScreen (tidak membocorkan isi lewat dialog).
    pendingPermanentDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text("Hapus Permanen?") },
            text = {
                val displayTitle = if (note.isLocked) "Catatan Terkunci" else note.title.ifBlank { "Catatan tanpa judul" }
                Text("\"$displayTitle\" akan dihapus permanen dan tidak bisa dipulihkan lagi.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.permanentDelete(note.id)
                    pendingPermanentDelete = null
                    scope.launch { snackbarHostState.showSnackbar("Catatan dihapus permanen", duration = SnackbarDuration.Short) }
                }) { Text("Hapus Permanen", color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentDelete = null }) { Text("Batal") }
            }
        )
    }
}
