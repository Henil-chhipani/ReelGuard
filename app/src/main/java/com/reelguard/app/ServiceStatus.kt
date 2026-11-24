package com.reelguard.app

import android.content.Context
import android.provider.Settings

internal object ServiceStatus {

    fun isEnabled(context: Context): Boolean {
        val enabled = runCatching {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
            )
        }.getOrDefault(0)
        if (enabled != 1) return false

        val services = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val me = "${context.packageName}/${ShareGuardService::class.java.name}"
        return services.split(':').any { it.equals(me, ignoreCase = true) }
    }
}
