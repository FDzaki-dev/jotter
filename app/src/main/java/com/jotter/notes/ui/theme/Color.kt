package com.jotter.notes.ui.theme

import androidx.compose.ui.graphics.Color

// iOS system color palette (light mode values) - used for note color tags
data class NoteColorOption(val label: String, val color: Color)

val NoteColors = listOf(
    NoteColorOption("Merah", Color(0xFFFF3B30)),
    NoteColorOption("Oranye", Color(0xFFFF9500)),
    NoteColorOption("Kuning", Color(0xFFFFCC00)),
    NoteColorOption("Hijau", Color(0xFF34C759)),
    NoteColorOption("Toska", Color(0xFF30B0C7)),
    NoteColorOption("Biru", Color(0xFF007AFF)),
    NoteColorOption("Nila", Color(0xFF5856D6)),
    NoteColorOption("Ungu", Color(0xFFAF52DE)),
    NoteColorOption("Merah Muda", Color(0xFFFF2D55)),
)

fun noteColorFor(index: Int): Color =
    if (index in NoteColors.indices) NoteColors[index].color else NoteColors[0].color

val JotterBackground = Color(0xFF000000)
val JotterSurface = Color(0xFF1C1C1E)
val JotterSurfaceElevated = Color(0xFF2C2C2E)
val JotterLabel = Color(0xFFFFFFFF)
val JotterSecondaryLabel = Color(0xFF8E8E93)
