package com.jotter.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jotter.notes.auth.AuthManager
import com.jotter.notes.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

enum class LockMode { SETUP, VERIFY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(
    mode: LockMode,
    settingsViewModel: SettingsViewModel = viewModel(),
    onResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val auth = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mode) {
        if (mode == LockMode.VERIFY) {
            val enabled = auth.isBiometricPreferenceEnabled()
            val available = auth.canUseBiometrics()
            if (enabled && available) {
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    val ok = auth.authenticateBiometric(activity)
                    if (ok) onResult(true)
                }
            }
        }
    }

    fun onDigit(d: String) {
        error = null
        if (mode == LockMode.SETUP) {
            if (!isConfirming) {
                if (pin.length < 4) pin += d
                if (pin.length == 4) isConfirming = true
            } else {
                if (confirmPin.length < 4) confirmPin += d
                if (confirmPin.length == 4) {
                    if (pin == confirmPin) {
                        auth.setPin(pin)
                        settingsViewModel.refresh()
                        onResult(true)
                    } else {
                        error = "PIN tidak cocok, coba lagi"
                        pin = ""; confirmPin = ""; isConfirming = false
                    }
                }
            }
        } else {
            if (pin.length < 4) pin += d
            if (pin.length == 4) {
                if (auth.verifyPin(pin)) onResult(true)
                else { error = "PIN salah"; pin = "" }
            }
        }
    }

    val activePin = if (mode == LockMode.SETUP && isConfirming) confirmPin else pin
    val title = if (mode == LockMode.SETUP) (if (isConfirming) "Konfirmasi PIN" else "Buat PIN Baru") else "Masukkan PIN"

    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                repeat(4) { i ->
                    Box(
                        Modifier.padding(8.dp).size(16.dp).clip(CircleShape)
                            .background(if (i < activePin.length) MaterialTheme.colorScheme.primary else Color.DarkGray)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            error?.let { Text(it, color = Color.Red) }
            Spacer(Modifier.height(24.dp))

            val canRetryBiometric = mode == LockMode.VERIFY && auth.isBiometricPreferenceEnabled() && auth.canUseBiometrics()
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "biometric", "0", "⌫")
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.width(260.dp)) {
                items(keys) { k ->
                    if (k == "biometric") {
                        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            if (canRetryBiometric) {
                                IconButton(
                                    modifier = Modifier.size(64.dp),
                                    onClick = {
                                        scope.launch {
                                            val activity = context as? androidx.fragment.app.FragmentActivity
                                            if (activity != null && auth.authenticateBiometric(activity)) onResult(true)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "Coba sidik jari lagi",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).clickable {
                                if (k == "⌫") { if (pin.isNotEmpty()) pin = pin.dropLast(1) } else onDigit(k)
                            },
                            contentAlignment = Alignment.Center
                        ) { Text(k, style = MaterialTheme.typography.headlineLarge) }
                    }
                }
            }
        }
    }
}
