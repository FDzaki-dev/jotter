package com.jotter.notes.updater

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}

/**
 * Download APK rilis ke `cacheDir/updates/` (folder yang sama diexpose lewat FileProvider di
 * Batch8, res/xml/file_paths.xml). WAJIB streaming chunk-by-chunk pakai Okio Sink — DILARANG
 * `response.body.bytes()`/`readBytes()` yang load body utuh ke RAM (Feature Lock Anti-OOM,
 * penting karena APK rilis bisa puluhan MB).
 */
class ReleaseDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * [onProgress] dipanggil dari thread IO (bukan Main) — caller (ViewModel) tanggung jawab
     * post ke UI state (mis. StateFlow) dengan cara yang thread-safe.
     */
    suspend fun download(
        asset: ReleaseAsset,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): DownloadResult = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Bersihkan sisa file unduhan lama sebelum mulai yang baru, biar cache gak numpuk.
        updatesDir.listFiles()?.forEach { it.delete() }
        val destFile = File(updatesDir, asset.name)
        val request = Request.Builder().url(asset.downloadUrl).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult.Error("Unduhan gagal: HTTP ${response.code}")
                }
                val body = response.body
                    ?: return@withContext DownloadResult.Error("Respons unduhan kosong")
                val totalBytes = body.contentLength().takeIf { it > 0 } ?: asset.size

                body.source().use { source ->
                    destFile.sink().buffer().use { sink ->
                        var totalRead = 0L
                        var read: Long
                        while (source.read(sink.buffer, DOWNLOAD_CHUNK_BYTES).also { read = it } != -1L) {
                            totalRead += read
                            sink.emitCompleteSegments()
                            onProgress(totalRead, totalBytes)
                        }
                    }
                }
                DownloadResult.Success(destFile)
            }
        } catch (e: IOException) {
            destFile.delete()
            DownloadResult.Error(e.message ?: "Gagal mengunduh pembaruan (periksa koneksi internet)")
        } catch (e: Exception) {
            destFile.delete()
            DownloadResult.Error(e.message ?: "Gagal mengunduh pembaruan")
        }
    }

    companion object {
        private const val DOWNLOAD_CHUNK_BYTES = 8L * 1024
    }
}
