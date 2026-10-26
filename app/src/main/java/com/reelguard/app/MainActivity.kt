package com.reelguard.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.reelguard.app.ui.MainScreen
import com.reelguard.app.ui.UiState
import com.reelguard.app.ui.theme.ReelGuardTheme

class MainActivity : ComponentActivity() {

    private var state by mutableStateOf(UiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelGuardTheme {
                MainScreen(
                    state = state,
                    onToggleToast = ::setToastEnabled,
                    onReset = {
                        Prefs.reset(this)
                        refresh()
                    },
                    onOpenAccessibility = ::openAccessibilitySettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        val serviceEnabled = isServiceEnabled()
        if (serviceEnabled) {
            Prefs.ensureJourneyStarted(this)
        }

        val journeyStart = Prefs.journeyStartedAt(this)
        val lastAttempt = Prefs.lastAttemptAt(this)
        val currentStart = when {
            lastAttempt > 0L -> lastAttempt
            journeyStart > 0L -> journeyStart
            else -> 0L
        }
        val currentStreak = if (currentStart > 0L) {
            (now - currentStart).coerceAtLeast(0L)
        } else {
            0L
        }
        val bestStreak = maxOf(Prefs.bestStreakMs(this), currentStreak)

        state = UiState(
            serviceEnabled = serviceEnabled,
            todayCount = Prefs.todayCount(this),
            totalCount = Prefs.total(this),
            toastEnabled = Prefs.toastEnabled(this),
            versionName = BuildConfig.VERSION_NAME,
            journeyStarted = currentStart > 0L,
            streakText = if (currentStart > 0L) StreakFormatter.duration(currentStreak) else "",
            sinceText = if (journeyStart > 0L) StreakFormatter.since(journeyStart, now) else "",
            bestStreakText = if (lastAttempt > 0L) StreakFormatter.duration(bestStreak) else null,
        )
    }

    private fun setToastEnabled(enabled: Boolean) {
        Prefs.setToastEnabled(this, enabled)
        state = state.copy(toastEnabled = enabled)
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = runCatching {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        }.getOrDefault(0)
        if (enabled != 1) return false

        val services = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val me = "$packageName/${ShareGuardService::class.java.name}"
        return services.split(':').any { it.equals(me, ignoreCase = true) }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.settings_error, Toast.LENGTH_LONG).show()
        }
    }
}
