package com.jotter.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.jotter.notes.data.AppDatabase
import com.jotter.notes.notification.ReminderScheduler
import com.jotter.notes.ui.navigation.JotterNavGraph
import com.jotter.notes.ui.theme.JotterTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// FragmentActivity (not plain ComponentActivity/Activity) is required for BiometricPrompt to
// host correctly - same lesson learned from the Flutter build (FlutterFragmentActivity), still
// applies at the native layer.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Reschedule-on-app-open: AlarmManager entries are cleared by the OS on every reboot.
        // A true instant-on-boot BroadcastReceiver is a pending follow-up (see PROJECT_STATE.md);
        // this covers the common case (reminders re-arm next time the app is opened).
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(applicationContext).noteDao()
            val notes = dao.observeWithReminders().first()
            notes.forEach { ReminderScheduler.schedule(applicationContext, it) }
        }

        setContent {
            JotterTheme {
                JotterNavGraph()
            }
        }
    }
}
