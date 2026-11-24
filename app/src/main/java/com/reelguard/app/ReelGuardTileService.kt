package com.reelguard.app

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Quick Settings tile for switching ReelGuard off before paying. Android only
 * lets the user turn an AccessibilityService back on, so an off tile opens the
 * Accessibility settings page.
 */
class ReelGuardTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        setTileActive(ServiceStatus.isEnabled(this))
    }

    override fun onClick() {
        super.onClick()
        if (!ServiceStatus.isEnabled(this)) {
            openAccessibilitySettings()
            return
        }

        if (ShareGuardService.requestPause()) {
            Toast.makeText(this, R.string.tile_paused_toast, Toast.LENGTH_SHORT).show()
            setTileActive(false)
        } else {
            openAccessibilitySettings()
        }
    }

    private fun setTileActive(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (active) R.string.tile_subtitle_on else R.string.tile_subtitle_off,
            )
            tile.contentDescription = getString(
                if (active) R.string.tile_content_on else R.string.tile_content_off,
            )
        }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.settings_error, Toast.LENGTH_LONG).show()
        }
    }
}
