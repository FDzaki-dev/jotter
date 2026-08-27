package com.jotter.notes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val appVersion = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
            .getOrNull() ?: "—"
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                CrashLogWriter.writeThrowable(applicationContext, "Jotter", appVersion, throwable, thread.name, "native")
            } catch (e: Exception) {
                // never block the crash from propagating
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jotter_reminders", "Pengingat Catatan", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifikasi pengingat untuk catatan Jotter" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
