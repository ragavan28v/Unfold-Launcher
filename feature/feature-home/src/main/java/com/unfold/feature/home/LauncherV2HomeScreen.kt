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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlinx.coroutines.launch
import com.unfold.core.ui.components.hud.HudBackgroundGrid
import com.unfold.core.ui.components.hud.HudCategories
import com.unfold.core.ui.components.hud.HudFlow
import com.unfold.core.ui.components.hud.HudGoogleFeed
import com.unfold.core.ui.components.hud.HudHome
import com.unfold.core.ui.components.hud.HudMusic
import com.unfold.core.ui.components.hud.HudSystem
import com.unfold.core.ui.components.hud.HudConnectorNode
import com.unfold.core.ui.components.hud.HudRailItem
import com.unfold.core.ui.components.hud.HudTrace
import com.unfold.core.ui.theme.LocalUnfoldTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherV2HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    val panelBorder = Modifier.border(1.dp, theme.panelBorder)
    val pagerState = rememberPagerState(pageCount = { 6 })
    val coroutineScope = rememberCoroutineScope()

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
                        currentPage = pagerState.currentPage,
                        onPageSelected = { page ->
                            coroutineScope.launch { pagerState.animateScrollToPage(page) }
                        },
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
                        showNumber = false,
                        modifier = panelBorder
                            .fillMaxWidth()
                            .weight(1f),
                        theme = theme
                    ) {
                        V2HudPageDisplay(viewModel = viewModel, pagerState = pagerState)
                    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2HudPageDisplay(
    viewModel: HomeViewModel,
    pagerState: PagerState
) {
    val state by viewModel.uiState.collectAsState()
    val rows = state.gridRows

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scale = min(
            maxHeight.value / 360f,
            maxWidth.value / 320f
        ).coerceIn(0.35f, 1.1f)

        HudBackgroundGrid(modifier = Modifier.fillMaxSize())
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageModifier = Modifier
                .fillMaxSize()

            when (page) {
                0 -> HudHome(
                    modifier = pageModifier,
                    gridRows = rows,
                    scale = scale,
                    iconPackPackage = state.iconPackPackage
                )
                1 -> HudMusic(
                    modifier = pageModifier,
                    gridRows = rows,
                    scale = scale,
                    iconPackPackage = state.iconPackPackage
                )
                2 -> HudSystem(
                    batteryPercent = state.systemStats?.batteryPercent ?: 0.5f,
                    batteryText = state.systemStats?.batteryText ?: "50%",
                    ramUsedText = state.systemStats?.ramUsedText ?: "4.2 GB / 8.0 GB",
                    ramUsedPercent = state.systemStats?.ramUsedPercent ?: 0.5f,
                    storageUsedText = state.systemStats?.storageUsedText ?: "64 GB / 128 GB",
                    storageUsedPercent = state.systemStats?.storageUsedPercent ?: 0.5f,
                    cpuTempText = state.systemStats?.cpuTempText ?: "36°C",
                    cpuTemp = state.systemStats?.cpuTemp ?: 36f,
                    modifier = pageModifier,
                    gridRows = rows,
                    scale = scale,
                    iconPackPackage = state.iconPackPackage
                )
                3 -> HudGoogleFeed(
                    modifier = pageModifier,
                    gridRows = rows,
                    scale = scale
                )
                4 -> HudFlow(
                    timelineItems = state.timelineItems,
                    notes = state.notes,
                    modifier = pageModifier,
                    gridRows = rows,
                    scale = scale,
                    onLoadMore = { viewModel.loadMoreTimelineEvents() },
                    onRefreshTimeline = { viewModel.refreshTimelineEvents() },
                    onSaveNote = { viewModel.saveNote(it) },
                    onDeleteNote = { viewModel.deleteNote(it) }
                )
                5 -> HudCategories(
                    folders = state.folders,
                    allApps = state.installedApps,
                    modifier = pageModifier,
                    gridRows = rows,
                    scale = scale,
                    iconPackPackage = state.iconPackPackage,
                    onCreateFolder = { name, appIds -> viewModel.createFolder(name, appIds) },
                    onRenameFolder = { folderId, name -> viewModel.renameFolder(folderId, name) },
                    onDeleteFolder = { folderId -> viewModel.deleteFolder(folderId) },
                    onUpdateFolderApps = { folderId, appIds -> viewModel.updateFolderApps(folderId, appIds) },
                    onReorderFolders = { folderIds -> viewModel.reorderFolders(folderIds) }
                )
            }
        }
    }
}

private data class V2RailNode(
    val icon: ImageVector,
    val page: Int? = null
)

@Composable
private fun VerticalHudRail(
    onOpenSettings: () -> Unit,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    batteryText: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val theme = LocalUnfoldTheme.current
    var flashlightEnabled by remember { mutableStateOf(false) }
    var soundMode by remember { mutableStateOf(detectSoundMode(context)) }
    val nodes = listOf(
        V2RailNode(Icons.Default.Home, page = 0),
        V2RailNode(Icons.Default.MusicNote, page = 1),
        V2RailNode(Icons.Default.Memory, page = 2),
        V2RailNode(if (flashlightEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff),
        V2RailNode(Icons.Default.Search, page = 3),
        V2RailNode(Icons.Default.ViewStream, page = 4),
        V2RailNode(Icons.Default.GridView, page = 5)
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
                    isSelected = node.page == currentPage || (index == 3 && flashlightEnabled),
                    onClick = {
                        if (index == 3) {
                            flashlightEnabled = !flashlightEnabled
                            toggleFlashlight(context, flashlightEnabled)
                        } else if (node.page != null) {
                            onPageSelected(node.page)
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
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
