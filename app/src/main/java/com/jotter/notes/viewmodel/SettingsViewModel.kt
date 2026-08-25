package com.jotter.notes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jotter.notes.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthManager(application)

    private val _isLockEnabled = MutableStateFlow(auth.hasPinSet())
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled

    private val _isBiometricEnabled = MutableStateFlow(auth.isBiometricPreferenceEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    fun refresh() {
        _isLockEnabled.value = auth.hasPinSet()
        _isBiometricEnabled.value = auth.isBiometricPreferenceEnabled()
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
