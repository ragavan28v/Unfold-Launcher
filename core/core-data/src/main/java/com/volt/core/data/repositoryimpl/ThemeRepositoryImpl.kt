package com.volt.core.data.repositoryimpl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import com.volt.core.domain.model.AppDrawerLayoutMode
import com.volt.core.domain.model.AppDrawerSortingMode
import com.volt.core.domain.model.AppDrawerStyleMode
import com.volt.core.domain.model.DockBackgroundMode
import com.volt.core.domain.model.DockRowsMode
import com.volt.core.domain.model.HomeAppPlacementMode
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
                homeGridColumns = json.optInt("home_grid_columns", json.optInt("grid_columns", 4)),
                homeGridRows = json.optInt("home_grid_rows", json.optInt("grid_rows", 3)),
                homeIconSize = json.optInt("home_icon_size", 72),
                homeLabelsEnabled = json.optBoolean("home_labels_enabled", true),
                homeAppPlacementMode = runCatching {
                    HomeAppPlacementMode.valueOf(json.optString("home_app_placement_mode", HomeAppPlacementMode.AUTO_ARRANGE.name))
                }.getOrDefault(HomeAppPlacementMode.AUTO_ARRANGE),
                dockRowsMode = runCatching {
                    DockRowsMode.valueOf(json.optString("dock_rows_mode", DockRowsMode.ONE_ROW.name))
                }.getOrDefault(DockRowsMode.ONE_ROW),
                dockIconCount = json.optInt("dock_icon_count", 6),
                dockIconSize = json.optInt("dock_icon_size", 56),
                dockLabelsEnabled = json.optBoolean("dock_labels_enabled", true),
                dockBackgroundMode = runCatching {
                    DockBackgroundMode.valueOf(json.optString("dock_background_mode", DockBackgroundMode.DEFAULT.name))
                }.getOrDefault(DockBackgroundMode.DEFAULT),
                dockBackgroundHex = json.optString("dock_background_hex", "#12161E"),
                appDrawerLayoutMode = runCatching {
                    AppDrawerLayoutMode.valueOf(json.optString("app_drawer_layout_mode", AppDrawerLayoutMode.ALPHABETIC_GRID.name))
                }.getOrDefault(AppDrawerLayoutMode.ALPHABETIC_GRID),
                appDrawerGridRows = json.optInt("app_drawer_grid_rows", 5),
                appDrawerGridColumns = json.optInt("app_drawer_grid_columns", 4),
                appDrawerIconSize = json.optInt("app_drawer_icon_size", 64),
                appDrawerSortingMode = runCatching {
                    AppDrawerSortingMode.valueOf(json.optString("app_drawer_sorting_mode", AppDrawerSortingMode.ALPHABETICAL.name))
                }.getOrDefault(AppDrawerSortingMode.ALPHABETICAL),
                appDrawerStyleMode = runCatching {
                    AppDrawerStyleMode.valueOf(json.optString("app_drawer_style_mode", AppDrawerStyleMode.CARD.name))
                }.getOrDefault(AppDrawerStyleMode.CARD),
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
            put("home_grid_columns", t.homeGridColumns)
            put("home_grid_rows", t.homeGridRows)
            put("home_icon_size", t.homeIconSize)
            put("home_labels_enabled", t.homeLabelsEnabled)
            put("home_app_placement_mode", t.homeAppPlacementMode.name)
            put("dock_rows_mode", t.dockRowsMode.name)
            put("dock_icon_count", t.dockIconCount)
            put("dock_icon_size", t.dockIconSize)
            put("dock_labels_enabled", t.dockLabelsEnabled)
            put("dock_background_mode", t.dockBackgroundMode.name)
            put("dock_background_hex", t.dockBackgroundHex)
            put("app_drawer_layout_mode", t.appDrawerLayoutMode.name)
            put("app_drawer_grid_rows", t.appDrawerGridRows)
            put("app_drawer_grid_columns", t.appDrawerGridColumns)
            put("app_drawer_icon_size", t.appDrawerIconSize)
            put("app_drawer_sorting_mode", t.appDrawerSortingMode.name)
            put("app_drawer_style_mode", t.appDrawerStyleMode.name)
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

    override suspend fun resetTheme() {
        dataStore.updateData { ThemeConfig() }
    }
}
