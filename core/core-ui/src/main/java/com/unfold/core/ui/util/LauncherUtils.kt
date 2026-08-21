package com.unfold.core.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserManager
import android.util.Log
import com.unfold.core.domain.model.AppInfo

object LauncherUtils {
    private const val TAG = "LauncherUtils"

    fun launchApp(context: Context, app: AppInfo) {
        try {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            val userManager = context.getSystemService(UserManager::class.java)
            val userHandle = userManager?.getUserForSerialNumber(app.userSerial)
            
            if (launcherApps != null && userHandle != null && app.activityName.isNotBlank()) {
                launcherApps.startMainActivity(
                    ComponentName(app.packageName, app.activityName),
                    userHandle,
                    null,
                    null
                )
                return
            }

            // Fallback to launch intent for current user
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                Log.w(TAG, "No launch intent found for ${app.packageName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: ${app.packageName}", e)
        }
    }
}
