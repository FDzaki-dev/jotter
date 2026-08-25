package com.jotter.notes.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Note: alarms set with setExactAndAllowWhileIdle do not survive reboot on most OEMs.
        // A full reschedule-on-boot (re-reading all notes with future reminderAt from Room)
        // is tracked as a pending follow-up - needs an app-level Application-scoped DB read here.
    }
}
