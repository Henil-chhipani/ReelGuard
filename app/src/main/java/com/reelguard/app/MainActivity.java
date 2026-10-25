package com.reelguard.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int BG = Color.parseColor("#F8FAFC");
    private static final int CARD = Color.parseColor("#FFFFFF");
    private static final int TEXT = Color.parseColor("#0F172A");
    private static final int MUTED = Color.parseColor("#64748B");
    private static final int GREEN = Color.parseColor("#16A34A");
    private static final int RED = Color.parseColor("#DC2626");

    private TextView statusView;
    private TextView statusDetailView;
    private TextView todayCountView;
    private TextView totalCountView;
    private Button enableButton;
    private Switch toastSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(24), dp(32), dp(24), dp(32));
        scroll.addView(column, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("ReelGuard");
        title.setTextSize(32);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT);
        column.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Stops Instagram's share sheet before a reel leaves your phone.");
        subtitle.setTextSize(15);
        subtitle.setTextColor(MUTED);
        subtitle.setPadding(0, dp(6), 0, dp(24));
        column.addView(subtitle);

        LinearLayout statusCard = card();
        column.addView(statusCard);

        statusView = new TextView(this);
        statusView.setTextSize(19);
        statusView.setTypeface(Typeface.DEFAULT_BOLD);
        statusCard.addView(statusView);

        statusDetailView = new TextView(this);
        statusDetailView.setTextSize(13);
        statusDetailView.setTextColor(MUTED);
        statusDetailView.setPadding(0, dp(4), 0, dp(14));
        statusCard.addView(statusDetailView);

        enableButton = new Button(this);
        enableButton.setAllCaps(false);
        enableButton.setTextSize(15);
        enableButton.setBackgroundTintList(ColorStateList.valueOf(GREEN));
        enableButton.setTextColor(Color.WHITE);
        enableButton.setOnClickListener(v -> openAccessibilitySettings());
        statusCard.addView(enableButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        LinearLayout counterCard = card();
        counterCard.setOrientation(LinearLayout.HORIZONTAL);
        counterCard.setGravity(android.view.Gravity.CENTER_VERTICAL);
        counterCard.setPadding(dp(20), dp(16), dp(20), dp(16));
        column.addView(counterCard, cardParams());

        LinearLayout counts = new LinearLayout(this);
        counts.setOrientation(LinearLayout.VERTICAL);
        counts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView todayLabel = new TextView(this);
        todayLabel.setText("BLOCKED TODAY");
        todayLabel.setTextSize(11);
        todayLabel.setLetterSpacing(0.08f);
        todayLabel.setTextColor(MUTED);
        counts.addView(todayLabel);

        todayCountView = new TextView(this);
        todayCountView.setTextSize(34);
        todayCountView.setTypeface(Typeface.DEFAULT_BOLD);
        todayCountView.setTextColor(TEXT);
        counts.addView(todayCountView);

        totalCountView = new TextView(this);
        totalCountView.setTextSize(13);
        totalCountView.setTextColor(MUTED);
        counts.addView(totalCountView);

        counterCard.addView(counts);

        Button resetButton = new Button(this);
        resetButton.setAllCaps(false);
        resetButton.setText("Reset");
        resetButton.setTextSize(13);
        resetButton.setOnClickListener(v -> {
            Prefs.reset(this);
            refresh();
            Toast.makeText(this, "Counter reset", Toast.LENGTH_SHORT).show();
        });
        counterCard.addView(resetButton);

        LinearLayout optionsCard = card();
        column.addView(optionsCard, cardParams());

        LinearLayout toastRow = new LinearLayout(this);
        toastRow.setOrientation(LinearLayout.VERTICAL);

        toastSwitch = new Switch(this);
        toastSwitch.setText("Show a \"Share blocked\" toast");
        toastSwitch.setTextSize(15);
        toastSwitch.setTextColor(TEXT);
        toastSwitch.setChecked(Prefs.toastEnabled(this));
        toastSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                Prefs.setToastEnabled(this, isChecked));
        toastRow.addView(toastSwitch);
        optionsCard.addView(toastRow);

        TextView note = new TextView(this);
        note.setText("How it works\n"
                + "Turn ReelGuard on in Accessibility. When Instagram's share sheet appears, "
                + "ReelGuard closes it immediately and counts the save.\n\n"
                + "If Android blocks the toggle (sideloaded apps on Android 13+), open "
                + "App info > three-dot menu > Allow restricted settings, then try again.\n\n"
                + "No internet permission. Nothing leaves your phone. Instagram updates can "
                + "change its screens, so a future update may stop detection from working.");
        note.setTextSize(13);
        note.setTextColor(MUTED);
        note.setLineSpacing(dp(4), 1f);
        note.setPadding(0, dp(20), 0, 0);
        column.addView(note);

        return scroll;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD);
        background.setCornerRadius(dp(18));
        card.setBackground(background);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(14);
        return params;
    }

    private void refresh() {
        boolean enabled = isServiceEnabled();
        if (enabled) {
            statusView.setText("Active");
            statusView.setTextColor(GREEN);
            statusDetailView.setText("ReelGuard is watching Instagram and will close the share sheet.");
            enableButton.setText("Accessibility settings");
            enableButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
            enableButton.setTextColor(TEXT);
        } else {
            statusView.setText("Off");
            statusView.setTextColor(RED);
            statusDetailView.setText("Turn on ReelGuard in Accessibility to start blocking shares.");
            enableButton.setText("Enable ReelGuard");
            enableButton.setBackgroundTintList(ColorStateList.valueOf(GREEN));
            enableButton.setTextColor(Color.WHITE);
        }

        todayCountView.setText(String.valueOf(Prefs.todayCount(this)));
        totalCountView.setText("All time: " + Prefs.total(this));
        toastSwitch.setChecked(Prefs.toastEnabled(this));
    }

    private boolean isServiceEnabled() {
        int enabled = 0;
        try {
            enabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException ignored) {
        }
        if (enabled != 1) {
            return false;
        }

        String services = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (services == null) {
            return false;
        }

        String me = getPackageName() + "/" + ShareGuardService.class.getName();
        for (String service : services.split(":")) {
            if (service.equalsIgnoreCase(me)) {
                return true;
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Accessibility settings", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
