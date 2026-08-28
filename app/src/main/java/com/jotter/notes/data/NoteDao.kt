package com.jotter.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = :archived AND isDeleted = :deleted")
    fun observe(archived: Boolean, deleted: Boolean): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND reminderAt IS NOT NULL")
    fun observeWithReminders(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("UPDATE notes SET isArchived = :value, modifiedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, value: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = :value, modifiedAt = :now WHERE id = :id")
    suspend fun setDeleted(id: String, value: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isLocked = :value WHERE id = :id")
    suspend fun setLocked(id: String, value: Boolean)

    @Query("UPDATE notes SET isPinned = :value WHERE id = :id")
    suspend fun setPinned(id: String, value: Boolean)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun permanentDelete(id: String)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): Note?
}
