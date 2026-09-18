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
import com.jotter.notes.ui.theme.JotterSecondaryLabel
import com.jotter.notes.ui.theme.JotterSurface
import com.jotter.notes.ui.theme.noteColorFor
import java.util.Calendar

// v2_Batch61: local `OpaqueCardBase` (nilai 0xFF1C1C1E, alasan lengkap kenapa harus opaque - lihat
// Batch56 - dipindah ke Color.kt) dikonsolidasi ke `JotterSurface` yang sudah ada di Color.kt,
// nilai literalnya PERSIS SAMA - 0 perubahan visual, cuma hapus duplikat.
enum class CardDensity { COMPACT, DETAIL, GRID, GRID_LARGE }

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
    density: CardDensity = CardDensity.DETAIL,
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
    // v2_Batch56: base warna kartu SELALU opaque (bukan MaterialTheme.colorScheme.surface yg
    // transparan di tema gradasi - lihat riwayat Batch56 di PROJECT_STATE.md utk detail lengkap).
    // v2_Batch61: sumbernya `JotterSurface` (Color.kt, dikonsolidasi dari local OpaqueCardBase,
    // nilai sama). Fraksi lerp 0.32 - identitas warna kategori makin jelas dibedakan.
    val cardBackground = lerp(JotterSurface, accentColor, 0.32f)

    // Checklist "semua item tercentang" - treatment visual grayed-out+strikethrough + badge
    // centang, meniru kartu "Daftar barang" di video showcase (berlaku di semua mode termasuk grid).
    val isFullyChecked = note.type == NoteType.CHECKLIST &&
        note.checklistItems.isNotEmpty() &&
        note.checklistItems.all { it.isChecked } &&
        !note.isLocked

    val isGrid = density == CardDensity.GRID || density == CardDensity.GRID_LARGE
    val contentPadding = when (density) {
        CardDensity.COMPACT -> 10.dp
        CardDensity.GRID -> 10.dp
        CardDensity.GRID_LARGE -> 14.dp
        CardDensity.DETAIL -> 14.dp
    }
    val titleStyle = if (density == CardDensity.GRID_LARGE) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium
    val titleMaxLines = if (isGrid) 2 else 1
    val previewTextMaxLines = when (density) {
        CardDensity.GRID -> 3
        CardDensity.GRID_LARGE -> 5
        else -> 4
    }
    val previewChecklistMaxItems = when (density) {
        CardDensity.GRID -> 4
        CardDensity.GRID_LARGE -> 6
        else -> 3
    }

    // v2_Batch56: SEMUA teks/icon "sekunder" di kartu (preview, item checklist, tanggal, badge
    // reminder, placeholder terkunci) sekarang SATU sumber warna: JotterSecondaryLabel (abu TERANG
    // ala iOS dark-mode, lihat Color.kt) - GANTI TOTAL dari `Color.Gray` (abu medium generik Compose,
    // kontrasnya lemah di atas kartu gelap+tinted). Judul pakai Color.White eksplisit (bukan
    // Color.Unspecified/inherit) - predictable 100%, gak gantung ke resolusi ambient content-color
    // yang secara teori sama tapi gak pernah benar2 diverifikasi via compile di sandbox ini.
    val secondaryColor = JotterSecondaryLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .clickable(onClick = onTap)
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(accentColor))
        Column(modifier = Modifier.weight(1f).padding(contentPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.isLocked) Icon(Icons.Default.Lock, null, tint = secondaryColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.weight(1f))
            note.reminderAt?.let { reminderAt ->
                if (note.isLocked) {
                    Icon(Icons.Default.Notifications, null, tint = secondaryColor, modifier = Modifier.size(14.dp))
                } else {
                    val isOverdue = reminderAt < System.currentTimeMillis()
                    val reminderColor = if (isOverdue) Color(0xFFFF3B30) else secondaryColor
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = reminderColor, modifier = Modifier.size(14.dp))
                        if (!isGrid) {
                            Spacer(Modifier.width(3.dp))
                            Text(formatReminderBadge(reminderAt), color = reminderColor, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (isFullyChecked) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.CheckCircle, "Checklist selesai", tint = secondaryColor, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (note.title.isNotEmpty() && !note.isLocked) {
            Text(
                note.title,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                style = titleStyle,
                color = if (isFullyChecked) secondaryColor else Color.White,
                textDecoration = if (isFullyChecked) TextDecoration.LineThrough else null
            )
        } else if (note.isLocked) {
            Text("Catatan Terkunci", maxLines = 1, overflow = TextOverflow.Ellipsis, style = titleStyle, color = secondaryColor)
        }

        // "Daftar" (COMPACT): 0 preview sama sekali, cuma judul+tanggal - densitas tertinggi.
        if (density != CardDensity.COMPACT) {
            Spacer(Modifier.height(6.dp))
            when {
                note.isLocked -> Text("•••••••", color = secondaryColor, style = MaterialTheme.typography.bodySmall)
                note.type == NoteType.CHECKLIST -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    note.checklistItems.take(previewChecklistMaxItems).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                null, tint = secondaryColor, modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                item.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = secondaryColor,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                else -> Text(note.content, maxLines = previewTextMaxLines, overflow = TextOverflow.Ellipsis, color = secondaryColor, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Tanggal terakhir diubah - cuma di Daftar & Detail (lihat isGrid, tetap dari Batch55).
        if (!note.isLocked && !isGrid) {
            Spacer(Modifier.height(if (density == CardDensity.COMPACT) 2.dp else 8.dp))
            Text(formatCardDate(note.modifiedAt), color = secondaryColor, style = MaterialTheme.typography.labelSmall)
        }
        }
    }
}

/** "14:30" kalau hari ini, "26 Agu 14:30" kalau bukan — dipakai di Daftar/Detail (grid cuma pakai icon). */
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

/** Tanggal terakhir diubah di footer kartu (Daftar/Detail doang) - "10 Sep" tahun berjalan, "26 Mei 2025" beda tahun. */
private fun formatCardDate(modifiedAt: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = modifiedAt }
    val now = Calendar.getInstance()
    val sameYear = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    val pattern = if (sameYear) "d MMM" else "d MMM yyyy"
    return java.text.SimpleDateFormat(pattern, java.util.Locale("id", "ID")).format(cal.time)
}
