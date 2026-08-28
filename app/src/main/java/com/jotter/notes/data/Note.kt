package com.jotter.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class NoteType { TEXT, CHECKLIST }

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isChecked: Boolean = false
)

class Converters {
    @TypeConverter
    fun fromChecklist(items: List<ChecklistItem>): String {
        val arr = JSONArray()
        items.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("text", it.text)
            obj.put("isChecked", it.isChecked)
            arr.put(obj)
        }
        return arr.toString()
    }

    @TypeConverter
    fun toChecklist(data: String): List<ChecklistItem> {
        if (data.isBlank()) return emptyList()
        val arr = JSONArray(data)
        return (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            ChecklistItem(obj.getString("id"), obj.getString("text"), obj.getBoolean("isChecked"))
        }
    }

    @TypeConverter
    fun fromNoteType(type: NoteType): String = type.name

    @TypeConverter
    fun toNoteType(value: String): NoteType = if (value == "CHECKLIST") NoteType.CHECKLIST else NoteType.TEXT
}

@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var content: String = "",
    var type: NoteType = NoteType.TEXT,
    var checklistItems: List<ChecklistItem> = emptyList(),
    var colorIndex: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis(),
    var reminderAt: Long? = null,
    var isArchived: Boolean = false,
    var isDeleted: Boolean = false,
    var isLocked: Boolean = false,
    var isPinned: Boolean = false
) {
    fun searchableText(): String =
        (title + " " + content + " " + checklistItems.joinToString(" ") { it.text }).lowercase()
}
