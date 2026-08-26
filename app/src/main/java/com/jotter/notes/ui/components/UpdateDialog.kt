package com.jotter.notes.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jotter.notes.updater.ReleaseAsset
import com.jotter.notes.viewmodel.UpdaterUiState
import java.io.File

/**
 * Dialog rounded/minimal (cupertino-look, konsisten dgn JotterShapes bawaan AlertDialog Material3)
 * utk seluruh alur in-app updater — dipanggil dari SettingsScreen berdasarkan [UpdaterUiState]
 * milik SettingsViewModel. Sengaja TIDAK render apapun utk Idle/Checking (Checking ditampilkan
 * sbg spinner inline di baris "Cek Pembaruan" milik SettingsScreen, bukan dialog).
 */
@Composable
fun UpdateDialog(
    state: UpdaterUiState,
    onDismiss: () -> Unit,
    onStartDownload: (asset: ReleaseAsset, tagName: String) -> Unit,
    onInstall: (file: File, tagName: String) -> Unit
) {
    when (state) {
        is UpdaterUiState.Available -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pembaruan Tersedia") },
            text = { Text("Versi baru (${state.release.tagName}) siap diunduh, ukuran ${formatSize(state.asset.size)}.") },
            confirmButton = {
                TextButton(onClick = { onStartDownload(state.asset, state.release.tagName) }) { Text("Unduh & Pasang") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Nanti") } }
        )

        is UpdaterUiState.NoMatchingAsset -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pembaruan Tersedia") },
            text = { Text("Rilis ${state.release.tagName} ditemukan, tapi tidak ada APK yang cocok dengan arsitektur perangkat ini.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )

        is UpdaterUiState.Downloading -> AlertDialog(
            onDismissRequest = {}, // non-dismissable selama unduhan berjalan
            title = { Text("Mengunduh Pembaruan") },
            text = {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    if (state.totalBytes > 0) {
                        val percent = ((state.bytesRead * 100) / state.totalBytes).toInt()
                        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "$percent% · ${formatSize(state.bytesRead)} / ${formatSize(state.totalBytes)}",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {}
        )

        is UpdaterUiState.ReadyToInstall -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Siap Dipasang") },
            text = { Text("Pembaruan sudah selesai diunduh. Lanjutkan proses instalasi?") },
            confirmButton = {
                TextButton(onClick = { onInstall(state.file, state.tagName) }) { Text("Pasang") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
        )

        is UpdaterUiState.UpToDate -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sudah Terbaru") },
            text = { Text("Kamu sudah memakai versi terbaru Jotter.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )

        is UpdaterUiState.CheckError -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Gagal Memeriksa Pembaruan") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )

        is UpdaterUiState.DownloadError -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Gagal Mengunduh") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )

        UpdaterUiState.Idle, UpdaterUiState.Checking -> Unit
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return "%.1f %s".format(value, units[unitIndex])
}
