package com.ragavan.unfold.ui.components.dock

import com.ragavan.unfold.data.apps.AppInfo

data class DockState(

    val apps: List<AppInfo>,

    val selectedPackage: String? = null

)