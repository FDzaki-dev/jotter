package com.jotter.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotter.notes.ui.theme.noteColorFor
import com.jotter.notes.viewmodel.NotesViewModel
import java.util.Calendar

@Composable
fun CalendarScreen(viewModel: NotesViewModel = viewModel(), onOpenNote: (String) -> Unit) {
    val reminders by viewModel.reminderNotes.collectAsState()
    var monthCursor by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance()) }

    fun sameDay(a: Calendar, millis: Long): Boolean {
        val b = Calendar.getInstance().apply { timeInMillis = millis }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    val dayNotes = reminders.filter { it.reminderAt != null && sameDay(selectedDay, it.reminderAt!!) }

    Scaffold(topBar = { TopAppBar(title = { Text("Kalender") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthCursor = (monthCursor.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) {
                    Icon(Icons.Default.ChevronLeft, "Bulan sebelumnya")
                }
                val monthFmt = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id", "ID"))
                Text(monthFmt.format(monthCursor.time), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { monthCursor = (monthCursor.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }) {
                    Icon(Icons.Default.ChevronRight, "Bulan berikutnya")
                }
            }

            val firstOfMonth = (monthCursor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            val startOffset = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = monthCursor.getActualMaximum(Calendar.DAY_OF_MONTH)
            val cells = (0 until startOffset).map { null } + (1..daysInMonth).map { it }

            Column(Modifier.padding(horizontal = 12.dp)) {
                listOf("S", "S", "R", "K", "J", "S", "M").let {
                    Row(Modifier.fillMaxWidth()) {
                        it.forEach { d -> Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.Gray) }
                    }
                }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(4.dp), contentAlignment = Alignment.Center) {
                                if (day != null) {
                                    val cal = (monthCursor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                                    val isSelected = sameDay(selectedDay, cal.timeInMillis)
                                    val hasReminder = reminders.any { it.reminderAt != null && sameDay(cal, it.reminderAt!!) }
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { selectedDay = cal },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(day.toString())
                                            if (hasReminder) Box(Modifier.size(4.dp).clip(CircleShape).background(Color.Red))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (dayNotes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada pengingat", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(dayNotes, key = { it.id }) { n ->
                        ListItem(
                            leadingContent = { Box(Modifier.size(12.dp).clip(CircleShape).background(noteColorFor(n.colorIndex))) },
                            headlineContent = { Text(if (n.isLocked) "Catatan Terkunci" else n.title.ifEmpty { "(Tanpa judul)" }) },
                            supportingContent = {
                                val c = Calendar.getInstance().apply { timeInMillis = n.reminderAt!! }
                                Text("%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)))
                            },
                            modifier = Modifier.clickable { onOpenNote(n.id) }
                        )
                    }
                }
            }
        }
    }
}
