package com.ragavan.unfold.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppRepository(private val context: Context) {

    fun getInstalledApps(): List<AppInfo> {

        val packageManager = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        return apps.map {

            AppInfo(
                name = it.loadLabel(packageManager).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(packageManager)
            )

        }.sortedBy { app ->
            app.name.lowercase()
        }

    }
}