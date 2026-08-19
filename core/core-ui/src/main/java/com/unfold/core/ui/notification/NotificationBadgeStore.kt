package com.unfold.core.ui.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationBadgeStore {
    private const val PREFS_NAME = "notification_badges"
    private const val RECORDS_KEY = "records"
    private val records = linkedMapOf<String, BadgeRecord>()
    private val badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private var preferences: android.content.SharedPreferences? = null

    val counts: StateFlow<Map<String, Int>> = badgeCounts.asStateFlow()

    fun initialize(context: Context) {
        synchronized(this) {
            if (preferences != null) return
            preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = preferences?.getString(RECORDS_KEY, null)
            if (!stored.isNullOrBlank()) {
                runCatching {
                    val array = JSONArray(stored)
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        records[item.getString("key")] = BadgeRecord(
                            instanceKey = item.getString("instance"),
                            notificationKey = item.getString("notification")
                        )
                    }
                }
            }
            publish()
        }
    }

    fun recordNotification(notificationKey: String, instanceKey: String) {
        if (notificationKey.isBlank() || instanceKey.isBlank()) return
        synchronized(this) {
            records[notificationKey] = BadgeRecord(instanceKey, notificationKey)
            persist()
            publish()
        }
    }

    fun clearInstance(instanceKey: String) {
        if (instanceKey.isBlank()) return
        synchronized(this) {
            records.entries.removeIf { it.value.instanceKey == instanceKey }
            persist()
            publish()
        }
    }

    fun instanceKey(packageName: String, userSerial: Long): String = "$packageName@$userSerial"

    private fun publish() {
        badgeCounts.value = records.values
            .groupingBy { it.instanceKey }
            .eachCount()
    }

    private fun persist() {
        val array = JSONArray()
        records.forEach { (notificationKey, record) ->
            array.put(JSONObject().apply {
                put("key", notificationKey)
                put("instance", record.instanceKey)
                put("notification", record.notificationKey)
            })
        }
        preferences?.edit()?.putString(RECORDS_KEY, array.toString())?.apply()
    }

    private data class BadgeRecord(
        val instanceKey: String,
        val notificationKey: String
    )
}
