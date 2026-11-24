package com.reelguard.app

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

internal object Prefs {

    private const val FILE = "reelguard"
    private const val KEY_TOTAL = "blocks_total"
    private const val KEY_TODAY = "blocks_today"
    private const val KEY_DAY = "blocks_day"
    private const val KEY_TOAST = "toast_enabled"
    private const val KEY_JOURNEY_START = "journey_started_at"
    private const val KEY_LAST_ATTEMPT = "last_attempt_at"
    private const val KEY_BEST_STREAK = "best_streak_ms"
    private const val KEY_COMMENT_TOTAL = "comments_total"
    private const val KEY_COMMENT_TODAY = "comments_today"
    private const val KEY_COMMENT_DAY = "comments_day"
    private const val KEY_COMMENT_ENABLED = "comment_block_enabled"

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun today(): String = LocalDate.now().toString()

    fun ensureJourneyStarted(context: Context) {
        val p = sp(context)
        if (!p.contains(KEY_JOURNEY_START)) {
            p.edit().putLong(KEY_JOURNEY_START, System.currentTimeMillis()).apply()
        }
    }

    fun journeyStartedAt(context: Context): Long =
        sp(context).getLong(KEY_JOURNEY_START, 0L)

    fun lastAttemptAt(context: Context): Long =
        sp(context).getLong(KEY_LAST_ATTEMPT, 0L)

    fun bestStreakMs(context: Context): Long =
        sp(context).getLong(KEY_BEST_STREAK, 0L)

    fun total(context: Context): Int = sp(context).getInt(KEY_TOTAL, 0)

    fun todayCount(context: Context): Int {
        val p = sp(context)
        if (today() != p.getString(KEY_DAY, "")) return 0
        return p.getInt(KEY_TODAY, 0)
    }

    fun recordBlock(context: Context) {
        val p = sp(context)
        val now = System.currentTimeMillis()
        val lastAttempt = p.getLong(KEY_LAST_ATTEMPT, 0L)
        val streakStart = if (lastAttempt > 0L) lastAttempt else journeyStartedAt(context)
        val currentStreak = if (streakStart > 0L) now - streakStart else 0L
        val bestStreak = maxOf(p.getLong(KEY_BEST_STREAK, 0L), currentStreak)
        val day = today()
        val today = if (day == p.getString(KEY_DAY, "")) p.getInt(KEY_TODAY, 0) else 0
        p.edit()
            .putString(KEY_DAY, day)
            .putInt(KEY_TODAY, today + 1)
            .putInt(KEY_TOTAL, p.getInt(KEY_TOTAL, 0) + 1)
            .putLong(KEY_LAST_ATTEMPT, now)
            .putLong(KEY_BEST_STREAK, bestStreak)
            .apply()
    }

    fun reset(context: Context) {
        sp(context).edit()
            .putString(KEY_DAY, today())
            .putInt(KEY_TODAY, 0)
            .putInt(KEY_TOTAL, 0)
            .putString(KEY_COMMENT_DAY, today())
            .putInt(KEY_COMMENT_TODAY, 0)
            .putInt(KEY_COMMENT_TOTAL, 0)
            .apply()
    }

    fun toastEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_TOAST, true)

    fun setToastEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_TOAST, enabled).apply()
    }

    fun commentTotal(context: Context): Int = sp(context).getInt(KEY_COMMENT_TOTAL, 0)

    fun commentTodayCount(context: Context): Int {
        val p = sp(context)
        if (today() != p.getString(KEY_COMMENT_DAY, "")) return 0
        return p.getInt(KEY_COMMENT_TODAY, 0)
    }

    fun recordCommentBlock(context: Context) {
        val p = sp(context)
        val day = today()
        val today = if (day == p.getString(KEY_COMMENT_DAY, "")) {
            p.getInt(KEY_COMMENT_TODAY, 0)
        } else {
            0
        }
        p.edit()
            .putString(KEY_COMMENT_DAY, day)
            .putInt(KEY_COMMENT_TODAY, today + 1)
            .putInt(KEY_COMMENT_TOTAL, p.getInt(KEY_COMMENT_TOTAL, 0) + 1)
            .apply()
    }

    fun commentBlockEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_COMMENT_ENABLED, true)

    fun setCommentBlockEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_COMMENT_ENABLED, enabled).apply()
    }
}
