package com.jotter.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jotter.notes.data.Note
import com.jotter.notes.data.NoteType
import com.jotter.notes.ui.theme.JotterSurface
import com.jotter.notes.ui.theme.noteColorFor
import java.util.Calendar

// v2_Batch54: kepadatan kartu utk 4 mode tampilan HomeScreen (video showcase ColorNote - menu
// "Lihat": Daftar/Detail/Petak/Petak Besar). COMPACT = "Daftar" (0 preview, cuma judul+tanggal,
// padding minimal). NORMAL = "Detail"/"Petak" (perilaku lama, TIDAK berubah sama sekali kalau
// parameter ini di-default/tidak diisi - 0 regresi utk pemanggil existing spt FilteredNotesScreen.kt
// yang belum di-update). LARGE = "Petak Besar" (preview lebih panjang, tipografi/padding lebih besar).
enum class CardDensity { COMPACT, NORMAL, LARGE }

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
    density: CardDensity = CardDensity.NORMAL,
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
        NoteCardContent(note = note, onTap = onTap, density = density)
    }
}

@Composable
private fun NoteCardContent(note: Note, onTap: () -> Unit, density: CardDensity) {
    val accentColor = noteColorFor(note.colorIndex)
    // P2.12 color/border treatment: swap uniform 1.5dp border ring + small header dot for a
    // left accent bar + subtle background tint (14% lerp toward the note's color). Closer to
    // the original ColorNote-style bold per-note color signature from the spec than a plain
    // outline was - the color is now the card's dominant visual identity, not an afterthought.
    val cardBackground = lerp(JotterSurface, accentColor, 0.14f)

    // v2_Batch54: checklist "semua item tercentang" - treatment visual grayed-out+strikethrough
    // + badge centang, meniru kartu "Daftar barang" di video showcase (checklist tuntas, bukan
    // note biasa). Sengaja tidak berlaku kalau note.isLocked (title/preview memang sudah
    // disamarkan total di jalur lock, jangan dobel logic di titik yang sama).
    val isFullyChecked = note.type == NoteType.CHECKLIST &&
        note.checklistItems.isNotEmpty() &&
        note.checklistItems.all { it.isChecked } &&
        !note.isLocked

    val contentPadding = when (density) {
        CardDensity.COMPACT -> 10.dp
        CardDensity.LARGE -> 16.dp
        CardDensity.NORMAL -> 14.dp
    }
    val titleStyle = if (density == CardDensity.LARGE) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium
    val previewTextMaxLines = if (density == CardDensity.LARGE) 8 else 4
    val previewChecklistMaxItems = if (density == CardDensity.LARGE) 6 else 3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .clickable(onClick = onTap)
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(accentColor))
        Column(modifier = Modifier.weight(1f).padding(contentPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            if (isFullyChecked) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.CheckCircle, "Checklist selesai", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (note.title.isNotEmpty() && !note.isLocked) {
            Text(
                note.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = titleStyle,
                color = if (isFullyChecked) Color.Gray else Color.Unspecified,
                textDecoration = if (isFullyChecked) TextDecoration.LineThrough else null
            )
        } else if (note.isLocked) {
            Text("Catatan Terkunci", maxLines = 1, overflow = TextOverflow.Ellipsis, style = titleStyle, color = Color.Gray)
        }

        // "Daftar" (COMPACT): meniru mode List ringkas ColorNote - cuma judul+tanggal, 0 preview
        // sama sekali (bukan cuma dipangkas ke 1 baris), biar densitas per layar jauh lebih tinggi
        // dibanding Detail/Petak/Petak Besar - itu esensi bedanya "Daftar" vs 3 mode lain.
        if (density != CardDensity.COMPACT) {
            Spacer(Modifier.height(6.dp))
            when {
                note.isLocked -> Text("•••••••", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                note.type == NoteType.CHECKLIST -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    note.checklistItems.take(previewChecklistMaxItems).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                null, tint = Color.Gray, modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                item.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.Gray,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                else -> Text(note.content, maxLines = previewTextMaxLines, overflow = TextOverflow.Ellipsis, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }

        // v2_Batch54: tanggal terakhir diubah - SEBELUMNYA TIDAK PERNAH dirender sama sekali di
        // kartu manapun (LIST/GRID), padahal `modifiedAt` sudah ada di data model & dipakai di
        // tempat lain (dialog restore, dst). Video showcase user (ColorNote) menampilkan tanggal
        // di SETIAP kartu tanpa kecuali - gap paling jelas yang bikin beranda kerasa "belum selesai"
        // dibanding referensi. Sengaja TIDAK ditampilkan utk note terkunci (konsisten dgn masking
        // metadata lain di kartu terkunci - lihat komentar reminder di atas).
        if (!note.isLocked) {
            Spacer(Modifier.height(if (density == CardDensity.COMPACT) 2.dp else 8.dp))
            Text(formatCardDate(note.modifiedAt), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
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

/** v2_Batch54: tanggal terakhir diubah di footer kartu - "10 Sep" kalau tahun berjalan (pola sama
 * dgn formatReminderBadge di atas), "26 Mei 2025" kalau beda tahun - konsisten dgn tampilan
 * tanggal ala ColorNote di video showcase (tahun cuma muncul kalau relevan/bukan tahun ini). */
private fun formatCardDate(modifiedAt: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = modifiedAt }
    val now = Calendar.getInstance()
    val sameYear = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    val pattern = if (sameYear) "d MMM" else "d MMM yyyy"
    return java.text.SimpleDateFormat(pattern, java.util.Locale("id", "ID")).format(cal.time)
}
