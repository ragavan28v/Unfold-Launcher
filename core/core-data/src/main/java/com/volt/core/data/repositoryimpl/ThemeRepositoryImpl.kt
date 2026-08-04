package com.volt.core.data.repositoryimpl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import com.volt.core.domain.model.ThemeConfig
import com.volt.core.domain.repository.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

object ThemeConfigSerializer : Serializer<ThemeConfig> {
    override val defaultValue: ThemeConfig = ThemeConfig()

    override suspend fun readFrom(input: InputStream): ThemeConfig {
        return try {
            val jsonStr = input.bufferedReader().use { it.readText() }
            if (jsonStr.isEmpty()) return defaultValue
            val json = JSONObject(jsonStr)
            ThemeConfig(
                accentPrimaryHex = json.optString("accent_primary_hex", "#38BDF8"),
                accentSecondaryHex = json.optString("accent_secondary_hex", "#6366F1"),
                bevelIntensity = json.optDouble("bevel_intensity", 0.6).toFloat(),
                blurRadiusDp = json.optDouble("blur_radius_dp", 24.0).toFloat(),
                panelOpacity = json.optDouble("panel_opacity", 0.72).toFloat(),
                timeAdaptiveHue = json.optBoolean("time_adaptive_hue", true),
                reducedMotion = json.optBoolean("reduced_motion", false),
                gridColumns = json.optInt("grid_columns", 4),
                gridRows = json.optInt("grid_rows", 6),
                iconPackPackage = json.optString("icon_pack_package", ""),
                soundFeedbackEnabled = json.optBoolean("sound_feedback_enabled", false)
            )
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: ThemeConfig, output: OutputStream) {
        val json = JSONObject().apply {
            put("accent_primary_hex", t.accentPrimaryHex)
            put("accent_secondary_hex", t.accentSecondaryHex)
            put("bevel_intensity", t.bevelIntensity.toDouble())
            put("blur_radius_dp", t.blurRadiusDp.toDouble())
            put("panel_opacity", t.panelOpacity.toDouble())
            put("time_adaptive_hue", t.timeAdaptiveHue)
            put("reduced_motion", t.reducedMotion)
            put("grid_columns", t.gridColumns)
            put("grid_rows", t.gridRows)
            put("icon_pack_package", t.iconPackPackage)
            put("sound_feedback_enabled", t.soundFeedbackEnabled)
        }
        withContext(Dispatchers.IO) {
            output.write(json.toString().toByteArray())
        }
    }
}


@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    private val dataStore: DataStore<ThemeConfig> = DataStoreFactory.create(
        serializer = ThemeConfigSerializer,
        produceFile = { File(context.filesDir, "datastore/theme_config.json") }
    )

    override fun observeTheme(): Flow<ThemeConfig> {
        return dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(ThemeConfig())
            } else {
                throw exception
            }
        }
    }

    override suspend fun updateTheme(config: ThemeConfig) {
        dataStore.updateData { config }
    }
}
