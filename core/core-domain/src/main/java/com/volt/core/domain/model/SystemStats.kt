package com.volt.core.domain.model

data class SystemStats(
    val ramUsedPercent: Float,
    val ramUsedText: String,
    val storageUsedPercent: Float,
    val storageUsedText: String,
    val batteryPercent: Float,
    val batteryText: String,
    val cpuTemp: Float,
    val cpuTempText: String
)
