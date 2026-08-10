package com.ragavan.unfold.data.preferences

import android.content.Context

class PinnedAppsRepository(
    context: Context
) {

    private val prefs =
        context.getSharedPreferences(
            "launcher_prefs",
            Context.MODE_PRIVATE
        )

    fun getPinnedPackages(): List<String> {

        return prefs.getStringSet(
            "pinned_apps",
            emptySet()
        )?.toList() ?: emptyList()

    }

    fun savePinnedPackages(
        packages: List<String>
    ) {

        prefs.edit()

            .putStringSet(
                "pinned_apps",
                packages.toSet()
            )

            .apply()

    }

    fun pinApp(
        packageName: String
    ) {

        val current =
            getPinnedPackages().toMutableList()

        if (!current.contains(packageName)) {

            current.add(packageName)

            savePinnedPackages(current)

        }

    }

    fun unpinApp(
        packageName: String
    ) {

        val current =
            getPinnedPackages().toMutableList()

        current.remove(packageName)

        savePinnedPackages(current)

    }

}