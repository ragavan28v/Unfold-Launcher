package com.unfold.core.ui.components.hud

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.unfold.core.ui.theme.LocalUnfoldTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.unfold.core.domain.model.AppInfo

@Composable
fun HudBackgroundGrid(modifier: Modifier = Modifier) {
    val theme = LocalUnfoldTheme.current
    val gridColor = theme.accentPrimary.copy(alpha = 0.05f)
    val highlightColor = theme.accentPrimary.copy(alpha = 0.12f)
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 48.dp.toPx()
        val width = size.width
        val height = size.height

        // Draw horizontal lines
        var y = 0f
        while (y < height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += step
        }

        // Draw vertical lines
        var x = 0f
        while (x < width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1.dp.toPx()
            )
            x += step
        }

        // Draw PCB junction points
        val dotRadius = 2.dp.toPx()
        for (i in 1..12) {
            for (j in 1..15) {
                if ((i * 3 + j * 7) % 11 == 0) {
                    drawCircle(
                        color = highlightColor,
                        radius = dotRadius,
                        center = Offset(i * step, j * step)
                    )
                }
            }
        }
    }
}

@Composable
fun HudRailItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeMultiplier: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val sizeCoerced by animateDpAsState(
        targetValue = if (isSelected) (40 * sizeMultiplier).dp else (36 * sizeMultiplier).dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "size"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.3f,
        animationSpec = tween(200),
        label = "border"
    )

    val containerColor = if (isSelected) {
        theme.accentPrimary.copy(alpha = 0.12f)
    } else {
        theme.bgPanel.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .size(sizeCoerced)
            .clip(RoundedCornerShape((14 * sizeMultiplier).dp))
            .background(containerColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = theme.accentPrimary.copy(alpha = borderAlpha),
                shape = RoundedCornerShape((14 * sizeMultiplier).dp)
            )
            .clickable { onClick() }
            .drawBehind {
                if (isSelected) {
                    // Draw outer soft glow
                    drawRoundRect(
                        color = theme.accentPrimary.copy(alpha = 0.2f * pulseAlpha),
                        size = size,
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) theme.accentPrimary else theme.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun HudTrace(
    modifier: Modifier = Modifier,
    horizontal: Boolean = true,
    length: Dp = 40.dp
) {
    val theme = LocalUnfoldTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(
        modifier = modifier
            .then(
                if (horizontal) Modifier.width(length).height(8.dp)
                else Modifier.width(8.dp).height(length)
            )
    ) {
        val stroke = 1.5.dp.toPx()
        val cX = size.width / 2f
        val cY = size.height / 2f

        if (horizontal) {
            drawLine(
                color = theme.accentPrimary.copy(alpha = 0.4f * pulseAlpha),
                start = Offset(0f, cY),
                end = Offset(size.width, cY),
                strokeWidth = stroke
            )
        } else {
            drawLine(
                color = theme.accentPrimary.copy(alpha = 0.4f * pulseAlpha),
                start = Offset(cX, 0f),
                end = Offset(cX, size.height),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
fun HudConnectorNode(modifier: Modifier = Modifier) {
    val theme = LocalUnfoldTheme.current
    Canvas(modifier = modifier.size(16.dp)) {
        drawCircle(
            color = theme.accentPrimary.copy(alpha = 0.2f),
            radius = 6.dp.toPx()
        )
        drawCircle(
            color = theme.accentPrimary,
            radius = 3.dp.toPx()
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape((8 * scale).dp))
            .background(theme.bgPanel.copy(alpha = 0.4f))
            .border(1.dp, theme.panelBorder.copy(alpha = 0.3f), RoundedCornerShape((8 * scale).dp))
            .padding(horizontal = (8 * scale).dp, vertical = (4 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((6 * scale).dp)
                .background(
                    if (isActive) Color(0xFF00FFCC).copy(alpha = dotAlpha) else theme.textMuted,
                    shape = RoundedCornerShape(50)
                )
        )
        Spacer(modifier = Modifier.width((6 * scale).dp))
        Text(
            text = text,
            color = theme.textSecondary,
            fontSize = (9 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun TelemetryLabel(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalUnfoldTheme.current
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = theme.textMuted,
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = theme.textPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun HudHome(
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE) }

    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var weatherTemp by remember { mutableStateOf(sharedPrefs.getString("weather_temp", "28°") ?: "28°") }
    var weatherDesc by remember { mutableStateOf(sharedPrefs.getString("weather_desc", "Partly Cloudy") ?: "Partly Cloudy") }
    var weatherLoc by remember { mutableStateOf(sharedPrefs.getString("weather_loc", "COIMBATORE, TAMIL NADU, INDIA") ?: "COIMBATORE, TAMIL NADU, INDIA") }

    var showDialog by remember { mutableStateOf(false) }
    var dialogInput by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }

    fun fetchWeather(cityName: String) {
        coroutineScope.launch {
            isFetching = true
            val success = withContext(Dispatchers.IO) {
                try {
                    var lat: Double? = null
                    var lon: Double? = null
                    var displayLoc = cityName.trim().uppercase()

                    val parts = cityName.split(Regex("[\\s,]+"))
                    if (parts.size == 2) {
                        val p1 = parts[0].toDoubleOrNull()
                        val p2 = parts[1].toDoubleOrNull()
                        if (p1 != null && p2 != null) {
                            lat = p1
                            lon = p2
                            displayLoc = "LAT: $lat, LON: $lon"
                        }
                    }

                    if (lat == null || lon == null) {
                        val encodedCity = URLEncoder.encode(cityName, "UTF-8")
                        val geoUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=en&format=json")
                        val geoConn = geoUrl.openConnection() as HttpURLConnection
                        geoConn.connectTimeout = 5000
                        geoConn.readTimeout = 5000
                        val geoResponse = geoConn.inputStream.bufferedReader().readText()
                        geoConn.disconnect()

                        val geoJson = JSONObject(geoResponse)
                        val results = geoJson.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            val firstResult = results.getJSONObject(0)
                            lat = firstResult.getDouble("latitude")
                            lon = firstResult.getDouble("longitude")
                            val name = firstResult.getString("name")
                            val admin1 = firstResult.optString("admin1")
                            val country = firstResult.optString("country")
                            displayLoc = listOf(name, admin1, country)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                                .uppercase()
                        }
                    }

                    if (lat != null && lon != null) {
                        val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                        val weatherConn = weatherUrl.openConnection() as HttpURLConnection
                        weatherConn.connectTimeout = 5000
                        weatherConn.readTimeout = 5000
                        val weatherResponse = weatherConn.inputStream.bufferedReader().readText()
                        weatherConn.disconnect()

                        val weatherJson = JSONObject(weatherResponse)
                        val currentWeather = weatherJson.getJSONObject("current_weather")
                        val tempVal = currentWeather.getDouble("temperature").toInt()
                        val weathercode = currentWeather.getInt("weathercode")
                        val condition = when (weathercode) {
                            0 -> "Clear Sky"
                            1, 2, 3 -> "Partly Cloudy"
                            45, 48 -> "Foggy"
                            51, 53, 55 -> "Drizzle"
                            61, 63, 65 -> "Rainy"
                            71, 73, 75 -> "Snowy"
                            80, 81, 82 -> "Rain Showers"
                            95 -> "Thunderstorm"
                            else -> "Overcast"
                        }

                        sharedPrefs.edit()
                            .putString("weather_temp", "${tempVal}°")
                            .putString("weather_desc", condition)
                            .putString("weather_loc", displayLoc)
                            .apply()

                        weatherTemp = "${tempVal}°"
                        weatherDesc = condition
                        weatherLoc = displayLoc
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            isFetching = false
            if (!success) {
                Toast.makeText(context, "Weather update failed, showing cached data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val verticalPadding = when (gridRows) {
        1 -> 22.dp
        2 -> 18.dp
        else -> 14.dp
    } * scale
    val sectionSpacing = when (gridRows) {
        1 -> 18.dp
        2 -> 14.dp
        else -> 12.dp
    } * scale

    val timeFontSize = when (gridRows) {
        1 -> 54.sp
        2 -> 48.sp
        else -> 42.sp
    } * scale
    val labelFontSize = when (gridRows) {
        1 -> 12.sp
        2 -> 11.sp
        else -> 10.sp
    } * scale
    val degreeFontSize = when (gridRows) {
        1 -> 42.sp
        2 -> 36.sp
        else -> 32.sp
    } * scale

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance().time
            timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
            dateText = SimpleDateFormat("EEEE · d MMMM yyyy", Locale.getDefault()).format(now).uppercase()
            kotlinx.coroutines.delay(1000)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("SET WEATHER LOCATION", color = theme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter city name:", color = theme.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = dialogInput,
                        onValueChange = { dialogInput = it },
                        placeholder = { Text("e.g. Coimbatore", color = theme.textMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dialogInput.isNotBlank()) {
                            fetchWeather(dialogInput)
                        }
                        showDialog = false
                    }
                ) {
                    Text("SAVE", color = theme.accentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("CANCEL", color = theme.textSecondary)
                }
            },
            containerColor = theme.bgPanel.copy(alpha = 0.95f),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = verticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        Column {
            Text(
                text = timeText,
                color = theme.textPrimary,
                fontSize = timeFontSize,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp
            )
            Text(
                text = dateText,
                color = theme.textSecondary,
                fontSize = labelFontSize,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height((4 * scale).dp))
            StatusChip(
                text = if (isFetching) "WEATHER UPDATING..." else "OPUS OS // SYSTEM ACTIVE",
                isActive = true,
                scale = scale
            )
        }

        Spacer(modifier = Modifier.height((8 * scale).dp))

        Column(verticalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
            ) {
                Text(
                    text = weatherTemp,
                    color = theme.textPrimary,
                    fontSize = degreeFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Set Location",
                    tint = theme.accentPrimary,
                    modifier = Modifier
                        .size((32 * scale).dp)
                        .clickable {
                            dialogInput = ""
                            showDialog = true
                        }
                )
            }

            Text(
                text = weatherDesc,
                color = theme.accentPrimary,
                fontSize = (16 * scale).sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = weatherLoc,
                color = theme.textSecondary.copy(alpha = 0.7f),
                fontSize = (11 * scale).sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // High / Low temp bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((16 * scale).dp),
            modifier = Modifier.padding(top = (8 * scale).dp)
        ) {
            Canvas(modifier = Modifier.size(width = (8 * scale).dp, height = (36 * scale).dp)) {
                drawLine(
                    color = theme.accentPrimary.copy(alpha = 0.3f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.dp.toPx() * scale
                )
                drawCircle(
                    color = theme.accentPrimary,
                    radius = 3.dp.toPx() * scale,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
            Column {
                Text(
                    text = "TODAY",
                    color = theme.textMuted,
                    fontSize = (9 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "32° / 24°",
                    color = theme.textPrimary,
                    fontSize = (18 * scale).sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

object HudMediaManager {
    data class MusicState(
        val title: String = "Nothing Playing",
        val artist: String = "NO SOURCE ACTIVE",
        val packageName: String = "",
        val isPlaying: Boolean = false,
        val position: Long = 0L,
        val duration: Long = 0L,
        val albumArt: ImageBitmap? = null
    )

    private val _musicState = MutableStateFlow(MusicState())
    val musicState: StateFlow<MusicState> = _musicState.asStateFlow()

    private var _onMediaControlListener: ((Int) -> Unit)? = null
    var onMediaControlListener: ((Int) -> Unit)?
        get() = _onMediaControlListener
        set(value) { _onMediaControlListener = value }

    fun updateState(state: MusicState) {
        _musicState.value = state
    }

    fun sendControl(keyCode: Int) {
        _onMediaControlListener?.invoke(keyCode)
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val flat = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat?.contains(context.packageName) == true
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%d:%02d", mins, secs)
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HudMusic(
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    
    val musicState by HudMediaManager.musicState.collectAsState()

    var isNotificationEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isNotificationEnabled = isNotificationServiceEnabled(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    val contentPadding = when (gridRows) {
        1 -> 18.dp
        2 -> 14.dp
        else -> 12.dp
    } * scale
    val sectionSpacing = when (gridRows) {
        1 -> 18.dp
        2 -> 14.dp
        else -> 12.dp
    } * scale

    val musicAppPackages = listOf(
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.apple.android.music",
        "org.videolan.vlc",
        "com.soundcloud.android",
        "com.amazon.mp3",
        "deezer.android.app",
        "com.pandora.android"
    )

    val installedMusicApps = remember(context) {
        musicAppPackages.mapNotNull { pkg ->
            try {
                val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                val drawable = context.packageManager.getApplicationIcon(appInfo)
                val width = drawable.intrinsicWidth.coerceAtLeast(1)
                val height = drawable.intrinsicHeight.coerceAtLeast(1)
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
                    drawable is android.graphics.drawable.AdaptiveIconDrawable) {
                    val bg = drawable.background
                    val fg = drawable.foreground
                    bg.setBounds(0, 0, width, height)
                    bg.draw(canvas)
                    val size = Math.min(width, height)
                    val circularBitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                    val circularCanvas = android.graphics.Canvas(circularBitmap)
                    val paint = android.graphics.Paint().apply { isAntiAlias = true }
                    circularCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                    paint.setXfermode(android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN))
                    circularCanvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, size, size), paint)
                    paint.setXfermode(null)
                    fg.setBounds(0, 0, size, size)
                    fg.draw(circularCanvas)
                    pkg to circularBitmap.asImageBitmap()
                } else {
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                    val size = Math.min(width, height)
                    val circularBitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                    val circularCanvas = android.graphics.Canvas(circularBitmap)
                    val paint = android.graphics.Paint().apply { isAntiAlias = true }
                    circularCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                    paint.setXfermode(android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN))
                    circularCanvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, size, size), paint)
                    pkg to circularBitmap.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = contentPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        if (!isNotificationEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape((12 * scale).dp))
                    .background(theme.accentPrimary.copy(alpha = 0.08f))
                    .border(1.dp, theme.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape((12 * scale).dp))
                    .clickable {
                        try {
                            context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Open Settings -> Notification Access manually", Toast.LENGTH_LONG).show()
                        }
                    }
                    .padding((12 * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TAP TO SYNC LIVE MUSIC TRACK DETAILS",
                    color = theme.accentPrimary,
                    fontSize = (11 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((16 * scale).dp)
        ) {
            Box(
                modifier = Modifier
                    .size((64 * scale).dp)
                    .clip(RoundedCornerShape((12 * scale).dp))
                    .background(theme.bgPanel.copy(alpha = 0.6f))
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.4f), RoundedCornerShape((12 * scale).dp)),
                contentAlignment = Alignment.Center
            ) {
                if (musicState.albumArt != null) {
                    Image(
                        bitmap = musicState.albumArt!!,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape((12 * scale).dp))
                    )
                } else {
                    Canvas(modifier = Modifier.size((28 * scale).dp)) {
                        val tint = theme.accentPrimary
                        val w = size.width
                        val h = size.height
                        drawOval(
                            color = tint,
                            topLeft = Offset(w * 0.2f, h * 0.6f),
                            size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.25f)
                        )
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.5f, h * 0.7f),
                            end = Offset(w * 0.5f, h * 0.2f),
                            strokeWidth = 3.dp.toPx() * scale,
                            cap = StrokeCap.Round
                        )
                        val flagPath = Path().apply {
                            moveTo(w * 0.5f, h * 0.2f)
                            quadraticTo(w * 0.7f, h * 0.25f, w * 0.8f, h * 0.4f)
                            quadraticTo(w * 0.7f, h * 0.35f, w * 0.5f, h * 0.35f)
                            close()
                        }
                        drawPath(flagPath, color = tint)
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((4 * scale).dp)
            ) {
                Text(
                    text = musicState.title,
                    color = theme.textPrimary,
                    fontSize = (16 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                val trimmedArtist = musicState.artist.trimEnd()
                val offsetAnimatable = remember(trimmedArtist) { androidx.compose.animation.core.Animatable(0f) }
                val fontSizeSp = 13 * scale
                var artistTextLeftPx by remember(trimmedArtist) { mutableStateOf(0f) }
                var artistTextRightPx by remember(trimmedArtist) { mutableStateOf(0f) }
                
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val containerWidthPx = with(density) { maxWidth.toPx() }
                    val startInsetPx = with(density) { 75.dp.toPx() }
                    
                    LaunchedEffect(trimmedArtist, artistTextLeftPx, artistTextRightPx, containerWidthPx) {
                        if ((artistTextRightPx - artistTextLeftPx) > containerWidthPx && containerWidthPx > 0f) {
                            val startOffsetPx = startInsetPx - artistTextLeftPx
                            val endOffsetPx = -artistTextRightPx
                            val scrollDistancePx = startOffsetPx - endOffsetPx
                            val scrollDuration = (scrollDistancePx * 38f).roundToInt()

                            while (true) {
                                offsetAnimatable.snapTo(startOffsetPx)
                                offsetAnimatable.animateTo(
                                    targetValue = endOffsetPx,
                                    animationSpec = tween(durationMillis = scrollDuration, easing = LinearEasing)
                                )
                                offsetAnimatable.snapTo(startOffsetPx)
                            }
                        } else {
                            offsetAnimatable.snapTo(0f)
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((4 * scale).dp))
                    ) {
                        Text(
                            text = trimmedArtist,
                            color = theme.textSecondary,
                            fontSize = fontSizeSp.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        x = offsetAnimatable.value.roundToInt(),
                                        y = 0
                                    )
                            },
                            onTextLayout = { layoutResult ->
                                artistTextLeftPx = layoutResult.getLineLeft(0)
                                artistTextRightPx = layoutResult.getLineRight(0)
                            }
                        )
                    }
                }
            }
        }
        var animatedPhase by remember { mutableStateOf(0f) }
        val isPlaying = musicState.isPlaying
        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    animatedPhase = (elapsed * 0.003f) % (2f * Math.PI.toFloat())
                    kotlinx.coroutines.delay(16)
                }
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(musicState.position),
                    color = theme.textMuted,
                    fontSize = (10 * scale).sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formatTime(musicState.duration),
                    color = theme.textMuted,
                    fontSize = (10 * scale).sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height((4 * scale).dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((24 * scale).dp)
            ) {
                val duration = if (musicState.duration > 0) musicState.duration else 1L
                val progressFraction = (musicState.position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                val playheadX = size.width * progressFraction
                val centerY = size.height / 2f
                val tint = theme.accentPrimary

                if (playheadX > 0f) {
                    val activePath = Path()
                    val steps = (playheadX / 2.dp.toPx()).toInt().coerceAtLeast(1)
                    for (i in 0..steps) {
                        val x = (i * (playheadX / steps)).coerceAtMost(playheadX)
                        val envelope = Math.sin(Math.PI * x / playheadX).toFloat()
                        val frequency = 0.04f
                        val waveY = centerY + Math.sin(x.toDouble() * frequency - animatedPhase.toDouble()).toFloat() * (8.dp.toPx() * scale) * envelope
                        if (i == 0) {
                            activePath.moveTo(x, waveY)
                        } else {
                            activePath.lineTo(x, waveY)
                        }
                    }
                    drawPath(
                        path = activePath,
                        color = tint,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawCircle(
                    color = tint,
                    radius = 5.dp.toPx() * scale,
                    center = Offset(playheadX, centerY)
                )
                drawCircle(
                    color = tint.copy(alpha = 0.4f),
                    radius = 9.dp.toPx() * scale,
                    center = Offset(playheadX, centerY)
                )

                if (playheadX < size.width) {
                    val dotSpacing = 6.dp.toPx()
                    val dotRadius = 2.dp.toPx()
                    var startX = playheadX + dotSpacing
                    while (startX < size.width) {
                        drawCircle(
                            color = tint.copy(alpha = 0.4f),
                            radius = dotRadius,
                            center = Offset(startX, centerY)
                        )
                        startX += dotSpacing
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compactControlThreshold = (260 * scale).dp
            val useCompactControls = maxWidth < compactControlThreshold
            val extraControlSize = (28 * scale).dp
            val navControlSize = (38 * scale).dp
            val primaryControlSize = (56 * scale).dp
            val iconSize = (14 * scale).dp
            val primaryIconSize = (28 * scale).dp
            val controlSpacing = (12 * scale).dp
            val controlBoxHeight = if (useCompactControls) (90 * scale).dp else (110 * scale).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(controlBoxHeight),
                contentAlignment = Alignment.Center
            ) {
                val tint = theme.accentPrimary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cX = size.width / 2f
                    val cY = size.height / 2f

                    drawLine(
                        color = tint.copy(alpha = 0.12f),
                        start = Offset(0f, cY),
                        end = Offset(size.width, cY),
                        strokeWidth = 2.5.dp.toPx()
                    )

                    drawCircle(
                        color = tint.copy(alpha = 0.15f),
                        radius = 42.dp.toPx() * scale,
                        center = Offset(cX, cY),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f))
                    )
                    drawCircle(
                        color = tint.copy(alpha = 0.08f),
                        radius = 54.dp.toPx() * scale,
                        center = Offset(cX, cY),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 12f), 0f))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = (8 * scale).dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (useCompactControls) Arrangement.SpaceEvenly else Arrangement.spacedBy(controlSpacing)
                    ) {
                        if (!useCompactControls) {
                            Box(
                                modifier = Modifier
                                    .size(extraControlSize)
                                    .clickable {
                                        HudMediaManager.sendControl(android.view.KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(iconSize)) {
                                    val color = theme.textSecondary.copy(alpha = 0.7f)
                                    val w = size.width
                                    val h = size.height
                                    val p1 = Path().apply {
                                        moveTo(0f, h * 0.2f)
                                        lineTo(w * 0.4f, h * 0.2f)
                                        lineTo(w * 0.7f, h * 0.8f)
                                        lineTo(w, h * 0.8f)
                                    }
                                    drawPath(p1, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                                    drawPath(
                                        Path().apply {
                                            moveTo(w * 0.85f, h * 0.7f)
                                            lineTo(w, h * 0.8f)
                                            lineTo(w * 0.85f, h * 0.9f)
                                        },
                                        color = color,
                                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    val p2 = Path().apply {
                                        moveTo(0f, h * 0.8f)
                                        lineTo(w * 0.4f, h * 0.8f)
                                        lineTo(w * 0.7f, h * 0.2f)
                                        lineTo(w, h * 0.2f)
                                    }
                                    drawPath(p2, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                                    drawPath(
                                        Path().apply {
                                            moveTo(w * 0.85f, h * 0.1f)
                                            lineTo(w, h * 0.2f)
                                            lineTo(w * 0.85f, h * 0.3f)
                                        },
                                        color = color,
                                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(navControlSize)
                                .border(1.dp, theme.textSecondary.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    HudMediaManager.sendControl(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(iconSize)) {
                                val color = theme.textPrimary
                                val w = size.width
                                val h = size.height
                                val path = Path().apply {
                                    moveTo(w * 0.8f, h * 0.2f)
                                    lineTo(w * 0.35f, h * 0.5f)
                                    lineTo(w * 0.8f, h * 0.8f)
                                    close()
                                }
                                drawPath(path, color = color)
                                drawRect(color = color, topLeft = Offset(w * 0.2f, h * 0.2f), size = androidx.compose.ui.geometry.Size(w * 0.1f, h * 0.6f))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(primaryControlSize)
                                .border(1.5.dp, tint, CircleShape)
                                .clickable {
                                    val key = if (musicState.isPlaying) android.view.KeyEvent.KEYCODE_MEDIA_PAUSE else android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                                    HudMediaManager.sendControl(key)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(primaryIconSize)) {
                                if (musicState.isPlaying) {
                                    val w = size.width
                                    val h = size.height
                                    drawRect(color = tint, topLeft = Offset(w * 0.3f, h * 0.25f), size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.5f))
                                    drawRect(color = tint, topLeft = Offset(w * 0.58f, h * 0.25f), size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.5f))
                                } else {
                                    val path = Path().apply {
                                        moveTo(size.width * 0.35f, size.height * 0.25f)
                                        lineTo(size.width * 0.75f, size.height * 0.5f)
                                        lineTo(size.width * 0.35f, size.height * 0.75f)
                                        close()
                                    }
                                    drawPath(path, color = tint)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(navControlSize)
                                .border(1.dp, theme.textSecondary.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    HudMediaManager.sendControl(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(iconSize)) {
                                val color = theme.textPrimary
                                val w = size.width
                                val h = size.height
                                val path = Path().apply {
                                    moveTo(w * 0.2f, h * 0.2f)
                                    lineTo(w * 0.65f, h * 0.5f)
                                    lineTo(w * 0.2f, h * 0.8f)
                                    close()
                                }
                                drawPath(path, color = color)
                                drawRect(color = color, topLeft = Offset(w * 0.7f, h * 0.2f), size = androidx.compose.ui.geometry.Size(w * 0.1f, h * 0.6f))
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = (8 * scale).dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOURCES: ",
                color = theme.textSecondary.copy(alpha = 0.5f),
                fontSize = (8 * scale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = (8 * scale).dp)
            )
            installedMusicApps.forEach { (pkg, icon) ->
                val isCurrent = musicState.packageName == pkg
                Box(
                    modifier = Modifier
                        .padding(horizontal = (4 * scale).dp)
                        .size((28 * scale).dp)
                        .border(
                            width = if (isCurrent) 1.5.dp else 0.5.dp,
                            color = if (isCurrent) theme.accentPrimary else theme.panelBorder.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable {
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                intent?.let { context.startActivity(it) }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size((22 * scale).dp).clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun HudSystem(
    batteryPercent: Float,
    batteryText: String,
    ramUsedText: String,
    ramUsedPercent: Float,
    storageUsedText: String,
    storageUsedPercent: Float,
    cpuTempText: String,
    cpuTemp: Float,
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    val contentPadding = when (gridRows) {
        1 -> 8.dp
        2 -> 6.dp
        else -> 4.dp
    } * scale
    val gaugeSize = when (gridRows) {
        1 -> 150.dp
        2 -> 130.dp
        else -> 110.dp
    } * scale

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = (12 * scale).dp, vertical = contentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Battery Gauge on Left
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            HudBatteryGauge(
                percent = batteryPercent,
                text = batteryText,
                modifier = Modifier.size(gaugeSize),
                scale = scale
            )
        }

        // Stat Cards stacked on Right
        Column(
            modifier = Modifier
                .weight(1.2f)
                .padding(start = (8 * scale).dp),
            verticalArrangement = Arrangement.spacedBy((6 * scale).dp)
        ) {
            HudInfoCard(
                title = "RAM",
                value = ramUsedText.split("/").firstOrNull()?.trim() ?: "4.2 GB",
                subtitle = "of " + (ramUsedText.split("/").lastOrNull()?.trim() ?: "8 GB"),
                progress = ramUsedPercent,
                scale = scale
            )

            HudInfoCard(
                title = "STORAGE",
                value = storageUsedText.split("/").firstOrNull()?.trim() ?: "64 GB",
                subtitle = "of " + (storageUsedText.split("/").lastOrNull()?.trim() ?: "128 GB"),
                progress = storageUsedPercent,
                scale = scale
            )

            HudInfoCard(
                title = "TEMP",
                value = cpuTempText,
                subtitle = "battery status ok",
                progress = cpuTemp / 100f,
                scale = scale
            )
        }
    }
}

@Composable
fun HudBatteryGauge(
    percent: Float,
    text: String,
    modifier: Modifier = Modifier.size(130.dp),
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "gauge")
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((110 * scale).dp)) {
            val strokeWidth = 5.dp.toPx() * scale
            // Track
            drawCircle(
                color = theme.panelBorder.copy(alpha = 0.2f),
                style = Stroke(width = strokeWidth)
            )

            // Progress Arc
            drawArc(
                color = theme.accentPrimary,
                startAngle = rotateAngle - 90f,
                sweepAngle = 360f * percent,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Decorative inner ticks
            val tickCount = 20
            val tickRadius = 45.dp.toPx() * scale
            for (i in 0 until tickCount) {
                val angle = (360f / tickCount) * i
                val x = size.width / 2 + tickRadius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                val y = size.height / 2 + tickRadius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                drawCircle(
                    color = theme.accentPrimary.copy(alpha = if (angle / 360f < percent) 0.6f else 0.15f),
                    radius = 1.5.dp.toPx() * scale,
                    center = Offset(x, y)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = theme.textPrimary,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "ON BATTERY",
                color = theme.textSecondary.copy(alpha = 0.6f),
                fontSize = (8 * scale).sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HudInfoCard(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((12 * scale).dp))
            .background(theme.bgPanel.copy(alpha = 0.4f))
            .border(1.dp, theme.panelBorder.copy(alpha = 0.25f), RoundedCornerShape((12 * scale).dp))
            .padding(horizontal = (12 * scale).dp, vertical = (8 * scale).dp),
        verticalArrangement = Arrangement.spacedBy((4 * scale).dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = theme.textSecondary.copy(alpha = 0.8f),
                fontSize = (8 * scale).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Text(
            text = value,
            color = theme.textPrimary,
            fontSize = (18 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = subtitle.lowercase(),
            color = theme.textMuted,
            fontSize = (9 * scale).sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height((2 * scale).dp))

        // Small Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((4 * scale).dp)
                .clip(RoundedCornerShape((2 * scale).dp))
                .background(theme.panelBorder.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(theme.accentPrimary)
            )
        }
    }
}

@Composable
fun HudGoogleFeed(
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = when (gridRows) {
                1 -> 18.dp
                2 -> 14.dp
                else -> 12.dp
            } * scale)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
    ) {
        Text(
            text = "FEED / INTEL",
            color = theme.accentPrimary,
            fontSize = (14 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height((12 * scale).dp))
        
        // Simulating futuristic feeds
        listOf(
            "CRITICAL: CPU thermal throttling active (36°C)" to "15m ago",
            "INTEL: Unfold Launcher v3.5 compiled successfully" to "1h ago",
            "NEWS: Android SDK 35 targets runtime enhancements" to "3h ago"
        ).forEach { (title, time) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = (6 * scale).dp)
                    .background(theme.bgPanel.copy(alpha = 0.3f), RoundedCornerShape((8 * scale).dp))
                    .border(1.5.dp, theme.panelBorder.copy(alpha = 0.2f), RoundedCornerShape((8 * scale).dp))
                    .padding((10 * scale).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        color = theme.textPrimary,
                        fontSize = (11 * scale).sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = time,
                        color = theme.textMuted,
                        fontSize = (9 * scale).sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun HudWidgets(
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = when (gridRows) {
                1 -> 18.dp
                2 -> 14.dp
                else -> 12.dp
            } * scale)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
    ) {
        Text(
            text = "WIDGET CONTROL",
            color = theme.accentPrimary,
            fontSize = (14 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height((12 * scale).dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)
        ) {
            // Simulated memory widget
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(theme.bgPanel.copy(alpha = 0.3f), RoundedCornerShape((12 * scale).dp))
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.2f), RoundedCornerShape((12 * scale).dp))
                    .padding((12 * scale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PING", color = theme.textSecondary, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace)
                Text("24 ms", color = theme.accentPrimary, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("loss: 0.0%", color = theme.textMuted, fontSize = (8 * scale).sp, fontFamily = FontFamily.Monospace)
            }
            
            // Simulated clock widget
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(theme.bgPanel.copy(alpha = 0.3f), RoundedCornerShape((12 * scale).dp))
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.2f), RoundedCornerShape((12 * scale).dp))
                    .padding((12 * scale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("UPTIME", color = theme.textSecondary, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace)
                Text("124h", color = theme.accentPrimary, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("status: normal", color = theme.textMuted, fontSize = (8 * scale).sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun HudCategories(
    apps: List<com.unfold.core.domain.model.AppInfo>,
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    
    // Simple category mapping based on package names
    val categorized = remember(apps) {
        val groups = mutableMapOf<String, MutableList<String>>()
        groups["System"] = mutableListOf()
        groups["Social/Comm"] = mutableListOf()
        groups["Media"] = mutableListOf()
        groups["Applications"] = mutableListOf()
        apps.forEach { app ->
            val pkg = app.packageName
            val cat = when {
                pkg.contains("android") || pkg.contains("system") || pkg.contains("settings") -> "System"
                pkg.contains("chrome") || pkg.contains("gmail") || pkg.contains("chat") || pkg.contains("whatsapp") || pkg.contains("messenger") -> "Social/Comm"
                pkg.contains("youtube") || pkg.contains("media") || pkg.contains("music") || pkg.contains("player") || pkg.contains("spotify") || pkg.contains("photos") -> "Media"
                else -> "Applications"
            }
            groups.getOrPut(cat) { mutableListOf() }.add(app.label)
        }
        groups.filter { it.value.isNotEmpty() }
    }

    val contentPadding = when (gridRows) {
        1 -> 18.dp
        2 -> 14.dp
        else -> 12.dp
    } * scale
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = contentPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
    ) {
        Text(
            text = "CATEGORY ORG",
            color = theme.accentPrimary,
            fontSize = (14 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height((12 * scale).dp))
        
        Column(verticalArrangement = Arrangement.spacedBy((8 * scale).dp)) {
            categorized.forEach { (categoryName, appLabels) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.bgPanel.copy(alpha = 0.3f), RoundedCornerShape((8 * scale).dp))
                        .border(1.dp, theme.panelBorder.copy(alpha = 0.15f), RoundedCornerShape((8 * scale).dp))
                        .padding((8 * scale).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryName.uppercase(),
                        color = theme.textPrimary,
                        fontSize = (10 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${appLabels.size} Apps",
                        color = theme.accentPrimary,
                        fontSize = (10 * scale).sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}


