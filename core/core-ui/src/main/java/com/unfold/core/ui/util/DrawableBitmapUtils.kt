package com.unfold.core.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.unfold.core.ui.iconpack.IconPackResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_ICON_BITMAP_SIZE = 256

private val appIconBitmapCache = object : LruCache<String, ImageBitmap>(96) {}

suspend fun loadCircularAppIconBitmap(
    context: Context,
    packageName: String,
    outputSize: Int = DEFAULT_ICON_BITMAP_SIZE,
    iconPackPackage: String? = null
): ImageBitmap? = withContext(Dispatchers.IO) {
    val cacheKey = "$packageName#$outputSize#${iconPackPackage.orEmpty()}"
    synchronized(appIconBitmapCache) {
        appIconBitmapCache.get(cacheKey)
    }?.let { return@withContext it }

    val drawable = runCatching {
        IconPackResolver.resolveAppIconDrawable(context, packageName, iconPackPackage)
    }.getOrNull() ?: return@withContext null

    val bitmap = drawableToCircularImageBitmap(drawable, outputSize) ?: return@withContext null
    synchronized(appIconBitmapCache) {
        appIconBitmapCache.put(cacheKey, bitmap)
    }
    bitmap
}

fun drawableToCircularImageBitmap(
    drawable: Drawable,
    outputSize: Int = DEFAULT_ICON_BITMAP_SIZE
): ImageBitmap? {
    return try {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val size = maxOf(outputSize, width, height)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, size, size)
        val clipPath = Path().apply {
            addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
        }
        val saveCount = canvas.save()
        canvas.clipPath(clipPath)
        drawable.draw(canvas)
        canvas.restoreToCount(saveCount)

        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
