package com.jotter.notes.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AuthManager(context: Context) {
    private val appContext = context.applicationContext

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        "jotter_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasPinSet(): Boolean = prefs.contains("pin_hash")

    fun setPin(pin: String) {
        val salt = generateSalt()
        prefs.edit().putString("pin_salt", salt).putString("pin_hash", hashPin(pin, salt)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString("pin_salt", null) ?: return false
        val stored = prefs.getString("pin_hash", null) ?: return false
        return hashPin(pin, salt) == stored
    }

    fun clearPin() {
        prefs.edit().remove("pin_hash").remove("pin_salt").remove("biometric_enabled").apply()
    }

    fun isBiometricPreferenceEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)

    fun setBiometricPreference(value: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", value).apply()
    }

    fun canUseBiometrics(): Boolean {
        val manager = BiometricManager.from(appContext)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticateBiometric(activity: FragmentActivity): Boolean = suspendCancellableCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(true)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (cont.isActive) cont.resume(false)
            }
            override fun onAuthenticationFailed() {
                // allow retry, do not resume yet
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verifikasi untuk membuka catatan terkunci")
            .setNegativeButtonText("Batal")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
