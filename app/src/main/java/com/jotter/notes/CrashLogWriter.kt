package com.jotter.notes

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.PrintWriter
import java.io.StringWriter

object CrashLogWriter {
    private const val MAX_LOGS = 50

    fun write(context: Context, appName: String, fileName: String, content: String) {
        try {
            val relativePath = Environment.DIRECTORY_DOCUMENTS + "/" + appName + "/logs"
            val resolver = context.applicationContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(content.toByteArray())
                }
            }
            enforceRetention(context, relativePath)
        } catch (e: Exception) {
            // fail-safe: never throw from the crash logger
        }
    }

    fun writeThrowable(context: Context, appName: String, throwable: Throwable, threadName: String, source: String = "native") {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val now = System.currentTimeMillis()
        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(now)
        val uuid = java.util.UUID.randomUUID().toString()
        val fileName = "crash_${ts}_$uuid.txt"
        val content = buildString {
            appendLine("OS: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Timestamp: $now")
            appendLine("Thread: $threadName")
            appendLine("Source: $source")
            appendLine("---")
            appendLine("Error: ${throwable.message}")
            appendLine()
            appendLine("StackTrace:")
            appendLine(sw.toString())
        }
        write(context, appName, fileName, content)
    }

    private fun enforceRetention(context: Context, relativePath: String) {
        val resolver = context.applicationContext.contentResolver
        val uriExternal = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf("$relativePath/")
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} ASC"
        resolver.query(uriExternal, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val count = cursor.count
            if (count > MAX_LOGS) {
                var deleted = 0
                val toDelete = count - MAX_LOGS
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext() && deleted < toDelete) {
                    val id = cursor.getLong(idColumn)
                    resolver.delete(MediaStore.Files.getContentUri("external", id), null, null)
                    deleted++
                }
            }
        }
    }
}
