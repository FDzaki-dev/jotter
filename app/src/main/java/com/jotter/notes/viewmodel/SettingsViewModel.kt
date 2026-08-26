package com.jotter.notes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jotter.notes.auth.AuthManager
import com.jotter.notes.updater.DownloadResult
import com.jotter.notes.updater.ReleaseAsset
import com.jotter.notes.updater.ReleaseInfo
import com.jotter.notes.updater.ReleaseDownloader
import com.jotter.notes.updater.UpdateChecker
import com.jotter.notes.updater.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * State UI utk alur in-app updater (Batch10 — wiring UI dari logic Batch9 di
 * `updater/UpdateChecker.kt` & `updater/ReleaseDownloader.kt`). Idle/Checking sengaja TIDAK
 * memicu dialog (lihat `UpdateDialog.kt`) — Checking ditampilkan sbg spinner inline di baris
 * "Cek Pembaruan" milik SettingsScreen.
 */
sealed class UpdaterUiState {
    data object Idle : UpdaterUiState()
    data object Checking : UpdaterUiState()
    data object UpToDate : UpdaterUiState()
    data class Available(val release: ReleaseInfo, val asset: ReleaseAsset) : UpdaterUiState()
    data class NoMatchingAsset(val release: ReleaseInfo) : UpdaterUiState()
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : UpdaterUiState()
    data class ReadyToInstall(val file: File, val tagName: String) : UpdaterUiState()
    data class CheckError(val message: String) : UpdaterUiState()
    data class DownloadError(val message: String) : UpdaterUiState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthManager(application)
    private val updateChecker = UpdateChecker(application)
    private val downloader = ReleaseDownloader(application)

    private val _isLockEnabled = MutableStateFlow(auth.hasPinSet())
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled

    private val _isBiometricEnabled = MutableStateFlow(auth.isBiometricPreferenceEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    private val _updaterState = MutableStateFlow<UpdaterUiState>(UpdaterUiState.Idle)
    val updaterState: StateFlow<UpdaterUiState> = _updaterState

    fun refresh() {
        _isLockEnabled.value = auth.hasPinSet()
        _isBiometricEnabled.value = auth.isBiometricPreferenceEnabled()
    }

    /** Dipicu tombol "Cek Pembaruan" di SettingsScreen. No-op kalau sedang cek/unduh. */
    fun checkForUpdate() {
        if (_updaterState.value is UpdaterUiState.Checking || _updaterState.value is UpdaterUiState.Downloading) return
        _updaterState.value = UpdaterUiState.Checking
        viewModelScope.launch {
            _updaterState.value = when (val result = updateChecker.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> UpdaterUiState.Available(result.release, result.asset)
                is UpdateCheckResult.UpToDate -> UpdaterUiState.UpToDate
                is UpdateCheckResult.NoMatchingAsset -> UpdaterUiState.NoMatchingAsset(result.release)
                is UpdateCheckResult.Error -> UpdaterUiState.CheckError(result.message)
            }
        }
    }

    /**
     * Dipicu tombol "Unduh & Pasang" di [UpdateDialog]. Progres [ReleaseDownloader] dipost
     * LANGSUNG ke StateFlow dari thread IO — aman krn `MutableStateFlow.value` thread-safe
     * (sesuai kontrak yg dicatat di ReleaseDownloader.kt), tapi dithrottle per persen bulat
     * (bukan tiap chunk 8KB) biar recomposition tidak banjir untuk file puluhan MB.
     */
    fun startDownload(asset: ReleaseAsset, tagName: String) {
        _updaterState.value = UpdaterUiState.Downloading(0L, 0L)
        viewModelScope.launch {
            var lastPercent = -1
            val result = downloader.download(asset) { bytesRead, totalBytes ->
                val percent = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else -1
                if (percent != lastPercent) {
                    lastPercent = percent
                    _updaterState.value = UpdaterUiState.Downloading(bytesRead, totalBytes)
                }
            }
            _updaterState.value = when (result) {
                is DownloadResult.Success -> UpdaterUiState.ReadyToInstall(result.file, tagName)
                is DownloadResult.Error -> UpdaterUiState.DownloadError(result.message)
            }
        }
    }

    /**
     * Dipanggil SettingsScreen setelah install intent (ACTION_VIEW APK via FileProvider) SUKSES
     * DIMULAI (`startActivity` tidak throw) — bukan menunggu instalasi benar2 selesai di OS
     * (butuh broadcast receiver terpisah di luar scope batch ini). Konsisten dgn desain
     * `markTagAsInstalled()` di UpdateChecker.kt: sengaja tidak dipanggil otomatis setelah
     * download saja.
     */
    fun markInstalled(tagName: String) {
        updateChecker.markTagAsInstalled(tagName)
        _updaterState.value = UpdaterUiState.Idle
    }

    /** Tutup dialog updater. Diabaikan selama Downloading (non-dismissable, sesuai UpdateDialog). */
    fun dismissUpdaterDialog() {
        if (_updaterState.value !is UpdaterUiState.Downloading) {
            _updaterState.value = UpdaterUiState.Idle
        }
    }

    fun enableLock(pin: String) {
        auth.setPin(pin)
        refresh()
    }

    fun disableLock() {
        auth.clearPin()
        refresh()
    }

    fun setBiometricEnabled(value: Boolean) {
        auth.setBiometricPreference(value)
        refresh()
    }
}
