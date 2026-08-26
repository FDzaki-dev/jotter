package com.jotter.notes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotter.notes.data.ChecklistItem
import com.jotter.notes.data.Note
import com.jotter.notes.data.NoteType
import com.jotter.notes.ui.theme.NoteColors
import com.jotter.notes.ui.theme.noteColorFor
import com.jotter.notes.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

// Uses BackHandler (native Android predictive-back integration via OnBackPressedDispatcher) -
// this REPLACES Flutter's PopScope+onPopInvoked, which had a confirmed framework bug where the
// callback never fired for gesture-triggered back when canPop=false. BackHandler has no such
// issue: it hooks directly into the system back callback, works identically for gesture AND
// button/system-bar back, since Android itself (not a cross-platform shim) owns the mechanism.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    viewModel: NotesViewModel = viewModel(),
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf(Note()) }
    var originalNote by remember { mutableStateOf<Note?>(if (noteId == null) note else null) }
    var isNew by remember { mutableStateOf(noteId == null) }
    var loaded by remember { mutableStateOf(noteId == null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    // PIN/biometrik ulang WAJIB sebelum konten note terkunci dirender — sebelumnya TIDAK ADA
    // gerbang apapun di sini: isLocked cuma dipakai utk MASKING title/content di list (NoteCard,
    // CalendarScreen), tapi begitu note di-tap, layar ini langsung render isi asli tanpa syarat.
    // `unlocked` di-keyed ke noteId biar reset tiap kali buka note (locked) yang beda.
    var unlocked by remember(noteId) { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.getById(noteId)?.let { note = it; isNew = false; originalNote = it }
            loaded = true
        }
    }

    // Dirty-check (audit High #7 / verdict P1.4): hanya panggil saveNote() kalau memang ada
    // perubahan dari state awal. Sebelumnya, buka catatan lama lalu langsung back TANPA
    // edit apapun tetap memicu save -> modifiedAt ke-bump padahal isinya identik.
    fun saveAndExit() {
        val isEmptyNew = isNew && note.title.isBlank() && note.content.isBlank() && note.checklistItems.isEmpty()
        val hasChanges = originalNote != null && note != originalNote
        if (!isEmptyNew && hasChanges) {
            viewModel.saveNote(note)
        }
        onBack()
    }

    val requiresUnlock = !isNew && note.isLocked
    // Selama gerbang PIN/biometrik belum lolos, konten note tidak pernah dirender -> tidak ada
    // yang perlu di-"save" kalau user batal (BackHandler cukup onBack() polos, bukan saveAndExit()).
    BackHandler(enabled = true) { if (requiresUnlock && !unlocked) onBack() else saveAndExit() }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (requiresUnlock && !unlocked) {
        LockScreen(mode = LockMode.VERIFY, onResult = { success -> if (success) unlocked = true else onBack() })
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Catatan") },
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Box(Modifier.size(20.dp).clip(CircleShape).background(noteColorFor(note.colorIndex)))
                    }
                    IconButton(onClick = { showReminderPicker = true }) {
                        Icon(if (note.reminderAt != null) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone, "Pengingat")
                    }
                    IconButton(onClick = {
                        val newLocked = !note.isLocked
                        note = note.copy(isLocked = newLocked)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (newLocked) "Catatan dikunci" else "Catatan tidak lagi dikunci",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }) {
                        Icon(if (note.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, "Kunci")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = note.title,
                onValueChange = { note = note.copy(title = it) },
                placeholder = { Text("Judul") },
                textStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            if (isNew) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = note.type == NoteType.TEXT,
                        onClick = { note = note.copy(type = NoteType.TEXT) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Teks") }
                    SegmentedButton(
                        selected = note.type == NoteType.CHECKLIST,
                        onClick = { note = note.copy(type = NoteType.CHECKLIST) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Checklist") }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (note.type == NoteType.CHECKLIST) {
                Column(Modifier.weight(1f)) {
                    LazyColumn(Modifier.weight(1f)) {
                        items(note.checklistItems, key = { it.id }) { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = {
                                    note = note.copy(checklistItems = note.checklistItems.map {
                                        if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it
                                    })
                                }) {
                                    Icon(if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null)
                                }
                                Text(item.text, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    // Feedback+undo (P1.9) - sebelumnya senyap total, item lenyap
                                    // tanpa jejak. Pola identik archiveWithUndo/deleteWithUndo di
                                    // HomeScreen: hapus dulu (biar responsif), tawarkan "Urungkan"
                                    // yang masukin balik item PERSIS di posisi semula (bukan di-append
                                    // ke akhir list, biar urutan checklist gak berantakan kalau di-undo).
                                    val removedIndex = note.checklistItems.indexOfFirst { it.id == item.id }
                                    note = note.copy(checklistItems = note.checklistItems.filter { it.id != item.id })
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Item dihapus",
                                            actionLabel = "Urungkan",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            note = note.copy(checklistItems = note.checklistItems.toMutableList().apply {
                                                add(removedIndex.coerceIn(0, size), item)
                                            })
                                        }
                                    }
                                }) { Icon(Icons.Default.Close, "Hapus item", modifier = Modifier.size(22.dp)) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newChecklistText,
                            onValueChange = { newChecklistText = it },
                            placeholder = { Text("Tambah item") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            if (newChecklistText.isNotBlank()) {
                                note = note.copy(checklistItems = note.checklistItems + ChecklistItem(text = newChecklistText.trim()))
                                newChecklistText = ""
                            }
                        }) { Icon(Icons.Default.AddCircle, "Tambah item") }
                    }
                }
            } else {
                OutlinedTextField(
                    value = note.content,
                    onValueChange = { note = note.copy(content = it) },
                    placeholder = { Text("Tulis catatan...") },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }

    if (showColorPicker) {
        ModalBottomSheet(onDismissRequest = { showColorPicker = false }) {
            FlowRowColors(selectedIndex = note.colorIndex) { index ->
                note = note.copy(colorIndex = index)
                showColorPicker = false
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showReminderPicker) {
        ReminderPickerSheet(
            initial = note.reminderAt,
            onClear = {
                val hadReminder = note.reminderAt != null
                note = note.copy(reminderAt = null)
                showReminderPicker = false
                if (hadReminder) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Pengingat dihapus dari catatan ini",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onConfirm = { millis ->
                note = note.copy(reminderAt = millis)
                showReminderPicker = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Pengingat diatur — aktif setelah catatan disimpan",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDismiss = { showReminderPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowColors(selectedIndex: Int, onPick: (Int) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NoteColors.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(option.color)
                    .then(
                        if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                    )
                    .clickable { onPick(index) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderPickerSheet(
    initial: Long?,
    onClear: () -> Unit,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val cal = java.util.Calendar.getInstance()
    initial?.let { cal.timeInMillis = it }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = cal.timeInMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(java.util.Calendar.MINUTE)
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onClear) { Text("Hapus Pengingat") }
                TextButton(onClick = {
                    val resultCal = java.util.Calendar.getInstance()
                    datePickerState.selectedDateMillis?.let { resultCal.timeInMillis = it }
                    resultCal.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                    resultCal.set(java.util.Calendar.MINUTE, timePickerState.minute)
                    onConfirm(resultCal.timeInMillis)
                }) { Text("Selesai") }
            }
            DatePicker(state = datePickerState, showModeToggle = false)
            TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
        }
    }
}
