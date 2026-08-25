package com.jotter.notes.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jotter.notes.data.Note
import com.jotter.notes.data.NoteType

object ReminderScheduler {
    fun schedule(context: Context, note: Note) {
        val reminderAt = note.reminderAt ?: return
        if (reminderAt < System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteId", note.id)
            if (note.isLocked) {
                putExtra("title", "Jotter")
                putExtra("body", "Catatan terkunci memiliki pengingat")
            } else {
                putExtra("title", note.title.ifEmpty { "Jotter" })
                putExtra("body", if (note.type == NoteType.CHECKLIST) "Anda memiliki checklist yang perlu diselesaikan" else note.content)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, note.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent)
        } catch (e: SecurityException) {
            // exact alarm permission not granted - fall back to inexact
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent)
        }
    }

    fun cancel(context: Context, noteId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, noteId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
