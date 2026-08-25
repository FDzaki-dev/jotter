package com.jotter.notes.updater

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Satu aset APK dalam rilis GitHub. CI (release.yml) selalu upload hasil ABI split dengan pola
 * nama `app-<abi>-release.apk` (mis. app-armeabi-v7a-release.apk, app-arm64-v8a-release.apk,
 * app-x86_64-release.apk) — lihat `files: app/build/outputs/apk/release/app-*-release.apk`.
 */
data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

/** Info rilis terbaru dari GitHub Releases API (`/releases/latest`). */
data class ReleaseInfo(
    val tagName: String,
    val assets: List<ReleaseAsset>
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo, val asset: ReleaseAsset) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class NoMatchingAsset(val release: ReleaseInfo) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Cek rilis terbaru repo GitHub `FDzaki-dev/jotter`, lalu bandingkan `tag_name` vs tag rilis
 * TERAKHIR YANG SUKSES DI-INSTALL user (disimpan lokal via [markTagAsInstalled] — WAJIB dipanggil
 * dari UI Batch10 setelah install intent selesai, bukan otomatis setelah download saja).
 *
 * Perbandingan pakai EQUALITY, BUKAN ordering numerik — tag format `build-YYYYMMDD-runnumber`,
 * run_number gak zero-padded jadi perbandingan numerik/string biasa gak aman ("9" vs "10").
 * Kalau belum pernah ada tag tersimpan (fresh install fitur ini), otomatis dianggap ada update —
 * ini disengaja & benar, karena APK manapun sebelum fitur ini ada memang lebih lama dari rilis apa pun.
 */
class UpdateChecker(context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("GitHub API HTTP ${response.code}")
                }
                val bodyString = response.body?.string()
                    ?: return@withContext UpdateCheckResult.Error("Respons rilis kosong")

                val json = JSONObject(bodyString)
                val tagName = json.getString("tag_name")
                val assetsJson = json.getJSONArray("assets")
                val assets = buildList {
                    for (i in 0 until assetsJson.length()) {
                        val a = assetsJson.getJSONObject(i)
                        val name = a.getString("name")
                        if (name.endsWith(".apk")) {
                            add(ReleaseAsset(name, a.getString("browser_download_url"), a.optLong("size", 0L)))
                        }
                    }
                }
                val release = ReleaseInfo(tagName, assets)

                val lastInstalledTag = prefs.getString(KEY_LAST_INSTALLED_TAG, null)
                if (lastInstalledTag != null && lastInstalledTag == tagName) {
                    return@withContext UpdateCheckResult.UpToDate
                }

                val matchedAsset = selectAssetForDevice(assets)
                    ?: return@withContext UpdateCheckResult.NoMatchingAsset(release)

                UpdateCheckResult.UpdateAvailable(release, matchedAsset)
            }
        } catch (e: IOException) {
            UpdateCheckResult.Error(e.message ?: "Gagal terhubung ke GitHub (periksa koneksi internet)")
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Gagal memproses data rilis GitHub")
        }
    }

    /**
     * Simpan tag rilis yang BARU SAJA sukses diinstal user. Dipanggil dari UI (Batch10) setelah
     * install intent dikonfirmasi selesai — BUKAN otomatis dipanggil setelah download selesai.
     */
    fun markTagAsInstalled(tagName: String) {
        prefs.edit().putString(KEY_LAST_INSTALLED_TAG, tagName).apply()
    }

    /**
     * Pilih aset APK sesuai ABI device, urut prioritas [Build.SUPPORTED_ABIS] (ABI[0] = paling
     * disukai perangkat). Nama aset dari CI selalu mengandung string ABI persis.
     */
    private fun selectAssetForDevice(assets: List<ReleaseAsset>): ReleaseAsset? {
        for (abi in Build.SUPPORTED_ABIS) {
            val match = assets.firstOrNull { it.name.contains(abi) }
            if (match != null) return match
        }
        return null
    }

    companion object {
        private const val REPO = "FDzaki-dev/jotter"
        private const val PREFS_NAME = "jotter_updater_prefs"
        private const val KEY_LAST_INSTALLED_TAG = "last_installed_tag"
    }
}
