package com.volt.feature.home

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import android.provider.AlarmClock
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.window.PopupProperties
import com.volt.core.domain.model.AppInfo
import com.volt.core.domain.model.DockBackgroundMode
import com.volt.core.domain.model.DockRowsMode
import com.volt.core.domain.model.WallpaperMode
import com.volt.core.domain.model.WallpaperPatternMode
import com.volt.core.ui.components.*
import com.volt.core.ui.components.hud.*
import com.volt.core.ui.theme.LocalVoltTheme
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalVoltTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 6 })
    val railNodes = listOf(
        RailNode("home", Icons.Default.Home, "Home"),
        RailNode("system", Icons.Default.Build, "System"),
        RailNode("media", Icons.Default.PlayArrow, "Media")
    )

    // Separate grid apps (positions < 100) and dock apps (positions >= 100)
    val gridApps = state.gridApps.filter { (it.gridPosition ?: 0) < 100 }
    val dockApps = state.gridApps.filter { (it.gridPosition ?: 0) >= 100 }
    val dockRows = if (state.dockRowsMode == DockRowsMode.TWO_ROWS) 2 else 1
    val dockVisible = state.dockRowsMode != DockRowsMode.HIDDEN
    val dockVisibleCount = state.dockIconCount.coerceIn(0, 6)
    val dockColumns = dockVisibleCount
    val dockSolidColor = remember(state.dockBackgroundHex) {
        runCatching { Color(android.graphics.Color.parseColor(state.dockBackgroundHex)) }
            .getOrElse { theme.bgPanel }
    }
    val rows = state.gridRows
    val columns = state.gridColumns
    val gridIconSize = remember(state.homeIconSize, columns) {
        launcherGridIconSize(state.homeIconSize, columns)
    }
    val gridSlotHeight = gridIconSize + if (state.homeLabelsEnabled) 28.dp else 14.dp

    var draggedApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val slotBounds = remember { mutableMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    val itemBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bgVoid)
            .combinedClickable(
                onClick = {},
                onLongClick = onNavigateToSettings
            )
    ) {
        LauncherWallpaperBackdrop(
            modifier = Modifier.fillMaxSize(),
            mode = state.homeWallpaperMode,
            colorHex = state.homeWallpaperHex,
            pattern = state.homeWallpaperPattern,
            imageUri = state.homeWallpaperImageUri,
            fallbackColor = theme.bgVoid
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
        Spacer(modifier = Modifier.height(2.dp))
        // 1. Top HUD Area (HUD panels nested inside NodeRail horizontally, stretches dynamically)
        var flashlightEnabled by remember { mutableStateOf(false) }
        var isSilentEnabled by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Background PCB grid
            HudBackgroundGrid()

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Horizontal Rail
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudRailItem(
                        icon = Icons.Default.Home,
                        isSelected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                    )
                    HudTrace(horizontal = true, length = 16.dp)
                    HudRailItem(
                        icon = Icons.Default.PlayArrow,
                        isSelected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                    )
                    HudTrace(horizontal = true, length = 16.dp)
                    HudRailItem(
                        icon = Icons.Default.Build,
                        isSelected = pagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                    )
                    HudTrace(horizontal = true, length = 16.dp)
                    
                    // Flashlight Rail Item
                    val context = LocalContext.current
                    HudRailItem(
                        icon = Icons.Default.Info, // Flashlight representation
                        isSelected = flashlightEnabled,
                        onClick = {
                            flashlightEnabled = !flashlightEnabled
                            try {
                                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                                val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                                if (cameraId != null) {
                                    cameraManager.setTorchMode(cameraId, flashlightEnabled)
                                }
                            } catch (e: Exception) {
                                Log.e("Flashlight", "Error toggling flashlight: ${e.message}")
                            }
                        }
                    )
                    
                    HudTrace(horizontal = true, length = 12.dp)
                    HudConnectorNode()
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // SILENT Chip
                    StatusChip(
                        text = if (isSilentEnabled) "SILENT" else "GENERAL",
                        isActive = isSilentEnabled,
                        modifier = Modifier.clickable { isSilentEnabled = !isSilentEnabled }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Content + Left Rail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Vertical Rail
                    Column(
                        modifier = Modifier
                            .width(46.dp)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val context = LocalContext.current
                        
                        HudTrace(horizontal = false, length = 8.dp)
                        
                        // Google Feed / Intel
                        HudRailItem(
                            icon = Icons.Default.Search,
                            isSelected = pagerState.currentPage == 3,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(3) }
                            }
                        )
                        HudTrace(horizontal = false, length = 8.dp)
                        
                        // Widgets
                        HudRailItem(
                            icon = Icons.Default.Info,
                            isSelected = pagerState.currentPage == 4,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(4) }
                            }
                        )
                        HudTrace(horizontal = false, length = 8.dp)
                        
                        // Folders / Category Org
                        HudRailItem(
                            icon = Icons.Default.Menu,
                            isSelected = pagerState.currentPage == 5,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(5) }
                            }
                        )
                        HudTrace(horizontal = false, length = 8.dp)
                        
                        HudConnectorNode()
                        
                        // Battery display (text rotated)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (state.systemStats?.batteryText ?: "100%").uppercase(),
                            color = theme.accentPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        HudConnectorNode()
                        
                        // Flexible vertical trace that expands and shrinks with the screen
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            val traceColor = theme.accentPrimary.copy(alpha = 0.4f)
                            Canvas(modifier = Modifier.fillMaxHeight().width(8.dp)) {
                                drawLine(
                                    color = traceColor,
                                    start = Offset(size.width / 2f, 0f),
                                    end = Offset(size.width / 2f, size.height),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        }
                        
                        // Settings Row with horizontal trace extending to the right
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)
                        ) {
                            HudRailItem(
                                icon = Icons.Default.Settings,
                                isSelected = false,
                                onClick = onNavigateToSettings
                            )
                            HudTrace(horizontal = true, length = 36.dp)
                            HudConnectorNode()
                        }
                    }

                    // Main HUD content page
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val hudPageModifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = when (rows) {
                                        1 -> 14.dp
                                        2 -> 12.dp
                                        else -> 10.dp
                                    },
                                    bottom = when (rows) {
                                        1 -> 16.dp
                                        2 -> 14.dp
                                        else -> 12.dp
                                    },
                                    start = 12.dp,
                                    end = 12.dp
                                )

                            when (page) {
                                0 -> HudHome(modifier = hudPageModifier, gridRows = rows)
                                1 -> HudMusic(modifier = hudPageModifier, gridRows = rows)
                                2 -> HudSystem(
                                    batteryPercent = state.systemStats?.batteryPercent ?: 0.5f,
                                    batteryText = state.systemStats?.batteryText ?: "50%",
                                    ramUsedText = state.systemStats?.ramUsedText ?: "4.2 GB / 8.0 GB",
                                    ramUsedPercent = state.systemStats?.ramUsedPercent ?: 0.5f,
                                    storageUsedText = state.systemStats?.storageUsedText ?: "64 GB / 128 GB",
                                    storageUsedPercent = state.systemStats?.storageUsedPercent ?: 0.5f,
                                    cpuTempText = state.systemStats?.cpuTempText ?: "36°C",
                                    cpuTemp = state.systemStats?.cpuTemp ?: 36f,
                                    modifier = hudPageModifier,
                                    gridRows = rows
                                )
                                3 -> HudGoogleFeed(modifier = hudPageModifier, gridRows = rows)
                                4 -> HudWidgets(modifier = hudPageModifier, gridRows = rows)
                                5 -> HudCategories(apps = state.gridApps, modifier = hudPageModifier, gridRows = rows)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Home App Grid Area (placed at the bottom, just above the dock!)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(theme.bgPanel.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .border(1.dp, theme.panelBorder.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (c in 0 until columns) {
                        val index = r * columns + c
                        val app = gridApps.firstOrNull { it.gridPosition == index }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(gridSlotHeight)
                                .onGloballyPositioned { coords ->
                                    slotBounds[index] = coords.boundsInRoot()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (app != null) {
                                val isDraggingThisApp = draggedApp?.packageName == app.packageName
                                var showContextMenu by remember(app.packageName) { mutableStateOf(false) }
                                var dragDistance by remember(app.packageName) { mutableStateOf(0f) }
                                Box(
                                    modifier = Modifier
                                        .alpha(if (isDraggingThisApp) 0.3f else 1.0f)
                                        .onGloballyPositioned { coords ->
                                            itemBounds[app.packageName] = coords.boundsInRoot()
                                        }
                                        .pointerInput(app.packageName) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                Log.d("UnfoldDrag", "Pointer down on app: ${app.label}")
                                                val longPress = awaitLongPressOrCancellation(down.id)
                                                if (longPress != null) {
                                                    Log.d("UnfoldDrag", "Long press triggered for: ${app.label}")
                                                    // Long press triggered: start drag!
                                                    draggedApp = app
                                                    val bounds = itemBounds[app.packageName]
                                                    Log.d("UnfoldDrag", "Item bounds: $bounds")
                                                    dragPosition = bounds?.topLeft ?: Offset.Zero
                                                    Log.d("UnfoldDrag", "Initial dragPosition: $dragPosition")
                                                    dragDistance = 0f
                                                    val currentDownId = down.id
                                                    
                                                    try {
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            val change = event.changes.firstOrNull { it.id == currentDownId } ?: break
                                                            Log.d("UnfoldDrag", "Pointer change: pressed=${change.pressed}, isConsumed=${change.isConsumed}")
                                                            if (!change.pressed) {
                                                                val dragBounds = Rect(
                                                                    left = dragPosition.x,
                                                                    top = dragPosition.y,
                                                                    right = dragPosition.x + (bounds?.width ?: 0f),
                                                                    bottom = dragPosition.y + (bounds?.height ?: 0f)
                                                                )
                                                                if (dragDistance < 15f) {
                                                                    showContextMenu = true
                                                                } else {
                                                                    handleAppDrop(
                                                                        app = app,
                                                                        dropBounds = dragBounds,
                                                                        slotBounds = slotBounds,
                                                                        allApps = state.gridApps,
                                                                        dockCapacity = dockVisibleCount * dockRows,
                                                                        sourcePosition = app.gridPosition,
                                                                        viewModel = viewModel
                                                                    )
                                                                }
                                                                draggedApp = null
                                                                break
                                                            }
                                                            val dragAmount = change.position - change.previousPosition
                                                            change.consume()
                                                            dragPosition += dragAmount
                                                            dragDistance += dragAmount.getDistance()
                                                            Log.d("UnfoldDrag", "Dragged. dragAmount: $dragAmount, new dragPosition: $dragPosition, totalDistance: $dragDistance")
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("UnfoldDrag", "Error in drag loop", e)
                                                        draggedApp = null
                                                    }
                                                } else {
                                                    Log.d("UnfoldDrag", "Long press returned null (cancelled/tapped)")
                                                    // Released before long press: click!
                                                    try {
                                                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        intent?.let { context.startActivity(it) }
                                                    } catch (e: Exception) {
                                                        // ignored
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    HomeAppGridItem(
                                        app = app,
                                        iconSize = gridIconSize,
                                        showLabel = state.homeLabelsEnabled,
                                        onClick = {}
                                    )

                                    if (showContextMenu) {
                                        DropdownMenu(
                                            expanded = showContextMenu,
                                            onDismissRequest = { showContextMenu = false },
                                            modifier = Modifier
                                                .border(1.dp, theme.panelBorder, RoundedCornerShape(18.dp)),
                                            shape = RoundedCornerShape(18.dp),
                                            containerColor = theme.bgPanel.copy(alpha = 0.96f),
                                            tonalElevation = 0.dp,
                                            shadowElevation = 14.dp,
                                            properties = PopupProperties(focusable = true)
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "REMOVE FROM HOME",
                                                        color = theme.textPrimary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.onIntent(HomeUiIntent.UnpinApp(app.packageName))
                                                    showContextMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }

        if (dockVisible) {
            Spacer(modifier = Modifier.height(24.dp))

            val dockHeight = when (dockRows) {
                2 -> 140.dp
                else -> 88.dp
            }

            val dockContent: @Composable () -> Unit = {
                val calculatedSize = state.dockIconSize.dp.coerceAtMost(56.dp).coerceAtLeast(36.dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    repeat(dockRows) { rowIndex ->
                        if (dockRows == 1) {
                            val sortedDockApps = dockApps.sortedBy { it.gridPosition }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(calculatedSize)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .matchParentSize(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(dockColumns) { colIndex ->
                                        val dockIdx = 100 + colIndex
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .onGloballyPositioned { coords ->
                                                    slotBounds[dockIdx] = coords.boundsInRoot()
                                                }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .matchParentSize(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    sortedDockApps.forEach { app ->
                                        val isDraggingThisApp = draggedApp?.packageName == app.packageName
                                        val iconDrawable = androidx.compose.runtime.remember(app.packageName) {
                                            try {
                                                context.packageManager.getApplicationIcon(app.packageName)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                        val dockIconDiameter = if (calculatedSize < state.dockIconSize.dp) {
                                            calculatedSize
                                        } else {
                                            state.dockIconSize.dp
                                        }
                                        var showDockMenu by remember(app.packageName) { mutableStateOf(false) }
                                        var dragDistance by remember(app.packageName) { mutableStateOf(0f) }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .onGloballyPositioned { coords ->
                                                    itemBounds[app.packageName] = coords.boundsInRoot()
                                                }
                                                .pointerInput(app.packageName) {
                                                    awaitEachGesture {
                                                        val down = awaitFirstDown(requireUnconsumed = false)
                                                        val longPress = awaitLongPressOrCancellation(down.id)
                                                        if (longPress != null) {
                                                            draggedApp = app
                                                            dragPosition = itemBounds[app.packageName]?.topLeft ?: Offset.Zero
                                                            dragDistance = 0f
                                                            val currentDownId = down.id
                                                            while (true) {
                                                                val event = awaitPointerEvent()
                                                                val change = event.changes.firstOrNull { it.id == currentDownId } ?: break
                                                                if (!change.pressed) {
                                                                    val itemWidth = itemBounds[app.packageName]?.width ?: 0f
                                                                    val itemHeight = itemBounds[app.packageName]?.height ?: 0f
                                                                    val dropBounds = Rect(
                                                                        left = dragPosition.x,
                                                                        top = dragPosition.y,
                                                                        right = dragPosition.x + itemWidth,
                                                                        bottom = dragPosition.y + itemHeight
                                                                    )
                                                                    if (dragDistance < 15f) {
                                                                        showDockMenu = true
                                                                    } else {
                                                                        handleAppDrop(
                                                                            app = app,
                                                                            dropBounds = dropBounds,
                                                                            slotBounds = slotBounds,
                                                                            allApps = state.gridApps,
                                                                            dockCapacity = dockVisibleCount * dockRows,
                                                                            sourcePosition = app.gridPosition,
                                                                            viewModel = viewModel
                                                                        )
                                                                    }
                                                                    draggedApp = null
                                                                    break
                                                                }
                                                                val dragAmount = change.position - change.previousPosition
                                                                change.consume()
                                                                dragPosition += dragAmount
                                                                dragDistance += dragAmount.getDistance()
                                                            }
                                                        } else {
                                                            try {
                                                                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                                intent?.let { context.startActivity(it) }
                                                            } catch (e: Exception) {
                                                                // ignored
                                                            }
                                                        }
                                                    }
                                                }
                                        ) {
                                            CarvedIcon(
                                                size = dockIconDiameter,
                                                icon = {
                                                    val imageBitmap = remember(app.packageName) {
                                                        iconDrawable?.let { drawableToImageBitmap(it) }
                                                    }
                                                    if (imageBitmap != null) {
                                                        Image(
                                                            bitmap = imageBitmap,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Text(
                                                            text = app.label.take(2).uppercase(),
                                                            color = theme.accentPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                },
                                                contentDescription = app.label,
                                                onClick = {}
                                            )

                                            if (showDockMenu) {
                                                DropdownMenu(
                                                    expanded = showDockMenu,
                                                    onDismissRequest = { showDockMenu = false },
                                                    modifier = Modifier
                                                        .border(1.dp, theme.panelBorder, RoundedCornerShape(18.dp)),
                                                    shape = RoundedCornerShape(18.dp),
                                                    containerColor = theme.bgPanel.copy(alpha = 0.96f),
                                                    tonalElevation = 0.dp,
                                                    shadowElevation = 14.dp,
                                                    properties = PopupProperties(focusable = true)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = "REMOVE FROM DOCK",
                                                                color = theme.textPrimary,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        },
                                                        onClick = {
                                                            viewModel.onIntent(HomeUiIntent.UnpinApp(app.packageName))
                                                            showDockMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(dockColumns) { colIndex ->
                                    val slotIndex = rowIndex * dockColumns + colIndex
                                    val dockIdx = 100 + slotIndex
                                    val app = dockApps.firstOrNull { it.gridPosition == dockIdx }

                                    // For a single-row dock we want slots to expand evenly across
                                    // the available width. For two-row mode we keep fixed sized
                                    // slots to preserve the grid-like appearance.
                                    val slotModifier = if (dockRows == 1) {
                                        Modifier
                                            .weight(1f)
                                            .height(calculatedSize)
                                            .onGloballyPositioned { coords ->
                                                slotBounds[dockIdx] = coords.boundsInRoot()
                                            }
                                    } else {
                                        Modifier
                                            .size(calculatedSize)
                                            .onGloballyPositioned { coords ->
                                                slotBounds[dockIdx] = coords.boundsInRoot()
                                            }
                                    }

                                    Box(
                                        modifier = slotModifier,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (app != null) {
                                            val isDraggingThisApp = draggedApp?.packageName == app.packageName
                                            val iconDrawable = androidx.compose.runtime.remember(app.packageName) {
                                                try {
                                                    context.packageManager.getApplicationIcon(app.packageName)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }
                                            val dockIconDiameter = if (calculatedSize < state.dockIconSize.dp) {
                                                calculatedSize
                                            } else {
                                                state.dockIconSize.dp
                                            }
                                            var showDockMenu by remember(app.packageName) { mutableStateOf(false) }
                                            var dragDistance by remember(app.packageName) { mutableStateOf(0f) }
                                            Box(
                                                modifier = Modifier
                                                    .alpha(if (isDraggingThisApp) 0.3f else 1.0f)
                                                    .onGloballyPositioned { coords ->
                                                        itemBounds[app.packageName] = coords.boundsInRoot()
                                                    }
                                                    .pointerInput(app.packageName) {
                                                        awaitEachGesture {
                                                            val down = awaitFirstDown(requireUnconsumed = false)
                                                            val longPress = awaitLongPressOrCancellation(down.id)
                                                            if (longPress != null) {
                                                                draggedApp = app
                                                                dragPosition = itemBounds[app.packageName]?.topLeft ?: Offset.Zero
                                                                dragDistance = 0f
                                                                val currentDownId = down.id
                                                                while (true) {
                                                                    val event = awaitPointerEvent()
                                                                    val change = event.changes.firstOrNull { it.id == currentDownId } ?: break
                                                                    if (!change.pressed) {
                                                                        val itemWidth = itemBounds[app.packageName]?.width ?: 0f
                                                                        val itemHeight = itemBounds[app.packageName]?.height ?: 0f
                                                                        val dropBounds = Rect(
                                                                            left = dragPosition.x,
                                                                            top = dragPosition.y,
                                                                            right = dragPosition.x + itemWidth,
                                                                            bottom = dragPosition.y + itemHeight
                                                                        )
                                                                        if (dragDistance < 15f) {
                                                                            showDockMenu = true
                                                                        } else {
                                                                            handleAppDrop(
                                                                                app = app,
                                                                                dropBounds = dropBounds,
                                                                                slotBounds = slotBounds,
                                                                                allApps = state.gridApps,
                                                                                dockCapacity = dockVisibleCount * dockRows,
                                                                                sourcePosition = app.gridPosition,
                                                                                viewModel = viewModel
                                                                            )
                                                                        }
                                                                        draggedApp = null
                                                                        break
                                                                    }
                                                                    val dragAmount = change.position - change.previousPosition
                                                                    change.consume()
                                                                    dragPosition += dragAmount
                                                                    dragDistance += dragAmount.getDistance()
                                                                }
                                                            } else {
                                                                try {
                                                                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                    }
                                                                    intent?.let { context.startActivity(it) }
                                                                } catch (e: Exception) {
                                                                    // ignored
                                                                }
                                                            }
                                                        }
                                                    }
                                            ) {
                                                CarvedIcon(
                                                    size = dockIconDiameter,
                                                    icon = {
                                                        val imageBitmap = remember(app.packageName) {
                                                            iconDrawable?.let { drawableToImageBitmap(it) }
                                                        }
                                                        if (imageBitmap != null) {
                                                            Image(
                                                                bitmap = imageBitmap,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Text(
                                                                text = app.label.take(2).uppercase(),
                                                                color = theme.accentPrimary,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    },
                                                    contentDescription = app.label,
                                                    onClick = {}
                                                )

                                                if (showDockMenu) {
                                                    DropdownMenu(
                                                        expanded = showDockMenu,
                                                        onDismissRequest = { showDockMenu = false },
                                                        modifier = Modifier
                                                            .border(1.dp, theme.panelBorder, RoundedCornerShape(18.dp)),
                                                        shape = RoundedCornerShape(18.dp),
                                                        containerColor = theme.bgPanel.copy(alpha = 0.96f),
                                                        tonalElevation = 0.dp,
                                                        shadowElevation = 14.dp,
                                                        properties = PopupProperties(focusable = true)
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = "REMOVE FROM DOCK",
                                                                    color = theme.textPrimary,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            },
                                                            onClick = {
                                                                viewModel.onIntent(HomeUiIntent.UnpinApp(app.packageName))
                                                                showDockMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // Empty slot: keep inner spacing similar to icon size
                                            Spacer(modifier = Modifier.size(calculatedSize - 12.dp))
                                        }
                                    }
                                }
                            }
                        }
                        if (dockRows == 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            when (state.dockBackgroundMode) {
                DockBackgroundMode.TRANSPARENT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dockHeight)
                            .padding(horizontal = 16.dp)
                    ) {
                        dockContent()
                    }
                }
                DockBackgroundMode.SOLID -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dockHeight)
                            .padding(horizontal = 16.dp)
                            .background(dockSolidColor.copy(alpha = 0.96f), RoundedCornerShape(24.dp))
                            .border(1.dp, theme.panelBorder, RoundedCornerShape(24.dp))
                    ) {
                        dockContent()
                    }
                }
                DockBackgroundMode.BLUR -> {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dockHeight)
                            .padding(horizontal = 16.dp),
                        cornerRadius = 24.dp,
                        opacity = 0.52f
                    ) {
                        dockContent()
                    }
                }
                DockBackgroundMode.DEFAULT -> {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dockHeight)
                            .padding(horizontal = 16.dp),
                        cornerRadius = 24.dp
                    ) {
                        dockContent()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Floating drag preview overlay
        draggedApp?.let { app ->
            val iconDrawable = remember(app.packageName) {
                try {
                    context.packageManager.getApplicationIcon(app.packageName)
                } catch (e: Exception) {
                    null
                }
            }
            val density = androidx.compose.ui.platform.LocalDensity.current
            val statusBarHeight = WindowInsets.statusBars.getTop(density)
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(
                    dragPosition.x.toInt(),
                    (dragPosition.y - statusBarHeight).toInt()
                ),
                properties = androidx.compose.ui.window.PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    excludeFromSystemGesture = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp, 72.dp)
                        .alpha(0.8f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CarvedIcon(
                            size = gridIconSize,
                            icon = {
                                val imageBitmap = remember(app.packageName) {
                                    iconDrawable?.let { drawableToImageBitmap(it) }
                                }
                                if (imageBitmap != null) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = app.label.take(2).uppercase(),
                                        color = theme.accentPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            contentDescription = app.label
                        )
                        Text(
                            text = app.label,
                            color = theme.textSecondary,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
}


@Composable
private fun LauncherWallpaperBackdrop(
    modifier: Modifier = Modifier,
    mode: WallpaperMode,
    colorHex: String,
    pattern: WallpaperPatternMode,
    imageUri: String,
    fallbackColor: Color
) {
    val baseColor = remember(colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
            .getOrElse { fallbackColor }
    }
    Box(modifier = modifier) {
        when (mode) {
            WallpaperMode.SOLID -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseColor)
                )
            }
            WallpaperMode.PATTERN -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseColor)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tint = Color.White.copy(alpha = 0.05f)
                        when (pattern) {
                            WallpaperPatternMode.GEOMETRIC -> {
                                repeat(7) { index ->
                                    val size = (40 + index * 18).dp.toPx()
                                    drawCircle(
                                        color = tint,
                                        radius = size,
                                        center = Offset(size * 1.8f, size * 0.9f + index * 110f)
                                    )
                                }
                            }
                            WallpaperPatternMode.ABSTRACT -> {
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
                            WallpaperPatternMode.MINIMAL -> {
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
            WallpaperMode.CUSTOM -> {
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

@Composable
fun HomeAppGridItem(
    app: AppInfo,
    iconSize: Dp,
    showLabel: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalVoltTheme.current
    val context = LocalContext.current
    val iconDrawable = androidx.compose.runtime.remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .width(iconSize + 12.dp)
            .height(iconSize + if (showLabel) 26.dp else 12.dp)
    ) {
        CarvedIcon(
            size = iconSize,
            icon = {
                val imageBitmap = remember(app.packageName) {
                    iconDrawable?.let { drawableToImageBitmap(it) }
                }
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = app.label.take(2).uppercase(),
                        color = theme.accentPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            contentDescription = app.label,
            onClick = onClick
        )
        if (showLabel) {
            Text(
                text = app.label,
                color = theme.textSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun launcherGridIconSize(rawSize: Int, columns: Int): Dp {
    val normalized = ((rawSize.coerceIn(30, 100) - 30) / 70f).coerceIn(0f, 1f)
    val maxSize = when (columns.coerceIn(3, 6)) {
        3 -> 56f
        4 -> 50f
        5 -> 44f
        else -> 40f
    }
    val minSize = when (columns.coerceIn(3, 6)) {
        3 -> 34f
        4 -> 32f
        5 -> 30f
        else -> 28f
    }
    return (minSize + (maxSize - minSize) * normalized).dp
}

@Composable
fun ClockWeatherPanel() {
    val theme = LocalVoltTheme.current
    GlassPanel(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "10:42",
                color = theme.textPrimary,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "SYSTEM ACTIVE",
                color = theme.accentPrimary,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            PillBadge(text = "sunny / 32°C", tint = theme.accentSecondary)
        }
    }
}

@Composable
fun SystemHUDPanel(state: HomeUiState) {
    val theme = LocalVoltTheme.current
    val stats = state.systemStats

    GlassPanel(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SYSTEM STATUS",
                color = theme.textPrimary,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HUDGauge(
                    value = stats?.ramUsedPercent ?: 0.45f,
                    label = "RAM",
                    valueText = stats?.ramUsedText?.split("/")?.firstOrNull()?.trim() ?: "4.2 GB"
                )
                HUDGauge(
                    value = stats?.batteryPercent ?: 0.85f,
                    label = "Battery",
                    valueText = stats?.batteryText ?: "85%",
                    ringColor = theme.accentSecondary
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TEMP:",
                    color = theme.textSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                PillBadge(
                    text = stats?.cpuTempText ?: "36°C",
                    tint = theme.accentWarn
                )
            }
        }
    }
}

@Composable
fun MediaPanel() {
    val theme = LocalVoltTheme.current
    GlassPanel(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NOW PLAYING",
                color = theme.textSecondary,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unfold Ambient Stream",
                color = theme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Control Board Audio",
                color = theme.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarvedIcon(
                    size = 36.dp,
                    icon = { Text("<", color = theme.textPrimary, fontSize = 14.sp) },
                    contentDescription = "Prev"
                )
                CarvedIcon(
                    size = 48.dp,
                    icon = { Icon(Icons.Default.PlayArrow, null, tint = theme.accentPrimary) },
                    contentDescription = "Play"
                )
                CarvedIcon(
                    size = 36.dp,
                    icon = { Text(">", color = theme.textPrimary, fontSize = 14.sp) },
                    contentDescription = "Next"
                )
            }
        }
    }
}

private fun handleAppDrop(
    app: AppInfo,
    dropBounds: Rect,
    slotBounds: Map<Int, androidx.compose.ui.geometry.Rect>,
    allApps: List<AppInfo>,
    dockCapacity: Int,
    sourcePosition: Int?,
    viewModel: HomeViewModel
) {
    val targetSlot = resolveDropTarget(dropBounds, slotBounds, sourcePosition, allApps)
    if (targetSlot != null) {
        val sourcePos = app.gridPosition ?: -1
        val appAtTarget = allApps.firstOrNull { it.gridPosition == targetSlot }

        if (targetSlot >= 100 && sourcePos < 100) {
            // Moving from Grid to Dock
            val dockAppsCount = allApps.count { (it.gridPosition ?: 0) >= 100 }
            if (dockAppsCount >= dockCapacity && appAtTarget == null) {
                // Dock is full and target is empty slot: reject drag (goes back)
                return
            }
        }

        // Move app to target
        viewModel.onIntent(HomeUiIntent.MoveApp(app.packageName, targetSlot))
        if (appAtTarget != null) {
            // Swap: move the other app to source slot
            viewModel.onIntent(HomeUiIntent.MoveApp(appAtTarget.packageName, sourcePos))
        }
    }
}

private fun resolveDropTarget(
    dropBounds: Rect,
    slotBounds: Map<Int, androidx.compose.ui.geometry.Rect>,
    sourcePosition: Int?,
    allApps: List<AppInfo>
): Int? {
    if (slotBounds.isEmpty()) return null

    val expandedDropBounds = Rect(
        left = dropBounds.left - 24f,
        top = dropBounds.top - 24f,
        right = dropBounds.right + 24f,
        bottom = dropBounds.bottom + 24f
    )

    val overlaps = slotBounds.entries
        .mapNotNull { (slot, bounds) ->
            val overlapLeft = maxOf(expandedDropBounds.left, bounds.left)
            val overlapTop = maxOf(expandedDropBounds.top, bounds.top)
            val overlapRight = minOf(expandedDropBounds.right, bounds.right)
            val overlapBottom = minOf(expandedDropBounds.bottom, bounds.bottom)
            if (overlapRight > overlapLeft && overlapBottom > overlapTop) {
                val overlapArea = (overlapRight - overlapLeft) * (overlapBottom - overlapTop)
                slot to overlapArea
            } else {
                null
            }
        }

    val dropCenter = Offset(
        x = (dropBounds.left + dropBounds.right) / 2f,
        y = (dropBounds.top + dropBounds.bottom) / 2f
    )

    fun nearestSlot(boundsMap: Map<Int, androidx.compose.ui.geometry.Rect>): Int? {
        if (boundsMap.isEmpty()) return null
        return boundsMap.entries.minByOrNull { (_, bounds) ->
            val slotCenter = Offset(
                x = (bounds.left + bounds.right) / 2f,
                y = (bounds.top + bounds.bottom) / 2f
            )
            val dx = dropCenter.x - slotCenter.x
            val dy = dropCenter.y - slotCenter.y
            dx * dx + dy * dy
        }?.key
    }

    val homeSlots = slotBounds.filterKeys { it < 100 }
    val dockSlots = slotBounds.filterKeys { it >= 100 }
    val dockTop = dockSlots.values.minOfOrNull { it.top } ?: Float.MAX_VALUE
    val homeBottom = homeSlots.values.maxOfOrNull { it.bottom } ?: Float.MIN_VALUE
    val homeDockBoundary = if (dockSlots.isNotEmpty() && homeSlots.isNotEmpty()) {
        (homeBottom + dockTop) / 2f
    } else {
        Float.MAX_VALUE
    }

    val droppingInDock = dropCenter.y >= homeDockBoundary
    val emptyDockSlotBounds = dockSlots.filter { (slot, _) ->
        allApps.none { it.gridPosition == slot }
    }

    if (sourcePosition != null && sourcePosition < 100 && droppingInDock) {
        nearestSlot(emptyDockSlotBounds)?.let { return it }

        overlaps
            .filter { (slot, _) -> slot >= 100 }
            .maxByOrNull { it.second }
            ?.first
            ?.let { return it }
    }

    if (sourcePosition != null && sourcePosition >= 100 && !droppingInDock) {
        overlaps
            .filter { (slot, _) -> slot < 100 }
            .maxByOrNull { it.second }
            ?.first
            ?.let { return it }
    }

    val targetSlots = if (droppingInDock) dockSlots else homeSlots
    overlaps
        .filter { (slot, _) -> targetSlots.containsKey(slot) }
        .maxByOrNull { it.second }
        ?.first
        ?.let { return it }

    if (droppingInDock) {
        nearestSlot(dockSlots)?.let { return it }
    }

    return nearestSlot(homeSlots)
}

private fun drawableToImageBitmap(drawable: android.graphics.drawable.Drawable): ImageBitmap? {
    return try {
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            drawable.bitmap.asImageBitmap()
        } else {
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val theme = LocalVoltTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bgVoid)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "UNFOLD LAUNCHER SETTINGS",
                color = theme.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            GlassPanel(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GRID SYSTEM CONFIG",
                        color = theme.accentPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current configuration is set dynamically. Long-press on the wallpaper to customize layout bindings.",
                        color = theme.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            GlassPanel(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GESTURE CONTROL CONFIG",
                        color = theme.accentSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Swipe down: Opens search/drawer\n• Swipe up: Opens search/drawer\n• Wallpaper long-press: Opens settings",
                        color = theme.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            androidx.compose.material3.Button(
                onClick = onBack,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = theme.accentPrimary)
            ) {
                Text("GO BACK", color = theme.bgVoid, fontWeight = FontWeight.Bold)
            }
        }
    }
}
