package com.jotter.notes.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotter.notes.auth.AuthManager
import com.jotter.notes.backup.BackupFileInfo
import com.jotter.notes.backup.BackupManager
import com.jotter.notes.backup.BackupResult
import com.jotter.notes.backup.RestoreResult
import com.jotter.notes.ui.components.UpdateDialog
import com.jotter.notes.updater.UpdateChecker
import com.jotter.notes.viewmodel.SettingsViewModel
import com.jotter.notes.viewmodel.UpdaterUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onOpenLockSetup: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { AuthManager(context) }
    val isLockEnabled by viewModel.isLockEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val updaterState by viewModel.updaterState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentVersionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }
    // Backup/restore (Batch40 — wiring UI dari logic Batch38/39). BackupManager itu `object`
    // stateless, dipanggil langsung dari sini (bukan lewat SettingsViewModel) — pola sama dgn
    // toggle PIN/Biometrik di file ini yang juga manggil AuthManager langsung, TANPA nambah
    // state machine StateFlow spt updater (yg genuinely butuh multi-step: check→download→install).
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupFileInfo?>(null) }

    // Sama persis BackupManager.restore() yang dipakai dialog konfirmasi di bawah — diekstrak
    // jadi 1 fungsi krn sekarang ada 2 entry point (auto-detect findLatestBackup() DAN SAF picker
    // manual di bawah), biar pesan Snackbar hasil restore konsisten & gak diketik 2x.
    suspend fun performRestore(uri: Uri): String {
        val result = BackupManager.restore(context, uri)
        return when (result) {
            is RestoreResult.Success -> {
                val base = "Berhasil memulihkan ${result.restoredCount} catatan"
                if (result.lockedPlaceholderCount > 0) {
                    "$base (${result.lockedPlaceholderCount} di antaranya catatan terkunci — isinya placeholder, bukan konten asli, sesuai desain keamanan backup)"
                } else base
            }
            is RestoreResult.Error -> "Pulihkan gagal: ${result.message}"
        }
    }

    // Fallback SAF (Storage Access Framework) — risiko dicatat eksplisit sejak Batch39: query
    // MediaStore (findLatestBackup) ada di area abu2 scoped storage API 29+ pasca app
    // uninstall+instal-ulang, TIDAK 100% pasti selalu bisa nemu balik file yang app sendiri buat
    // di sesi SEBELUMNYA di semua versi Android/OEM. ACTION_OPEN_DOCUMENT SELALU jalan tanpa
    // permission tambahan apapun (user pilih file lewat picker bawaan OS, dapat izin baca
    // sementara ke URI itu otomatis) — 1 tap ekstra dari user, tapi 0 ketergantungan ke
    // kemungkinan query MediaStore gagal. TIDAK butuh takePersistableUriPermission krn file
    // langsung dibaca sekali saat itu juga (bukan disimpan buat dipakai lagi nanti).
    val restoreFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isRestoring = true
            scope.launch {
                val message = performRestore(uri)
                isRestoring = false
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pengaturan") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text("KEAMANAN", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
            ListItem(
                headlineContent = { Text("Kunci Aplikasi (PIN)") },
                trailingContent = {
                    Switch(checked = isLockEnabled, onCheckedChange = { v ->
                        if (v) {
                            onOpenLockSetup()
                        } else {
                            auth.clearPin()
                            viewModel.refresh()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Kunci PIN dinonaktifkan. Kalau ada catatan yang masih terkunci, atur PIN baru lagi untuk membukanya.",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    })
                }
            )
            if (isLockEnabled) {
                ListItem(
                    headlineContent = { Text("Gunakan Biometrik") },
                    trailingContent = {
                        Switch(checked = isBiometricEnabled, onCheckedChange = { v ->
                            if (v) {
                                val unavailableReason = auth.biometricUnavailableReason()
                                if (unavailableReason == null) {
                                    viewModel.setBiometricEnabled(true)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Biometrik diaktifkan",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = unavailableReason,
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            } else {
                                viewModel.setBiometricEnabled(false)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Biometrik dinonaktifkan",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        })
                    }
                )
            }

            Text("CATATAN", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
            ListItem(
                headlineContent = { Text("Arsip") },
                leadingContent = { Icon(Icons.Default.Archive, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable(onClick = onOpenArchive)
            )
            ListItem(
                headlineContent = { Text("Sampah") },
                leadingContent = { Icon(Icons.Default.Delete, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable(onClick = onOpenTrash)
            )

            Text("CADANGAN DATA", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
            ListItem(
                headlineContent = { Text("Backup Data") },
                supportingContent = { Text("Simpan salinan semua catatan ke Documents/Jotter/backup (catatan terkunci disimpan tanpa isi asli, demi keamanan)") },
                leadingContent = { Icon(Icons.Default.Backup, null) },
                trailingContent = {
                    if (isBackingUp) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.ChevronRight, null)
                },
                modifier = Modifier.clickable(enabled = !isBackingUp) {
                    isBackingUp = true
                    scope.launch {
                        val result = BackupManager.backup(context, "Jotter")
                        isBackingUp = false
                        val message = when (result) {
                            is BackupResult.Success -> "Backup berhasil — ${result.noteCount} catatan disimpan (${result.fileName})"
                            is BackupResult.Error -> "Backup gagal: ${result.message}"
                        }
                        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Pulihkan dari Backup") },
                supportingContent = { Text("Ambil catatan dari file backup paling baru di Documents/Jotter/backup") },
                leadingContent = { Icon(Icons.Default.Restore, null) },
                trailingContent = {
                    if (isRestoring) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.ChevronRight, null)
                },
                modifier = Modifier.clickable(enabled = !isRestoring) {
                    scope.launch {
                        val info = withContext(Dispatchers.IO) { BackupManager.findLatestBackup(context, "Jotter") }
                        if (info == null) {
                            snackbarHostState.showSnackbar(
                                message = "Belum ada file backup ditemukan di Documents/Jotter/backup",
                                duration = SnackbarDuration.Long
                            )
                        } else {
                            pendingRestore = info
                        }
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Pilih File Backup Manual") },
                supportingContent = { Text("Kalau deteksi otomatis di atas tidak menemukan file, pilih manual lewat file picker") },
                leadingContent = { Icon(Icons.Default.FolderOpen, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable(enabled = !isRestoring) {
                    restoreFilePicker.launch(arrayOf("application/json"))
                }
            )

            Text("PEMBARUAN APLIKASI", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
            ListItem(
                headlineContent = { Text("Cek Pembaruan") },
                supportingContent = { Text("Versi terpasang: $currentVersionName") },
                leadingContent = { Icon(Icons.Default.Refresh, null) },
                trailingContent = {
                    if (updaterState is UpdaterUiState.Checking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                },
                modifier = Modifier.clickable(onClick = viewModel::checkForUpdate)
            )
            ListItem(
                headlineContent = { Text("Lihat Rilis di GitHub") },
                supportingContent = { Text("Buka halaman rilis & unduh APK manual di browser") },
                leadingContent = { Icon(Icons.Default.OpenInNew, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable {
                    val url = "https://github.com/${UpdateChecker.REPO}/releases/latest"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )

            Spacer(Modifier.weight(1f))
            Text(
                "Jotter · 100% offline, semua catatan tersimpan di perangkat ini",
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    pendingRestore?.let { info ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Pulihkan dari Backup?") },
            text = {
                val dateStr = java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                    .format(java.util.Date(info.dateAddedSeconds * 1000L))
                Text("File backup ditemukan dari $dateStr. Catatan dari file ini akan ditambahkan ke catatan yang ada sekarang (catatan dengan ID sama akan ditimpa versi backup). Lanjutkan?")
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = info.uri
                    pendingRestore = null
                    isRestoring = true
                    scope.launch {
                        val message = performRestore(uri)
                        isRestoring = false
                        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
                    }
                }) { Text("Pulihkan") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Batal") }
            }
        )
    }

    UpdateDialog(
        state = updaterState,
        installedVersionName = currentVersionName,
        onDismiss = viewModel::dismissUpdaterDialog,
        onStartDownload = { asset, tagName -> viewModel.startDownload(asset, tagName) },
        onInstall = { file, tagName ->
            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            viewModel.markInstalled(tagName)
        }
    )
}
