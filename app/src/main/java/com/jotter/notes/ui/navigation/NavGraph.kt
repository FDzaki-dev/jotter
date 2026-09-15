package com.jotter.notes.ui.navigation

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
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
        // v2_Batch59: user minta kompromi "fade pendek + opaque" drpd instan total (Batch58).
        // Mekanisme: EXIT tetap None (layar lama HILANG SEKETIKA, bukan fade-out) - ini yang
        // secara struktural mencegah 2 layar transparan tumpang tindih (akar bug ghosting Batch58
        // gak mungkin kejadian lagi krn gak pernah ada momen dua-duanya kelihatan bersamaan).
        // ENTER dikasih fade 150ms - layar baru muncul dgn transisi halus di atas gradient root
        // (yang sendirinya statis/gak ikut animasi, jadi bukan "opaque" literal, tapi efeknya
        // sama: gak ada teks/kartu numpuk teks/kartu lain, cuma satu konten yang fade-in sendirian).
        NavHost(
            navController = tabNavController,
            startDestination = Routes.HOME,
            modifier = androidx.compose.ui.Modifier.padding(padding),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { fadeIn(animationSpec = tween(150)) },
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

    // v2_Batch59: kompromi sama persis dgn NavHost tab di atas - lihat komentar di sana utk
    // penjelasan mekanisme "exit instan + enter fade pendek".
    NavHost(
        navController = rootNavController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
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
