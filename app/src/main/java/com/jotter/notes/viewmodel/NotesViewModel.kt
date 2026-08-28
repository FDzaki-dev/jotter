package com.jotter.notes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jotter.notes.data.AppDatabase
import com.jotter.notes.data.Note
import com.jotter.notes.data.NoteRepository
import com.jotter.notes.data.SortMode
import com.jotter.notes.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

enum class ViewMode { LIST, GRID }

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NoteRepository(AppDatabase.getInstance(application).noteDao())

    val sortMode = MutableStateFlow(SortMode.MODIFIED)
    val viewMode = MutableStateFlow(ViewMode.GRID)
    val searchQuery = MutableStateFlow("")

    val activeNotes: StateFlow<List<Note>> = combine(
        repo.observeActive(), searchQuery, sortMode
    ) { notes, query, sort -> repo.sortAndFilter(notes, query, sort) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = combine(
        repo.observeArchived(), searchQuery, sortMode
    ) { notes, query, sort -> repo.sortAndFilter(notes, query, sort) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = combine(
        repo.observeTrashed(), searchQuery, sortMode
    ) { notes, query, sort -> repo.sortAndFilter(notes, query, sort) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminderNotes: StateFlow<List<Note>> = repo.observeWithReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortMode(mode: SortMode) { sortMode.value = mode }
    fun setViewMode(mode: ViewMode) { viewMode.value = mode }
    fun setSearchQuery(q: String) { searchQuery.value = q }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            repo.save(note)
            val ctx = getApplication<Application>()
            if (note.reminderAt != null) ReminderScheduler.schedule(ctx, note)
            else ReminderScheduler.cancel(ctx, note.id)
        }
    }

    fun archiveNote(id: String) = viewModelScope.launch { repo.archive(id) }
    fun unarchiveNote(id: String) = viewModelScope.launch { repo.unarchive(id) }
    fun trashNote(id: String) = viewModelScope.launch {
        repo.trash(id)
        ReminderScheduler.cancel(getApplication(), id)
    }
    fun restoreNote(id: String) = viewModelScope.launch { repo.restore(id) }
    fun permanentDelete(id: String) = viewModelScope.launch {
        repo.permanentDelete(id)
        ReminderScheduler.cancel(getApplication(), id)
    }
    fun setLocked(id: String, value: Boolean) = viewModelScope.launch { repo.setLocked(id, value) }
    suspend fun getById(id: String): Note? = repo.getById(id)
    /** Dipakai HomeScreen sekali per app-open utk deteksi kandidat "app baru di-uninstall+instal
     * ulang / DB corrupt" (lihat backup/BackupManager.kt) — 0 filter, beda dari activeNotes. */
    suspend fun isEmpty(): Boolean = repo.count() == 0
}
