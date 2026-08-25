package com.jotter.notes.data

import kotlinx.coroutines.flow.Flow

enum class SortMode { MODIFIED, CREATED, ALPHABETICAL, COLOR }

class NoteRepository(private val dao: NoteDao) {

    fun observeActive(): Flow<List<Note>> = dao.observe(archived = false, deleted = false)
    fun observeArchived(): Flow<List<Note>> = dao.observe(archived = true, deleted = false)
    fun observeTrashed(): Flow<List<Note>> = dao.observe(archived = false, deleted = true)
    fun observeWithReminders(): Flow<List<Note>> = dao.observeWithReminders()

    fun sortAndFilter(notes: List<Note>, query: String, sortMode: SortMode): List<Note> {
        var result = notes
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.searchableText().contains(q) }
        }
        result = when (sortMode) {
            SortMode.MODIFIED -> result.sortedByDescending { it.modifiedAt }
            SortMode.CREATED -> result.sortedByDescending { it.createdAt }
            SortMode.ALPHABETICAL -> result.sortedBy { it.title.lowercase() }
            SortMode.COLOR -> result.sortedBy { it.colorIndex }
        }
        return result
    }

    suspend fun save(note: Note) {
        note.modifiedAt = System.currentTimeMillis()
        dao.upsert(note)
    }

    suspend fun archive(id: String) = dao.setArchived(id, true)
    suspend fun unarchive(id: String) = dao.setArchived(id, false)
    suspend fun trash(id: String) = dao.setDeleted(id, true)
    suspend fun restore(id: String) = dao.setDeleted(id, false)
    suspend fun permanentDelete(id: String) = dao.permanentDelete(id)
    suspend fun setLocked(id: String, value: Boolean) = dao.setLocked(id, value)
    suspend fun getById(id: String): Note? = dao.getById(id)
}
