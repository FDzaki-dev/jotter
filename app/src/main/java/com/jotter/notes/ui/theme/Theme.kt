package com.jotter.notes.ui.theme

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// v2_Batch51: user minta alternatif dari AMOLED hardcode - 3 tema gradasi baru ditambah,
// AMOLED tetap default (0 perubahan visual bagi user yang gak pernah buka pemilih tema).
enum class AppTheme(val label: String, val previewSwatch: List<Color>) {
    AMOLED("AMOLED Klasik", listOf(Color(0xFF000000), Color(0xFF1C1C1E))),
    AURORA("Aurora", listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))),
    SUNSET("Senja", listOf(Color(0xFF1A0B2E), Color(0xFF5C1A45), Color(0xFF8B2F5C))),
    OCEAN("Samudra", listOf(Color(0xFF001220), Color(0xFF063C4B), Color(0xFF0B5E6B)))
}

// Singleton ringan (bukan ViewModel penuh) - state tema perlu diakses dari 2 tempat yang gak
// ada hubungan parent-child langsung (JotterTheme di root vs SettingsScreen di dalam NavGraph),
// jadi StateFlow di object lebih simpel drpd nembus ViewModel lewat banyak layer. Reuse
// SharedPreferences "ui_prefs" - file yang sama dipakai view_mode/swipe_hint_dismissed.
object ThemeManager {
    private var themeFlow: MutableStateFlow<AppTheme>? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    fun flow(context: Context): StateFlow<AppTheme> {
        var f = themeFlow
        if (f == null) {
            val saved = prefs(context).getString("app_theme", AppTheme.AMOLED.name)
            f = MutableStateFlow(runCatching { AppTheme.valueOf(saved ?: "") }.getOrDefault(AppTheme.AMOLED))
            themeFlow = f
        }
        return f
    }

    fun setTheme(context: Context, theme: AppTheme) {
        prefs(context).edit().putString("app_theme", theme.name).apply()
        flow(context) // pastikan sudah diinisialisasi
        themeFlow?.value = theme
    }
}

private fun colorSchemeFor(theme: AppTheme) = if (theme == AppTheme.AMOLED) {
    darkColorScheme(
        primary = Color(0xFF007AFF),
        background = JotterBackground,
        surface = JotterSurface,
        surfaceVariant = JotterSurfaceElevated,
        onBackground = JotterLabel,
        onSurface = JotterLabel,
    )
} else {
    // Tema gradasi: background/surface DIBUAT TRANSPARAN/semi-transparan supaya gradient Box di
    // JotterTheme() nembus ke semua Scaffold (yang defaultnya pakai colorScheme.background APA
    // ADANYA, 0 screen lain perlu diubah). Surface semi-transparan putih = efek glassmorphism -
    // sekalian menuhin requirement "iOS-look glassmorphism" yang dari awal proyek ini emang wajib
    // tapi belum pernah benar2 ada wujud nyatanya (AMOLED lama 100% flat, 0 glass).
    darkColorScheme(
        primary = Color(0xFF0A84FF),
        background = Color.Transparent,
        surface = Color(0x1FFFFFFF),
        surfaceVariant = Color(0x33FFFFFF),
        onBackground = Color.White,
        onSurface = Color.White,
    )
}

private fun gradientFor(theme: AppTheme): List<Color> =
    if (theme == AppTheme.AMOLED) listOf(JotterBackground, JotterBackground) else theme.previewSwatch

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
    val context = LocalContext.current
    val theme by ThemeManager.flow(context).collectAsState()

    MaterialTheme(
        colorScheme = colorSchemeFor(theme),
        shapes = JotterShapes,
        typography = JotterTypography,
    ) {
        // Gradient (atau solid hitam utk AMOLED) digambar SEKALI di sini, di root - semua
        // screen anak otomatis "tembus" lihat ini krn Scaffold mereka pakai
        // colorScheme.background apa adanya (transparent utk tema gradasi), 0 screen lain
        // perlu disentuh satu2.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientFor(theme)))
        ) {
            content()
        }
    }
}
