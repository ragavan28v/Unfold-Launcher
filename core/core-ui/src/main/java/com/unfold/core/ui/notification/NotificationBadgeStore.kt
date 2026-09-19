package com.unfold.core.ui.notification

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationBadgeStore {
    private const val PREFS_NAME = "notification_badges"
    private const val INSTANCES_KEY = "instances"
    private val instances = linkedSetOf<String>()
    private val badgeInstances = MutableStateFlow<Set<String>>(emptySet())
    private var preferences: android.content.SharedPreferences? = null

    val badges: StateFlow<Set<String>> = badgeInstances.asStateFlow()

    fun initialize(context: Context) {
        synchronized(this) {
            if (preferences != null) return
            preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            instances += preferences?.getStringSet(INSTANCES_KEY, emptySet()).orEmpty()
            publish()
        }
    }

    fun recordNotification(instanceKey: String) {
        if (instanceKey.isBlank()) return
        synchronized(this) {
            instances += instanceKey
            persist()
            publish()
        }
    }

    fun clearInstance(instanceKey: String) {
        if (instanceKey.isBlank()) return
        synchronized(this) {
            instances.remove(instanceKey)
            persist()
            publish()
        }
    }

    fun instanceKey(packageName: String, userSerial: Long): String = "$packageName@$userSerial"

    private fun publish() {
        badgeInstances.value = instances.toSet()
    }

    private fun persist() {
        preferences?.edit()?.putStringSet(INSTANCES_KEY, instances)?.apply()
    }
}
