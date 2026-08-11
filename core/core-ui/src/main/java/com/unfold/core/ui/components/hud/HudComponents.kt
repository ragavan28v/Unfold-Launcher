package com.unfold.core.ui.components.hud

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
import com.unfold.core.ui.theme.LocalUnfoldTheme
import java.text.SimpleDateFormat
import java.util.*

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
        targetValue = if (isSelected) (46 * sizeMultiplier).dp else (42 * sizeMultiplier).dp,
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
    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
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

    // responsive font sizes driven by the home grid rows
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = verticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        // Time & Date Header
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
            StatusChip(text = "OPUS OS // SYSTEM ACTIVE", isActive = true, scale = scale)
        }

        Spacer(modifier = Modifier.height((8 * scale).dp))

        // Weather Details block
        Column(verticalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
            ) {
                Text(
                    text = "28°",
                    color = theme.textPrimary,
                    fontSize = degreeFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = theme.accentPrimary,
                    modifier = Modifier.size((32 * scale).dp)
                )
            }

            Text(
                text = "Partly Cloudy",
                color = theme.accentPrimary,
                fontSize = (16 * scale).sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "COIMBATORE, TAMIL NADU, INDIA",
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

@Composable
fun HudMusic(
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    var isPlaying by remember { mutableStateOf(false) }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = contentPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        // Track Card
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
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = theme.accentPrimary,
                    modifier = Modifier.size((28 * scale).dp)
                )
            }

            Column {
                Text(
                    text = "Nothing Playing",
                    color = theme.textPrimary,
                    fontSize = (18 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "NO SOURCE ACTIVE",
                    color = theme.textSecondary.copy(alpha = 0.6f),
                    fontSize = (11 * scale).sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Timeline Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0:00", color = theme.textMuted, fontSize = (10 * scale).sp, fontFamily = FontFamily.Monospace)
                Text("0:00", color = theme.textMuted, fontSize = (10 * scale).sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height((4 * scale).dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((12 * scale).dp)
            ) {
                drawLine(
                    color = theme.panelBorder.copy(alpha = 0.3f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx() * scale
                )
                drawCircle(
                    color = theme.accentPrimary,
                    radius = 4.dp.toPx() * scale,
                    center = Offset((24 * scale).dp.toPx(), size.height / 2)
                )
            }
        }

        // Controls Area with surrounding traces
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((100 * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            // Background interactive traces
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height / 2f
                // Draw horizontal trace line
                drawLine(
                    color = theme.accentPrimary.copy(alpha = 0.15f),
                    start = Offset(0f, cY),
                    end = Offset(size.width, cY),
                    strokeWidth = 1.dp.toPx() * scale
                )
                // Junction lines
                drawLine(
                    color = theme.accentPrimary.copy(alpha = 0.15f),
                    start = Offset(cX - 60.dp.toPx() * scale, cY),
                    end = Offset(cX - 60.dp.toPx() * scale, cY + 30.dp.toPx() * scale),
                    strokeWidth = 1.dp.toPx() * scale
                )
                drawLine(
                    color = theme.accentPrimary.copy(alpha = 0.15f),
                    start = Offset(cX + 60.dp.toPx() * scale, cY),
                    end = Offset(cX + 60.dp.toPx() * scale, cY - 30.dp.toPx() * scale),
                    strokeWidth = 1.dp.toPx() * scale
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((24 * scale).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = theme.textSecondary,
                    modifier = Modifier
                        .size((28 * scale).dp)
                        .clickable { }
                )

                Box(
                    modifier = Modifier
                        .size((56 * scale).dp)
                        .background(theme.accentPrimary.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .border(2.dp, theme.accentPrimary, RoundedCornerShape(50))
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = theme.accentPrimary,
                        modifier = Modifier.size((32 * scale).dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = theme.textSecondary,
                    modifier = Modifier
                        .size((28 * scale).dp)
                        .clickable { }
                )
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
        1 -> 18.dp
        2 -> 14.dp
        else -> 12.dp
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
            verticalArrangement = Arrangement.spacedBy((8 * scale).dp)
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
            .padding((12 * scale).dp),
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


