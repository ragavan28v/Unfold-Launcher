package com.ragavan.unfold.data.dock

import android.content.Context
import com.ragavan.unfold.data.apps.AppInfo

class DockRepository(
    private val context: Context
) {

    fun createDefaultDock(
        apps: List<AppInfo>
    ): DockLayout {

        return DockLayout(

            items = apps
                .take(4)
                .mapIndexed { index, app ->

                    DockItem(

                        packageName = app.packageName,

                        position = index

                    )

                }

        )

    }

}