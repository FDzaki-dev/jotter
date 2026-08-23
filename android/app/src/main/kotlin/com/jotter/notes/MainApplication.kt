package com.jotter.notes

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                CrashLogWriter.writeThrowable(applicationContext, "Jotter", throwable, thread.name)
            } catch (e: Exception) {
                // never block the crash from propagating
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
