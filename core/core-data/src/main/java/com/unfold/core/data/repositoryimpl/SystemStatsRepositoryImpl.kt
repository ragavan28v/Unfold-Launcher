package com.unfold.core.data.repositoryimpl

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.unfold.core.domain.model.SystemStats
import com.unfold.core.domain.repository.SystemStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemStatsRepository {

    override fun observeStats(pollIntervalMs: Long): Flow<SystemStats> = flow {
        while (true) {
            emit(getSystemStats())
            delay(pollIntervalMs)
        }
    }.flowOn(Dispatchers.IO)

    private fun getSystemStats(): SystemStats {
        // 1. RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRam = memoryInfo.totalMem
        val availRam = memoryInfo.availMem
        val usedRam = totalRam - availRam
        val ramPercent = if (totalRam > 0) usedRam.toFloat() / totalRam.toFloat() else 0f
        val ramText = String.format("%.1f GB / %.1f GB", usedRam / 1e9, totalRam / 1e9)

        // 2. Storage
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong
        val totalStorage = totalBlocks * blockSize
        val availStorage = availBlocks * blockSize
        val usedStorage = totalStorage - availStorage
        val storagePercent = if (totalStorage > 0) usedStorage.toFloat() / totalStorage.toFloat() else 0f
        val storageText = String.format("%.1f GB / %.1f GB", usedStorage / 1e9, totalStorage / 1e9)

        // 3. Battery
        val batteryStatus: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) level.toFloat() / scale.toFloat() else 0.5f
        val batteryText = "${(batteryPct * 100).toInt()}%"

        // 4. Temp (Battery temp acts as a good fallback for CPU/system temp on standard Android)
        val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val tempText = "${temp.toInt()}°C"

        return SystemStats(
            ramUsedPercent = ramPercent,
            ramUsedText = ramText,
            storageUsedPercent = storagePercent,
            storageUsedText = storageText,
            batteryPercent = batteryPct,
            batteryText = batteryText,
            cpuTemp = temp / 100f, // normalization
            cpuTempText = tempText
        )
    }
}

