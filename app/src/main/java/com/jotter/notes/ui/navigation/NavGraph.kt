package com.jotter.notes.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jotter.notes.auth.AuthManager
import com.jotter.notes.ui.screens.*
import androidx.compose.ui.platform.LocalContext

private object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{noteId}"
    const val ARCHIVE = "archive"
    const val TRASH = "trash"
    const val LOCK_SETUP = "lock_setup"
    const val LOCK_VERIFY = "lock_verify"
    const val UNLOCKED_ROOT = "unlocked_root"
}

private val tabs = listOf(
    Triple(Routes.HOME, "Catatan", Icons.Default.Description),
    Triple(Routes.CALENDAR, "Kalender", Icons.Default.CalendarMonth),
    Triple(Routes.SETTINGS, "Pengaturan", Icons.Default.Settings),
)

@Composable
fun MainTabScaffold(rootNavController: androidx.navigation.NavHostController) {
    val tabNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination
                tabs.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            tabNavController.navigate(route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        // v2_Batch58: FIX bug ghosting yang kelihatan di recording user - pas pindah tab (mis.
        // Pengaturan -> Kalender), konten tab LAMA sempat numpuk transparan kebaca jelas di
        // belakang tab BARU selama animasi crossfade default NavHost berjalan. Akar masalahnya:
        // tiap Scaffold layar pakai `containerColor`/`colorScheme.background` yang SENGAJA
        // `Color.Transparent` di tema gradasi (biar gradient root nembus) - itu bagus utk kondisi
        // statis, tapi selama crossfade (~300ms, dua layar transparan dianimasikan alpha
        // bersamaan) hasilnya dua konten kebaca numpuk, bukan dissolve mulus kayak di app dgn
        // background solid. Fix: matikan animasinya total (None) - transisi jadi instan, 0 window
        // waktu utk numpuk. Trade-off sadar: kehilangan fade halus, TAPI itu jauh lebih baik drpd
        // regresi visual yang bikin app kelihatan rusak.
        NavHost(
            navController = tabNavController,
            startDestination = Routes.HOME,
            modifier = androidx.compose.ui.Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Routes.HOME) {
                HomeScreen(onOpenNote = { id -> rootNavController.navigate("editor/${id ?: "new"}") })
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(onOpenNote = { id -> rootNavController.navigate("editor/$id") })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenLockSetup = { rootNavController.navigate(Routes.LOCK_SETUP) },
                    onOpenArchive = { rootNavController.navigate(Routes.ARCHIVE) },
                    onOpenTrash = { rootNavController.navigate(Routes.TRASH) }
                )
            }
        }
    }
}

@Composable
fun JotterNavGraph() {
    val context = LocalContext.current
    val auth = remember { AuthManager(context) }
    val rootNavController = rememberNavController()
    val startDestination = if (auth.hasPinSet()) Routes.LOCK_VERIFY else Routes.UNLOCKED_ROOT

    // v2_Batch58: sama persis alasannya dgn NavHost tab di MainTabScaffold di atas - root NavHost
    // ini yang nangani Editor/Archive/Trash/Lock, dan ghosting yang sama juga kekonfirmasi pas
    // back-navigation dari Editor ke Home (kartu Home numpuk transparan di belakang layar Editor
    // yang lagi nutup).
    NavHost(
        navController = rootNavController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Routes.UNLOCKED_ROOT) { MainTabScaffold(rootNavController) }

        composable(Routes.LOCK_VERIFY) {
            LockScreen(mode = LockMode.VERIFY, onResult = { success ->
                if (success) {
                    rootNavController.navigate(Routes.UNLOCKED_ROOT) {
                        popUpTo(Routes.LOCK_VERIFY) { inclusive = true }
                    }
                }
            })
        }

        composable(Routes.LOCK_SETUP) {
            LockScreen(mode = LockMode.SETUP, onResult = { success -> rootNavController.popBackStack() })
        }

        composable(Routes.EDITOR) { backStackEntry ->
            val noteIdArg = backStackEntry.arguments?.getString("noteId")
            NoteEditorScreen(
                noteId = if (noteIdArg == "new") null else noteIdArg,
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Routes.ARCHIVE) {
            FilteredNotesScreen(
                mode = FilteredMode.ARCHIVE,
                onOpenNote = { id -> rootNavController.navigate("editor/$id") },
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable(Routes.TRASH) {
            FilteredNotesScreen(
                mode = FilteredMode.TRASH,
                onOpenNote = { id -> rootNavController.navigate("editor/$id") },
                onBack = { rootNavController.popBackStack() }
            )
        }
    }
}
