package com.unfold.feature.home

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.unfold.core.domain.model.AppInfo
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
import com.unfold.core.ui.util.LauncherUtils
import com.unfold.core.ui.notification.NotificationBadgeStore

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherV2HomeScreen(
    viewModel: HomeViewModel,
    onDrawerDragDistance: (Float) -> Unit,
    onDrawerDragEnd: (Float, Float) -> Unit,
    onOpenSettings: () -> Unit
) {
    val homeState by viewModel.uiState.collectAsState()
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
                    theme = theme,
                    extraModifier = Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDrag = 0f
                            val velocityTracker = VelocityTracker()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            onDrawerDragDistance(0f)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val dragAmount = change.position.x - change.previousPosition.x
                                if (dragAmount != 0f) {
                                    totalDrag += dragAmount
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                    if (totalDrag < 0f) {
                                        onDrawerDragDistance(totalDrag)
                                    }
                                }

                                if (!change.pressed) {
                                    onDrawerDragEnd(totalDrag, velocityTracker.calculateVelocity().x)
                                    break
                                }
                            }
                        }
                    }
                ) {
                    VerticalHomeGrid(
                        apps = homeState.gridApps,
                        iconSize = launcherGridIconSize(homeState.homeIconSize, homeState.gridColumns).coerceAtMost(48.dp),
                        showLabels = homeState.homeLabelsEnabled,
                        iconPackPackage = homeState.iconPackPackage,
                        showBadges = homeState.showNotificationBadges,
                        badgeColor = remember(homeState.badgeColorHex) {
                            runCatching { Color(android.graphics.Color.parseColor(homeState.badgeColorHex)) }
                                .getOrElse { Color(0xFFF44336) }
                        },
                        onMove = { app, position ->
                            viewModel.onIntent(HomeUiIntent.MoveApp(app.appId, position))
                        },
                        onRemove = { app ->
                            viewModel.onIntent(HomeUiIntent.UnpinApp(app.appId))
                        }
                    )
                }
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
                    showNumber = false,
                    modifier = panelBorder
                        .fillMaxHeight()
                        .weight(1f),
                    theme = theme
                ) {
                    CameraPanelAction()
                }
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

@Composable
private fun VerticalHomeGrid(
    apps: List<AppInfo>,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabels: Boolean,
    iconPackPackage: String,
    showBadges: Boolean,
    badgeColor: Color,
    onMove: (AppInfo, Int) -> Unit,
    onRemove: (AppInfo) -> Unit
) {
    val gridApps = apps.filter { (it.gridPosition ?: -1) in 0 until 100 }
    val notificationBadges by NotificationBadgeStore.badges.collectAsState()
    var draggedAppId by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        gridApps.forEach { app ->
            VerticalHomeGridItem(
                app = app,
                isDragging = draggedAppId == app.appId,
                dragPosition = dragPosition,
                dragOrigin = dragOrigin,
                itemBounds = itemBounds,
                iconSize = iconSize,
                showLabels = showLabels,
                iconPackPackage = iconPackPackage,
                showBadge = showBadges && NotificationBadgeStore.instanceKey(app.packageName, app.userSerial) in notificationBadges,
                badgeColor = badgeColor,
                onDragStart = { bounds ->
                    draggedAppId = app.appId
                    dragOrigin = bounds.topLeft
                    dragPosition = bounds.topLeft
                },
                onDrag = { dragPosition += it },
                onDrop = {
                    val targetApp = resolveVerticalGridDropTarget(
                        dragPosition,
                        gridApps.filterNot { it.appId == app.appId },
                        itemBounds
                    )
                    val sourceSlot = app.gridPosition
                    val targetSlot = targetApp?.gridPosition
                    if (targetApp != null && sourceSlot != null && targetSlot != null && targetApp.appId != app.appId) {
                        onMove(app, targetSlot)
                        onMove(targetApp, sourceSlot)
                    }
                    draggedAppId = null
                },
                onRemove = onRemove
            )
        }
    }
}

@Composable
private fun CameraPanelAction() {
    val context = androidx.compose.ui.platform.LocalContext.current
    HudRailItem(
        icon = Icons.Default.CameraAlt,
        isSelected = false,
        onClick = {
            val cameraIntent = listOf(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            ).firstOrNull { it.resolveActivity(context.packageManager) != null }

            if (cameraIntent != null) {
                runCatching { context.startActivity(cameraIntent) }
                    .onFailure { Log.w("LauncherV2", "Unable to open camera", it) }
            }
        },
        sizeMultiplier = 1.15f
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalHomeGridItem(
    app: AppInfo,
    isDragging: Boolean,
    dragPosition: Offset,
    dragOrigin: Offset,
    itemBounds: MutableMap<String, androidx.compose.ui.geometry.Rect>,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabels: Boolean,
    iconPackPackage: String,
    showBadge: Boolean,
    badgeColor: Color,
    onDragStart: (androidx.compose.ui.geometry.Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onDrop: () -> Unit,
    onRemove: (AppInfo) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val theme = LocalUnfoldTheme.current
    val menuDragThresholdPx = with(LocalDensity.current) { 14.dp.toPx() }
    var showMenu by remember { mutableStateOf(false) }
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.appId, iconPackPackage) {
        value = withContext(Dispatchers.IO) {
            com.unfold.core.ui.iconpack.IconPackResolver.resolveAppIconDrawable(
                context,
                app.packageName,
                iconPackPackage.takeIf { it.isNotBlank() }
            )?.let(::drawableToImageBitmap)
        }
    }

    Box(
        modifier = Modifier
            .alpha(if (isDragging) 0.3f else 1f)
            .onGloballyPositioned { itemBounds[app.appId] = it.boundsInRoot() }
            .offset {
                IntOffset(
                    (if (isDragging) dragPosition.x - dragOrigin.x else 0f).roundToInt(),
                    (if (isDragging) dragPosition.y - dragOrigin.y else 0f).roundToInt()
                )
            }
            .pointerInput(app.appId) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        // HomeAppGridItem handles the regular tap, matching the V1 grid.
                    } else {
                        onDragStart(itemBounds[app.appId] ?: return@awaitEachGesture)
                        var totalDrag = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (totalDrag < menuDragThresholdPx) {
                                    showMenu = true
                                }
                                onDrop()
                                break
                            }
                            val delta = change.position - change.previousPosition
                            totalDrag += delta.getDistance()
                            change.consume()
                            onDrag(delta)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        HomeAppGridItem(
            app = app,
            iconSize = iconSize,
            showLabel = showLabels,
            iconBitmap = iconBitmap,
            rawIcon = iconPackPackage.isNotBlank() &&
                !com.unfold.core.ui.iconpack.IconPackResolver.isLauncherRingEnabled(context),
            badgeColor = badgeColor,
            showBadge = showBadge,
            onClick = {
                NotificationBadgeStore.clearInstance(
                    NotificationBadgeStore.instanceKey(app.packageName, app.userSerial)
                )
                LauncherUtils.launchApp(context, app)
            }
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.border(1.dp, theme.accentPrimary.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
            containerColor = theme.bgPanel,
            properties = PopupProperties(focusable = true)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "REMOVE FROM HOME",
                        color = theme.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                onClick = {
                    onRemove(app)
                    showMenu = false
                }
            )
        }
    }
}

private fun resolveVerticalGridDropTarget(
    dragPosition: Offset,
    apps: List<AppInfo>,
    itemBounds: Map<String, androidx.compose.ui.geometry.Rect>
): AppInfo? = apps.minByOrNull { app ->
    val bounds = itemBounds[app.appId] ?: return@minByOrNull Float.MAX_VALUE
    val centerX = (bounds.left + bounds.right) / 2f
    val centerY = (bounds.top + bounds.bottom) / 2f
    val dx = dragPosition.x - centerX
    val dy = dragPosition.y - centerY
    dx * dx + dy * dy
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
    extraModifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.then(extraModifier),
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
