package com.ragavan.unfold.data.layout

import android.content.Context
import com.ragavan.unfold.data.apps.AppInfo

class HomeLayoutRepository(
    private val context: Context
) {

    fun createDefaultLayout(
        apps: List<AppInfo>
    ): HomeLayout {

        val items = apps
            .take(12)
            .mapIndexed { index, app ->

                HomeItem(

                    packageName = app.packageName,

                    page = 0,

                    row = index / 4,

                    column = index % 4

                )

            }

        return HomeLayout(items)

    }

}