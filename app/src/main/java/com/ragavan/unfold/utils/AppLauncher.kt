package com.ragavan.unfold.utils

import android.content.Context

object AppLauncher {

    fun launch(
        context: Context,
        packageName: String
    ) {

        val launchIntent =
            context.packageManager
                .getLaunchIntentForPackage(packageName)

        launchIntent?.let {

            context.startActivity(it)

        }

    }

}