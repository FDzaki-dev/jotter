package com.jotter.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jotter.notes.data.Note
import com.jotter.notes.data.NoteType
import com.jotter.notes.ui.theme.JotterSurface
import com.jotter.notes.ui.theme.noteColorFor
import java.util.Calendar

// Native Compose swipe-to-reveal-actions - real gesture handling via SwipeToDismissBox,
// no third-party plugin indirection (this replaces the flaky flutter_slidable approach).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    onTap: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    archiveLabel: String = "Arsip",
    archiveIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Archive,
    archiveColor: Color = Color(0xFFFF9500),
    deleteLabel: String = "Hapus",
    deleteIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Delete,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                SwipeToDismissBoxValue.StartToEnd -> { onArchive(); false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val (bg, icon, label, alignment) = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> listOf(archiveColor, archiveIcon, archiveLabel, Alignment.CenterStart)
                SwipeToDismissBoxValue.EndToStart -> listOf(Color(0xFFFF3B30), deleteIcon, deleteLabel, Alignment.CenterEnd)
                else -> listOf(Color.Transparent, Icons.Default.MoreHoriz, "", Alignment.Center)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg as Color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment as Alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, contentDescription = label as String, tint = Color.White)
                }
            }
        }
    ) {
        NoteCardContent(note = note, onTap = onTap)
    }
}

@Composable
private fun NoteCardContent(note: Note, onTap: () -> Unit) {
    val borderColor = noteColorFor(note.colorIndex)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JotterSurface)
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(16.dp))
            .clickable(onClick = onTap)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(borderColor))
            Spacer(Modifier.width(6.dp))
            if (note.isLocked) Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            Spacer(Modifier.weight(1f))
            note.reminderAt?.let { reminderAt ->
                if (note.isLocked) {
                    // Note terkunci: cukup tunjukkan ADA pengingat (perilaku lama, sudah aman),
                    // tapi JANGAN tampilkan tanggal/jam spesifik — itu metadata baru yang bisa
                    // bocorkan konteks note terkunci, melanggar invariant masking (Batch1/11/13).
                    Icon(Icons.Default.Notifications, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                } else {
                    val isOverdue = reminderAt < System.currentTimeMillis()
                    val reminderColor = if (isOverdue) Color(0xFFFF3B30) else Color.Gray
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = reminderColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(formatReminderBadge(reminderAt), color = reminderColor, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        if (note.title.isNotEmpty() && !note.isLocked) {
            Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        } else if (note.isLocked) {
            Text("Catatan Terkunci", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        }
        Spacer(Modifier.height(4.dp))
        when {
            note.isLocked -> Text("•••••••", color = Color.Gray)
            note.type == NoteType.CHECKLIST -> Column {
                note.checklistItems.take(3).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            null, tint = Color.Gray, modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(item.text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray)
                    }
                }
            }
            else -> Text(note.content, maxLines = 4, overflow = TextOverflow.Ellipsis, color = Color.Gray)
        }
    }
}

/** "14:30" kalau hari ini, "26 Agu 14:30" kalau bukan — biar badge di kartu sempit (grid 2 kolom) tetap ringkas. */
private fun formatReminderBadge(reminderAt: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = reminderAt }
    val now = Calendar.getInstance()
    val timeStr = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    if (isToday) return timeStr
    val dateFmt = java.text.SimpleDateFormat("d MMM", java.util.Locale("id", "ID"))
    return "${dateFmt.format(cal.time)} $timeStr"
}
