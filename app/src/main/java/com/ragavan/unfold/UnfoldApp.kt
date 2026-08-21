package com.ragavan.unfold

import android.app.Application
import com.unfold.core.ui.notification.NotificationBadgeStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UnfoldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationBadgeStore.initialize(this)
    }
}

