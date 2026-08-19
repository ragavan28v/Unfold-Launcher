package com.unfold.core.ui.iconpack

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.util.Xml
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Locale

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val drawableCount: Int
)

class IconPackResolver private constructor() {
    companion object {
        private val iconCache = LruCache<String, Drawable>(200)
        private val filterCache = mutableMapOf<String, Map<String, String>>()
        private var lastIconPack = ""

        fun detectInstalledIconPacks(context: Context): List<IconPackInfo> {
            val pm = context.packageManager
            return pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { applicationInfo ->
                    val packageName = applicationInfo.packageName
                    if (packageName == context.packageName) return@filter false
                    
                    val resources = runCatching { pm.getResourcesForApplication(applicationInfo) }.getOrNull()
                    resources != null && (
                        resources.getIdentifier("appfilter", "xml", packageName) != 0 ||
                        resources.getIdentifier("iconpack", "xml", packageName) != 0
                    )
                }
                .map { applicationInfo ->
                    val label = applicationInfo.loadLabel(pm).toString().ifBlank { applicationInfo.packageName }
                    val resources = pm.getResourcesForApplication(applicationInfo)
                    val mapping = getAppFilter(resources, applicationInfo.packageName)
                    IconPackInfo(
                        packageName = applicationInfo.packageName,
                        label = label,
                        drawableCount = mapping.size
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }

        fun getSelectedIconPack(context: Context): String {
            val themeFile = File(context.filesDir, "datastore/theme_config.json")
            if (!themeFile.exists()) return ""
            return runCatching {
                JSONObject(themeFile.readText()).optString("icon_pack_package", "")
            }.getOrDefault("")
        }

        fun resolveAppIconDrawable(context: Context, packageName: String, iconPackPackage: String? = null): Drawable? {
            val requestedPack = iconPackPackage ?: getSelectedIconPack(context)
            
            if (requestedPack != lastIconPack) {
                iconCache.evictAll()
                lastIconPack = requestedPack
            }

            if (requestedPack.isNotBlank()) {
                val cacheKey = "${requestedPack}_${packageName}"
                iconCache.get(cacheKey)?.let { return it }

                val candidate = resolveFromPack(context, packageName, requestedPack)
                if (candidate != null) {
                    iconCache.put(cacheKey, candidate)
                    return candidate
                }
            }
            
            val systemCacheKey = "system_${packageName}"
            iconCache.get(systemCacheKey)?.let { return it }
            val systemIcon = runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
            if (systemIcon != null) {
                iconCache.put(systemCacheKey, systemIcon)
            }
            return systemIcon
        }

        fun buildResourceCandidates(packageName: String): List<String> {
            val normalized = packageName
                .replace(".", "_")
                .replace("-", "_")
            val shortName = packageName.substringAfterLast('.')
            return linkedSetOf(
                normalized,
                "icon_${normalized}",
                "ic_${normalized}",
                "app_icon_${normalized}",
                shortName,
                "icon_${shortName}",
                "ic_${shortName}",
                "app_icon_${shortName}"
            ).toList()
        }

        private fun resolveFromPack(context: Context, packageName: String, iconPackPackage: String): Drawable? {
            val pm = context.packageManager
            val appInfo = runCatching { pm.getApplicationInfo(iconPackPackage, PackageManager.GET_META_DATA) }.getOrNull()
                ?: return null
            val resources = runCatching { pm.getResourcesForApplication(appInfo) }.getOrNull() ?: return null
            val mapping = getAppFilter(resources, iconPackPackage)
            
            val directEntry = mapping[packageName]
                ?: mapping[packageName.substringAfterLast('.')]
                ?: mapping["com.${packageName.substringAfterLast('.')}"]
            
            val candidates = mutableListOf<String>()
            if (directEntry != null) candidates += directEntry
            candidates += buildResourceCandidates(packageName)

            for (candidate in candidates.distinct()) {
                val drawableId = resources.getIdentifier(candidate, "drawable", iconPackPackage)
                    .takeIf { it != 0 }
                    ?: resources.getIdentifier(candidate, "mipmap", iconPackPackage)
                        .takeIf { it != 0 }
                if (drawableId != null && drawableId != 0) {
                    return runCatching { resources.getDrawable(drawableId, context.theme) }.getOrNull()
                }
            }
            return null
        }

        private fun getAppFilter(resources: Resources, packageName: String): Map<String, String> {
            filterCache[packageName]?.let { return it }
            val mapping = parseAppFilter(resources, packageName)
            filterCache[packageName] = mapping
            return mapping
        }

        private fun parseAppFilter(resources: Resources, packageName: String): Map<String, String> {
            val appFilterId = resources.getIdentifier("appfilter", "xml", packageName)
            if (appFilterId == 0) return emptyMap()

            val map = linkedMapOf<String, String>()
            val parser = resources.getXml(appFilterId)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                        // Handle "ComponentInfo{com.android.settings/com.android.settings.Settings}"
                        val pkg = if (component.startsWith("ComponentInfo{")) {
                            component.substringAfter("{").substringBefore("/")
                        } else if (component.contains("/")) {
                            component.substringBefore("/")
                        } else {
                            component
                        }
                        map[pkg] = drawable
                    }
                }
                eventType = parser.next()
            }
            return map
        }
    }
}
