package com.ragavan.unfold.viewmodel.state

import com.ragavan.unfold.data.apps.AppInfo
import com.ragavan.unfold.data.layout.HomeLayout
import com.ragavan.unfold.data.dock.DockLayout

data class HomeState(

    val layout: HomeLayout,

    val pinnedApps: List<AppInfo>,

    val dockLayout: DockLayout,

    val installedApps: List<AppInfo>

)