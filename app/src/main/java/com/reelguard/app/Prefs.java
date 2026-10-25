package com.reelguard.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class Prefs {

    private static final String FILE = "reelguard";
    private static final String KEY_TOTAL = "blocks_total";
    private static final String KEY_TODAY = "blocks_today";
    private static final String KEY_DAY = "blocks_day";
    private static final String KEY_TOAST = "toast_enabled";

    private Prefs() {
    }

    private static SharedPreferences sp(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static int total(Context context) {
        return sp(context).getInt(KEY_TOTAL, 0);
    }

    static int todayCount(Context context) {
        SharedPreferences p = sp(context);
        if (!today().equals(p.getString(KEY_DAY, ""))) {
            return 0;
        }
        return p.getInt(KEY_TODAY, 0);
    }

    static void recordBlock(Context context) {
        SharedPreferences p = sp(context);
        String day = today();
        int today = day.equals(p.getString(KEY_DAY, "")) ? p.getInt(KEY_TODAY, 0) : 0;
        p.edit()
                .putString(KEY_DAY, day)
                .putInt(KEY_TODAY, today + 1)
                .putInt(KEY_TOTAL, p.getInt(KEY_TOTAL, 0) + 1)
                .apply();
    }

    static void reset(Context context) {
        sp(context).edit()
                .putString(KEY_DAY, today())
                .putInt(KEY_TODAY, 0)
                .putInt(KEY_TOTAL, 0)
                .apply();
    }

    static boolean toastEnabled(Context context) {
        return sp(context).getBoolean(KEY_TOAST, true);
    }

    static void setToastEnabled(Context context, boolean enabled) {
        sp(context).edit().putBoolean(KEY_TOAST, enabled).apply();
    }
}
