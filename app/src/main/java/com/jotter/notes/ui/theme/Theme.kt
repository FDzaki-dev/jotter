package com.jotter.notes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val JotterColorScheme = darkColorScheme(
    primary = Color(0xFF007AFF),
    background = JotterBackground,
    surface = JotterSurface,
    surfaceVariant = JotterSurfaceElevated,
    onBackground = JotterLabel,
    onSurface = JotterLabel,
)

val JotterShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val JotterTypography = Typography(
    // 5 role di bawah SUDAH ADA sejak awal & dipakai di beberapa tempat (LargeTopAppBar
    // collapsed, ModalBottomSheet title, dst) - NILAINYA TIDAK DIUBAH sama sekali.
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    // P2.10 (perkuat typography hierarchy): 10 role di bawah SEBELUMNYA TIDAK DIDEFINISIKAN -
    // otomatis jatuh ke default generik Material3 (Roboto standar Android, ukuran/weight beda
    // sendiri dari skala Jotter di atas). Efeknya nyata & gak keliatan dari kode: LargeTopAppBar
    // pas EXPANDED pakai role `headlineMedium`, dan SEMUA teks Button/TextButton di app pakai
    // `labelLarge` - keduanya diam2 render pakai style asing yg gak nyambung sama 5 role custom
    // di atas. Sekarang dilengkapi jadi 1 skala turun yang koheren, TANPA ubah fontFamily
    // (di luar scope "hierarchy" - itu soal ukuran/weight relatif, bukan jenis huruf).
    displayLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun JotterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JotterColorScheme,
        shapes = JotterShapes,
        typography = JotterTypography,
        content = content
    )
}
