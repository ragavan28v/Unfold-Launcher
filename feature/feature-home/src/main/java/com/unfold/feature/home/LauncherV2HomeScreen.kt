package com.unfold.feature.home

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unfold.core.ui.components.hud.HudConnectorNode
import com.unfold.core.ui.components.hud.HudRailItem
import com.unfold.core.ui.components.hud.HudTrace
import com.unfold.core.ui.theme.LocalUnfoldTheme

@Composable
fun LauncherV2HomeScreen(
    onOpenSettings: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    val panelBorder = Modifier.border(1.dp, theme.panelBorder)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(theme.bgVoid)
            .border(1.dp, theme.panelBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(6f)
            ) {
                NumberedPanel(
                    number = 1,
                    showNumber = false,
                    modifier = panelBorder
                        .fillMaxHeight()
                        .weight(1f),
                    theme = theme
                ) {
                    VerticalHudRail(
                        onOpenSettings = onOpenSettings,
                        batteryText = rememberBatteryText()
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(3f)
                ) {
                    NumberedPanel(
                        number = 2,
                        modifier = panelBorder
                            .fillMaxWidth()
                            .weight(1f),
                        theme = theme
                    )
                    NumberedPanel(
                        number = 4,
                        modifier = panelBorder
                            .fillMaxWidth()
                            .weight(1f),
                        theme = theme
                    )
                }
                NumberedPanel(
                    number = 3,
                    modifier = panelBorder
                        .fillMaxHeight()
                        .weight(1f),
                    theme = theme
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                NumberedPanel(
                    number = 5,
                    showNumber = false,
                    modifier = panelBorder
                        .fillMaxHeight()
                        .weight(1f),
                    theme = theme
                ) {
                    PhonePanelAction()
                }
                NumberedPanel(
                    number = 6,
                    modifier = panelBorder
                        .fillMaxHeight()
                        .weight(3f),
                    theme = theme
                )
                NumberedPanel(
                    number = 7,
                    modifier = panelBorder
                        .fillMaxHeight()
                        .weight(1f),
                    theme = theme
                )
            }
        }
    }
}

private data class V2RailNode(
    val icon: ImageVector,
    val selected: Boolean = false
)

@Composable
private fun VerticalHudRail(
    onOpenSettings: () -> Unit,
    batteryText: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val theme = LocalUnfoldTheme.current
    var selectedNode by remember { mutableStateOf(0) }
    var flashlightEnabled by remember { mutableStateOf(false) }
    var soundMode by remember { mutableStateOf(detectSoundMode(context)) }
    val nodes = listOf(
        V2RailNode(Icons.Default.Home),
        V2RailNode(Icons.Default.MusicNote),
        V2RailNode(Icons.Default.Memory),
        V2RailNode(if (flashlightEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff),
        V2RailNode(Icons.Default.Search),
        V2RailNode(Icons.Default.ViewStream),
        V2RailNode(Icons.Default.GridView)
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HudTrace(horizontal = false, length = 8.dp)
            nodes.forEachIndexed { index, node ->
                HudRailItem(
                    icon = node.icon,
                    isSelected = selectedNode == index || (index == 3 && flashlightEnabled),
                    onClick = {
                        if (index == 3) {
                            flashlightEnabled = !flashlightEnabled
                            toggleFlashlight(context, flashlightEnabled)
                        } else {
                            selectedNode = index
                        }
                    },
                    sizeMultiplier = 1f
                )
                if (index < nodes.lastIndex) {
                    HudTrace(horizontal = false, length = 6.dp)
                }
            }
            HudTrace(horizontal = false, length = 8.dp)
            HudRailItem(
                icon = soundMode.icon,
                isSelected = soundMode != V2SoundMode.GENERAL,
                onClick = {
                    soundMode = soundMode.next()
                    applySoundMode(context, soundMode)
                },
                sizeMultiplier = 1f
            )
            HudTrace(horizontal = false, length = 8.dp)
            HudConnectorNode()
            Text(
                text = batteryText,
                color = theme.accentPrimary,
                maxLines = 1
            )
            HudTrace(horizontal = false, length = 10.dp)
            HudConnectorNode()
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HudVerticalTrace()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HudRailItem(
                icon = Icons.Default.Settings,
                isSelected = false,
                onClick = onOpenSettings,
                sizeMultiplier = 1f
            )
            HudTrace(horizontal = false, length = 10.dp)
            HudConnectorNode()
        }
    }
}

@Composable
private fun rememberBatteryText(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    var batteryText by remember { mutableStateOf(readBatteryText(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryText = batteryTextFromIntent(intent)
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    return batteryText
}

private fun readBatteryText(context: Context): String {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return batteryTextFromIntent(intent)
}

private fun batteryTextFromIntent(intent: Intent?): String {
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    if (level < 0 || scale <= 0) return "--%"
    return "${(level * 100 / scale).coerceIn(0, 100)}%"
}

@Composable
private fun HudVerticalTrace() {
    val theme = LocalUnfoldTheme.current
    Canvas(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        val centerX = size.width / 2f
        drawLine(
            color = theme.accentPrimary.copy(alpha = 0.45f),
            start = androidx.compose.ui.geometry.Offset(centerX, 0f),
            end = androidx.compose.ui.geometry.Offset(centerX, size.height),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
private fun PhonePanelAction() {
    val context = androidx.compose.ui.platform.LocalContext.current
    HudRailItem(
        icon = Icons.Default.Phone,
        isSelected = false,
        onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_DIAL))
            }
        },
        sizeMultiplier = 1.15f
    )
}

private fun toggleFlashlight(context: Context, enabled: Boolean) {
    runCatching {
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val cameraId = cameraManager?.cameraIdList?.firstOrNull()
        if (cameraId != null) {
            cameraManager.setTorchMode(cameraId, enabled)
        }
    }.onFailure { error ->
        Log.e("LauncherV2", "Unable to toggle flashlight", error)
    }
}

private enum class V2SoundMode(
    val icon: ImageVector
) {
    GENERAL(Icons.Default.VolumeUp),
    VIBRATE(Icons.Default.Vibration),
    SILENT(Icons.Default.VolumeOff);

    fun next(): V2SoundMode = when (this) {
        GENERAL -> VIBRATE
        VIBRATE -> SILENT
        SILENT -> GENERAL
    }
}

private fun detectSoundMode(context: Context): V2SoundMode {
    val audioManager = context.getSystemService(AudioManager::class.java)
    return when (audioManager?.ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> V2SoundMode.VIBRATE
        AudioManager.RINGER_MODE_SILENT -> V2SoundMode.SILENT
        else -> V2SoundMode.GENERAL
    }
}

private fun applySoundMode(context: Context, mode: V2SoundMode) {
    runCatching {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        audioManager.ringerMode = when (mode) {
            V2SoundMode.GENERAL -> AudioManager.RINGER_MODE_NORMAL
            V2SoundMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            V2SoundMode.SILENT -> AudioManager.RINGER_MODE_SILENT
        }
    }.onFailure { error ->
        Log.e("LauncherV2", "Unable to change sound mode", error)
    }
}

@Composable
private fun NumberedPanel(
    number: Int,
    modifier: Modifier,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors,
    showNumber: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (showNumber) {
            Text(
                text = number.toString(),
                color = theme.textPrimary
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}
