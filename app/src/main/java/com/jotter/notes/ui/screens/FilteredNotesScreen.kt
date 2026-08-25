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
import com.jotter.notes.ui.components.NoteCard
import com.jotter.notes.viewmodel.NotesViewModel

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

    Scaffold(
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
                        onArchive = { if (isArchive) viewModel.unarchiveNote(note.id) else viewModel.restoreNote(note.id) },
                        onDelete = { if (isArchive) viewModel.trashNote(note.id) else viewModel.permanentDelete(note.id) }
                    )
                }
            }
        }
    }
}
