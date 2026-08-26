package com.jotter.notes.ui.screens

import android.content.Intent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotter.notes.auth.AuthManager
import com.jotter.notes.ui.components.UpdateDialog
import com.jotter.notes.viewmodel.SettingsViewModel
import com.jotter.notes.viewmodel.UpdaterUiState

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
    val currentVersionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Pengaturan") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text("KEAMANAN", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
            ListItem(
                headlineContent = { Text("Kunci Aplikasi (PIN)") },
                trailingContent = {
                    Switch(checked = isLockEnabled, onCheckedChange = { v ->
                        if (v) onOpenLockSetup() else { auth.clearPin(); viewModel.refresh() }
                    })
                }
            )
            if (isLockEnabled) {
                ListItem(
                    headlineContent = { Text("Gunakan Biometrik") },
                    trailingContent = {
                        Switch(checked = isBiometricEnabled, onCheckedChange = { v ->
                            if (!v || auth.canUseBiometrics()) viewModel.setBiometricEnabled(v)
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

            Spacer(Modifier.weight(1f))
            Text(
                "Jotter v2.0 (Native Kotlin) · 100% offline · Log crash tersimpan di Documents/Jotter/logs",
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    UpdateDialog(
        state = updaterState,
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
