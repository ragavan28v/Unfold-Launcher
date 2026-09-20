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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
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
import com.unfold.core.ui.components.CarvedIcon
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
            .background(theme.bgVoid)
            .border(1.dp, theme.panelBorder)
    ) {
        V2WallpaperBackdrop(
            modifier = Modifier.fillMaxSize(),
            mode = homeState.homeWallpaperMode,
            colorHex = homeState.homeWallpaperHex,
            pattern = homeState.homeWallpaperPattern,
            imageUri = homeState.homeWallpaperImageUri,
            fallbackColor = theme.bgVoid
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                    showNumber = false,
                    modifier = panelBorder
                        .fillMaxHeight()
                        .weight(3f),
                    theme = theme
                ) {
                    V2DockPanel(
                        apps = homeState.gridApps,
                        folders = homeState.folders,
                        allApps = homeState.installedApps,
                        iconPackPackage = homeState.iconPackPackage,
                        onMoveApp = { app, slot -> viewModel.onIntent(HomeUiIntent.MoveApp(app.appId, slot)) },
                        onRemoveApp = { app -> viewModel.onIntent(HomeUiIntent.UnpinApp(app.appId)) },
                        onCreateFolder = { slot, name, appIds -> viewModel.createDockFolder(slot, name, appIds) },
                        onUpdateFolderApps = { folderId, appIds -> viewModel.updateFolderApps(folderId, appIds) },
                        onRemoveFolder = { folderId -> viewModel.deleteFolder(folderId) },
                        onMoveFolder = { folderId, slot -> viewModel.moveFolder(folderId, slot) }
                    )
                }
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

@Composable
private fun V2WallpaperBackdrop(
    modifier: Modifier = Modifier,
    mode: com.unfold.core.domain.model.WallpaperMode,
    colorHex: String,
    pattern: com.unfold.core.domain.model.WallpaperPatternMode,
    imageUri: String,
    fallbackColor: Color
) {
    val baseColor = remember(colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
            .getOrElse { fallbackColor }
    }

    Box(modifier = modifier) {
        when (mode) {
            com.unfold.core.domain.model.WallpaperMode.SOLID -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseColor)
                )
            }
            com.unfold.core.domain.model.WallpaperMode.PATTERN -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseColor)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tint = Color.White.copy(alpha = 0.05f)
                        when (pattern) {
                            com.unfold.core.domain.model.WallpaperPatternMode.GEOMETRIC -> {
                                repeat(7) { index ->
                                    val size = (40 + index * 18).dp.toPx()
                                    drawCircle(
                                        color = tint,
                                        radius = size,
                                        center = Offset(size * 1.8f, size * 0.9f + index * 110f)
                                    )
                                }
                            }
                            com.unfold.core.domain.model.WallpaperPatternMode.ABSTRACT -> {
                                repeat(6) { index ->
                                    val y = 120f + index * 160f
                                    drawLine(
                                        color = tint,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y + 24f),
                                        strokeWidth = 10f
                                    )
                                }
                            }
                            com.unfold.core.domain.model.WallpaperPatternMode.MINIMAL -> {
                                repeat(22) { index ->
                                    drawCircle(
                                        color = tint.copy(alpha = 0.03f),
                                        radius = 18f + (index % 4) * 3f,
                                        center = Offset(
                                            (index * 67f) % size.width,
                                            (index * 103f) % size.height
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            com.unfold.core.domain.model.WallpaperMode.PRESET,
            com.unfold.core.domain.model.WallpaperMode.CUSTOM -> {
                if (imageUri.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(baseColor)
                    )
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

private sealed interface V2DockEntry {
    val slot: Int
    data class AppEntry(val app: AppInfo) : V2DockEntry { override val slot = app.gridPosition ?: 100 }
    data class FolderEntry(val folder: com.unfold.core.domain.model.FolderInfo) : V2DockEntry { override val slot = folder.gridPosition }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2DockPanel(
    apps: List<AppInfo>,
    folders: List<com.unfold.core.domain.model.FolderInfo>,
    allApps: List<AppInfo>,
    iconPackPackage: String,
    onMoveApp: (AppInfo, Int) -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onCreateFolder: (Int, String, List<String>) -> Unit,
    onUpdateFolderApps: (String, List<String>) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onMoveFolder: (String, Int) -> Unit
) {
    val theme = LocalUnfoldTheme.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val entries: List<V2DockEntry> = buildList {
        apps.filter { it.gridPosition in 100..102 }.forEach { add(V2DockEntry.AppEntry(it)) }
        folders.filter { it.gridPosition in 100..102 }.forEach { folder ->
            if (none { it.slot == folder.gridPosition }) add(V2DockEntry.FolderEntry(folder))
        }
    }
    var draggedEntry by remember { mutableStateOf<V2DockEntry?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    val slotBounds = remember { mutableMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    var appMenu by remember { mutableStateOf<AppInfo?>(null) }
    var folderMenu by remember { mutableStateOf<com.unfold.core.domain.model.FolderInfo?>(null) }
    var createFromApp by remember { mutableStateOf<Pair<AppInfo, Int>?>(null) }
    var manageFolder by remember { mutableStateOf<com.unfold.core.domain.model.FolderInfo?>(null) }
    var openFolder by remember { mutableStateOf<com.unfold.core.domain.model.FolderInfo?>(null) }
    var pendingFolderSlot by remember { mutableStateOf<Int?>(null) }
    var pickAppSlot by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(folders, pendingFolderSlot) {
        pendingFolderSlot?.let { slot ->
            folders.firstOrNull { it.gridPosition == slot }?.let { folder ->
                openFolder = folder
                pendingFolderSlot = null
            }
        }
    }

    fun move(entry: V2DockEntry, slot: Int) = when (entry) {
        is V2DockEntry.AppEntry -> onMoveApp(entry.app, slot)
        is V2DockEntry.FolderEntry -> onMoveFolder(entry.folder.id, slot)
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val slot = 100 + index
            val entry = entries.firstOrNull { it.slot == slot }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .onGloballyPositioned { slotBounds[slot] = it.boundsInRoot() },
                contentAlignment = Alignment.Center
            ) {
                if (entry == null) {
                    Text(
                        text = "+",
                        color = theme.textMuted,
                        modifier = Modifier.combinedClickable(onClick = { pickAppSlot = slot }, onLongClick = { pickAppSlot = slot })
                    )
                } else {
                    V2DockEntryItem(
                        entry = entry,
                        iconPackPackage = iconPackPackage,
                        isDragging = draggedEntry == entry,
                        dragPosition = dragPosition,
                        dragOrigin = dragOrigin,
                        onTap = {
                            when (entry) {
                                is V2DockEntry.AppEntry -> LauncherUtils.launchApp(context, entry.app)
                                is V2DockEntry.FolderEntry -> openFolder = entry.folder
                            }
                        },
                        onDragStart = { bounds ->
                            draggedEntry = entry
                            dragOrigin = bounds.topLeft
                            dragPosition = bounds.topLeft
                        },
                        onDrag = { dragPosition += it },
                        onLongPressRelease = {
                            when (entry) {
                                is V2DockEntry.AppEntry -> appMenu = entry.app
                                is V2DockEntry.FolderEntry -> folderMenu = entry.folder
                            }
                        },
                        onDrop = {
                            val target = slotBounds.minByOrNull { (_, bounds) ->
                                val x = (bounds.left + bounds.right) / 2f - dragPosition.x
                                val y = (bounds.top + bounds.bottom) / 2f - dragPosition.y
                                x * x + y * y
                            }?.key
                            if (target != null && target != entry.slot) {
                                entries.firstOrNull { it.slot == target }?.let { move(it, entry.slot) }
                                move(entry, target)
                            }
                            draggedEntry = null
                        }
                    )
                }
            }
        }
    }

    appMenu?.let { app ->
        V2DockActionDialog(
            title = app.label,
            actions = listOf(
                "MAKE FOLDER" to { createFromApp = app to (app.gridPosition ?: 100); appMenu = null },
                "REMOVE FROM DOCK" to { onRemoveApp(app); appMenu = null }
            ),
            onDismiss = { appMenu = null }
        )
    }
    folderMenu?.let { folder ->
        V2DockActionDialog(
            title = folder.name,
            actions = listOf(
                "MANAGE APPS" to { manageFolder = folder; folderMenu = null },
                "REMOVE FROM DOCK" to { onRemoveFolder(folder.id); folderMenu = null }
            ),
            onDismiss = { folderMenu = null }
        )
    }
    createFromApp?.let { (app, slot) ->
        V2DockCreateFolderDialog(app.label, onDismiss = { createFromApp = null }) { name ->
            onCreateFolder(slot, name, listOf(app.appId))
            pendingFolderSlot = slot
            createFromApp = null
        }
    }
    manageFolder?.let { folder ->
        V2DockFolderAppsDialog(folder, allApps, iconPackPackage, onDismiss = { manageFolder = null }) { appIds ->
            onUpdateFolderApps(folder.id, appIds)
            openFolder = folder.copy(apps = allApps.filter { it.appId in appIds })
            manageFolder = null
        }
    }
    openFolder?.let { folder ->
        V2DockFolderContentsDialog(
            folder = folder,
            iconPackPackage = iconPackPackage,
            onDismiss = { openFolder = null },
            onManageApps = { manageFolder = folder; openFolder = null }
        )
    }
    pickAppSlot?.let { slot ->
        V2DockAppPickerDialog(allApps, iconPackPackage, onDismiss = { pickAppSlot = null }) { app ->
            onMoveApp(app, slot); pickAppSlot = null
        }
    }
}

@Composable
private fun V2DockEntryItem(
    entry: V2DockEntry,
    iconPackPackage: String,
    isDragging: Boolean,
    dragPosition: Offset,
    dragOrigin: Offset,
    onTap: () -> Unit,
    onDragStart: (androidx.compose.ui.geometry.Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onLongPressRelease: () -> Unit,
    onDrop: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val iconBitmap by produceState<ImageBitmap?>(null, entry, iconPackPackage) {
        val app = (entry as? V2DockEntry.AppEntry)?.app ?: return@produceState
        value = withContext(Dispatchers.IO) {
            com.unfold.core.ui.iconpack.IconPackResolver.resolveAppIconDrawable(context, app.packageName, iconPackPackage.takeIf { it.isNotBlank() })?.let(::drawableToImageBitmap)
        }
    }
    Box(
        modifier = Modifier.alpha(1f)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .offset { IntOffset((if (isDragging) dragPosition.x - dragOrigin.x else 0f).roundToInt(), (if (isDragging) dragPosition.y - dragOrigin.y else 0f).roundToInt()) }
            .pointerInput(entry) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) onTap() else {
                        onDragStart(bounds ?: return@awaitEachGesture)
                        var distance = 0f
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (distance < 14f) onLongPressRelease() else onDrop()
                                if (distance < 14f) onDrop()
                                break
                            }
                            val delta = change.position - change.previousPosition
                            distance += delta.getDistance(); change.consume(); onDrag(delta)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (entry) {
            is V2DockEntry.AppEntry -> CarvedIcon(
                size = 44.dp,
                raw = iconPackPackage.isNotBlank() && !com.unfold.core.ui.iconpack.IconPackResolver.isLauncherRingEnabled(context),
                icon = { if (iconBitmap != null) Image(iconBitmap!!, null, Modifier.fillMaxSize()) else Text(entry.app.label.take(2).uppercase()) },
                contentDescription = entry.app.label,
                onClick = onTap
            )
            is V2DockEntry.FolderEntry -> CarvedIcon(
                size = 44.dp,
                icon = { Icon(Icons.Default.Folder, entry.folder.name, tint = LocalUnfoldTheme.current.accentPrimary) },
                contentDescription = entry.folder.name,
                onClick = onTap
            )
        }
    }
}

@Composable
private fun V2DockCreateFolderDialog(defaultName: String, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    val theme = LocalUnfoldTheme.current
    var name by remember { mutableStateOf(defaultName) }
    V2DockLauncherDialog(onDismiss) {
        V2DockDialogTitle("CREATE FOLDER")
        V2DockThemedTextInput(value = name, onValueChange = { name = it }, placeholder = "FOLDER NAME")
        V2DockDialogButtons(onDismiss) { if (name.isNotBlank()) onCreate(name.trim()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2DockFolderAppsDialog(folder: com.unfold.core.domain.model.FolderInfo, allApps: List<AppInfo>, iconPackPackage: String, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var selectedIds by remember(folder.id, folder.apps) { mutableStateOf(folder.apps.map { it.appId }.toSet()) }
    V2DockLauncherDialog(onDismiss) {
        V2DockDialogTitle("MANAGE ${folder.name}")
        Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
            allApps.sortedBy { it.label }.forEach { app ->
                V2DockManageAppRow(
                    app = app,
                    iconPackPackage = iconPackPackage,
                    selected = app.appId in selectedIds,
                    onToggle = { selectedIds = selectedIds.toggle(app.appId) }
                )
            }
        }
        V2DockDialogButtons(onDismiss) { onSave(selectedIds.toList()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2DockAppPickerDialog(allApps: List<AppInfo>, iconPackPackage: String, onDismiss: () -> Unit, onPick: (AppInfo) -> Unit) {
    var selectedAppId by remember { mutableStateOf<String?>(null) }
    V2DockLauncherDialog(onDismiss) {
        V2DockDialogTitle("PIN TO DOCK")
        Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
            allApps.sortedBy { it.label }.forEach { app ->
                V2DockManageAppRow(
                    app = app,
                    iconPackPackage = iconPackPackage,
                    selected = selectedAppId == app.appId,
                    onToggle = { selectedAppId = if (selectedAppId == app.appId) null else app.appId }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = LocalUnfoldTheme.current.textSecondary) }
            TextButton(onClick = {
                allApps.firstOrNull { it.appId == selectedAppId }?.let(onPick)
            }) { Text("PIN", color = LocalUnfoldTheme.current.accentPrimary) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2DockFolderContentsDialog(
    folder: com.unfold.core.domain.model.FolderInfo,
    iconPackPackage: String,
    onDismiss: () -> Unit,
    onManageApps: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    V2DockLauncherDialog(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            V2DockDialogTitle(folder.name, Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("CLOSE", color = theme.textSecondary) }
        }
        if (folder.apps.isEmpty()) {
            Text("No apps in this folder yet.", color = theme.textSecondary)
        } else {
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                folder.apps.chunked(4).forEach { rowApps ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowApps.forEach { app -> V2DockFolderAppCell(app, iconPackPackage, onDismiss, Modifier.weight(1f)) }
                        repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        TextButton(onClick = onManageApps, modifier = Modifier.align(Alignment.End)) { Text("MANAGE APPS", color = theme.accentPrimary) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2DockActionDialog(
    title: String,
    actions: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit
) {
    V2DockLauncherDialog(onDismiss) {
        V2DockDialogTitle(title.uppercase())
        actions.forEach { (label, action) ->
            Text(
                text = label,
                color = LocalUnfoldTheme.current.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = action, onLongClick = {}).padding(horizontal = 10.dp, vertical = 12.dp)
            )
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("CLOSE", color = LocalUnfoldTheme.current.textSecondary) }
    }
}

@Composable
private fun V2DockLauncherDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalUnfoldTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(18.dp))
                .background(theme.bgPanel.copy(alpha = 0.84f))
                .border(2.dp, theme.accentPrimary.copy(alpha = 0.82f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun V2DockDialogTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, color = LocalUnfoldTheme.current.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
}

@Composable
private fun V2DockDialogButtons(onDismiss: () -> Unit, onSave: () -> Unit) {
    val theme = LocalUnfoldTheme.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text("CANCEL", color = theme.textSecondary) }
        TextButton(onClick = onSave) { Text("SAVE", color = theme.accentPrimary) }
    }
}

@Composable
private fun V2DockThemedTextInput(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val theme = LocalUnfoldTheme.current
    Box(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, theme.panelBorder.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .background(theme.bgVoid.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        if (value.isBlank()) Text(placeholder, color = theme.textMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = theme.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2DockManageAppRow(app: AppInfo, iconPackPackage: String, selected: Boolean, onToggle: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val theme = LocalUnfoldTheme.current
    val iconBitmap by produceState<ImageBitmap?>(null, app.appId, iconPackPackage) {
        value = withContext(Dispatchers.IO) {
            com.unfold.core.ui.iconpack.IconPackResolver.resolveAppIconDrawable(context, app.packageName, iconPackPackage.takeIf { it.isNotBlank() })?.let(::drawableToImageBitmap)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(theme.bgVoid.copy(alpha = if (selected) 0.42f else 0.18f))
            .border(1.dp, theme.panelBorder.copy(alpha = if (selected) 0.5f else 0.2f), RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onToggle, onLongClick = {}).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CarvedIcon(
            size = 34.dp,
            raw = iconPackPackage.isNotBlank() && !com.unfold.core.ui.iconpack.IconPackResolver.isLauncherRingEnabled(context),
            icon = { if (iconBitmap != null) Image(iconBitmap!!, app.label, Modifier.fillMaxSize()) else Text(app.label.take(2).uppercase(), color = theme.accentPrimary, fontSize = 9.sp) },
            contentDescription = app.label,
            onClick = onToggle
        )
        Text(app.label, color = theme.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V2DockFolderAppCell(app: AppInfo, iconPackPackage: String, onFolderDismiss: () -> Unit, modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val theme = LocalUnfoldTheme.current
    val iconBitmap by produceState<ImageBitmap?>(null, app.appId, iconPackPackage) {
        value = withContext(Dispatchers.IO) {
            com.unfold.core.ui.iconpack.IconPackResolver.resolveAppIconDrawable(context, app.packageName, iconPackPackage.takeIf { it.isNotBlank() })?.let(::drawableToImageBitmap)
        }
    }
    Column(
        modifier = modifier.combinedClickable(onClick = { LauncherUtils.launchApp(context, app); onFolderDismiss() }, onLongClick = {}).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CarvedIcon(
            size = 44.dp,
            raw = iconPackPackage.isNotBlank() && !com.unfold.core.ui.iconpack.IconPackResolver.isLauncherRingEnabled(context),
            icon = { if (iconBitmap != null) Image(iconBitmap!!, app.label, Modifier.fillMaxSize()) else Text(app.label.take(2).uppercase(), color = theme.accentPrimary, fontSize = 10.sp) },
            contentDescription = app.label,
            onClick = { LauncherUtils.launchApp(context, app); onFolderDismiss() }
        )
        Text(app.label, color = theme.textSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value

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
            .alpha(1f)
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
