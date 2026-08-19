package com.unfold.feature.drawer

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.produceState
import android.content.pm.LauncherApps
import android.os.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.unfold.core.ui.notification.NotificationBadgeStore
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.model.WallpaperMode
import com.unfold.core.domain.model.WallpaperPatternMode
import com.unfold.core.domain.model.AppDrawerViewMode
import com.unfold.core.domain.model.AppDrawerSearchBarPosition
import com.unfold.core.domain.model.AppDrawerStyleMode
import com.unfold.core.ui.components.CarvedIcon
import com.unfold.core.ui.theme.LocalUnfoldTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector

class AppAlphabetSection(
    val letter: Char,
    val apps: List<AppInfo>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    modifier: Modifier = Modifier,
    viewModel: AppDrawerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    NotificationBadgeStore.initialize(context)
    val notificationBadges by NotificationBadgeStore.counts.collectAsState()
    val badgeColor = remember(state.badgeColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(state.badgeColorHex)) }
            .getOrElse { Color(0xFFF44336) }
    }
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val sections = remember(state.filteredApps) { buildAlphabetSections(state.filteredApps) }

    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showDrawerSettingsMenu by remember { mutableStateOf(false) }

    val currentAlphabet = remember(
        state.viewMode,
        state.filteredApps,
        sections,
        gridState.firstVisibleItemIndex,
        gridState.firstVisibleItemScrollOffset,
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset
    ) {
        derivedStateOf {
            when (state.viewMode) {
                AppDrawerViewMode.GRID -> {
                    state.filteredApps.getOrNull(gridState.firstVisibleItemIndex)
                        ?.label
                        ?.firstOrNull()
                        ?.takeIf { it.isLetter() }
                        ?.uppercaseChar()
                        ?: '#'
                }

                AppDrawerViewMode.LIST,
                AppDrawerViewMode.LISTED_GRID -> {
                    sections.getOrNull(listState.firstVisibleItemIndex)?.letter ?: '#'
                }
            }
        }
    }
    val renderedIconSize = remember(state.iconSize, state.gridColumns, state.viewMode) {
        drawerIconSize(state.iconSize, state.gridColumns, state.viewMode)
    }
    val drawerBackdropColor = remember(state.styleMode) {
        when (state.styleMode) {
            AppDrawerStyleMode.OPEN -> theme.bgVoid
            AppDrawerStyleMode.CLOSED -> theme.bgPanel.copy(alpha = 0.88f)
            AppDrawerStyleMode.TRANSPARENT -> theme.bgVoid.copy(alpha = 0.82f)
            AppDrawerStyleMode.BLUR -> theme.bgPanel.copy(alpha = 0.56f)
            AppDrawerStyleMode.CARD -> theme.bgPanel.copy(alpha = 0.70f)
        }
    }
    val drawerItemAlpha = remember(state.styleMode) {
        when (state.styleMode) {
            AppDrawerStyleMode.OPEN -> 0.64f
            AppDrawerStyleMode.CLOSED -> 0.80f
            AppDrawerStyleMode.TRANSPARENT -> 0.36f
            AppDrawerStyleMode.BLUR -> 0.56f
            AppDrawerStyleMode.CARD -> 0.72f
        }
    }

    val closeThresholdPx = remember(density) { with(density) { 72.dp.toPx() } }

    LaunchedEffect(state.showKeyboardOnOpen) {
        if (state.showKeyboardOnOpen && state.searchBarPosition != AppDrawerSearchBarPosition.HIDDEN) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(
        modifier = modifier
                .fillMaxSize()
                .background(drawerBackdropColor)
    ) {
        val wallpaperMode = if (state.drawerWallpaperSyncWithHome) state.homeWallpaperMode else state.drawerWallpaperMode
        val wallpaperHex = if (state.drawerWallpaperSyncWithHome) state.homeWallpaperHex else state.drawerWallpaperHex
        val wallpaperPattern = if (state.drawerWallpaperSyncWithHome) state.homeWallpaperPattern else state.drawerWallpaperPattern
        val wallpaperUri = if (state.drawerWallpaperSyncWithHome) state.homeWallpaperImageUri else state.drawerWallpaperImageUri

        LauncherWallpaperBackdrop(
            modifier = Modifier.fillMaxSize(),
            mode = wallpaperMode,
            colorHex = wallpaperHex,
            pattern = wallpaperPattern,
            imageUri = wallpaperUri,
            fallbackColor = theme.bgVoid
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp, end = 2.dp, bottom = 16.dp)
        ) {
            if (state.searchBarPosition == AppDrawerSearchBarPosition.TOP) {
                DrawerSearchHeader(
                    searchQuery = state.searchQuery,
                    searchFocusRequester = searchFocusRequester,
                    isSearchFocused = isSearchFocused,
                    onSearchFocusedChange = { isSearchFocused = it },
                    onSearchChange = { viewModel.onIntent(AppDrawerUiIntent.Search(it)) },
                    viewMode = state.viewMode,
                    onViewModeChange = { viewModel.onIntent(AppDrawerUiIntent.SetViewMode(it)) },
                    showDrawerSettingsMenu = showDrawerSettingsMenu,
                    onToggleSettingsMenu = { showDrawerSettingsMenu = !showDrawerSettingsMenu },
                    searchBarPosition = state.searchBarPosition,
                    showKeyboardOnOpen = state.showKeyboardOnOpen,
                    onToggleKeyboardOnOpen = {
                        viewModel.onIntent(AppDrawerUiIntent.SetShowKeyboardOnOpen(it))
                        if (it && isSearchFocused) {
                            keyboardController?.show()
                        }
                    },
                    onChangeSearchBarPosition = { viewModel.onIntent(AppDrawerUiIntent.SetSearchBarPosition(it)) },
                    theme = theme
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else if (state.searchBarPosition == AppDrawerSearchBarPosition.HIDDEN) {
                DrawerHeaderControls(
                    viewMode = state.viewMode,
                    onViewModeChange = { viewModel.onIntent(AppDrawerUiIntent.SetViewMode(it)) },
                    showDrawerSettingsMenu = showDrawerSettingsMenu,
                    onToggleSettingsMenu = { showDrawerSettingsMenu = !showDrawerSettingsMenu },
                    searchBarPosition = state.searchBarPosition,
                    showKeyboardOnOpen = state.showKeyboardOnOpen,
                    onToggleKeyboardOnOpen = {
                        viewModel.onIntent(AppDrawerUiIntent.SetShowKeyboardOnOpen(it))
                    },
                    onChangeSearchBarPosition = { viewModel.onIntent(AppDrawerUiIntent.SetSearchBarPosition(it)) },
                    theme = theme
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(
                            state.viewMode,
                            gridState.firstVisibleItemIndex,
                            gridState.firstVisibleItemScrollOffset,
                            listState.firstVisibleItemIndex,
                            listState.firstVisibleItemScrollOffset
                        ) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                val atTop = when (state.viewMode) {
                                    AppDrawerViewMode.GRID ->
                                        gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                                    AppDrawerViewMode.LIST,
                                    AppDrawerViewMode.LISTED_GRID ->
                                        listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                                }

                                if (!atTop) {
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                    }
                                    return@awaitEachGesture
                                }

                                var totalDownDrag = 0f
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break

                                    val dy = change.positionChange().y
                                    if (dy > 0f) {
                                        totalDownDrag += dy
                                        change.consume()
                                        if (totalDownDrag >= closeThresholdPx) {
                                            onBack()
                                            break
                                        }
                                    } else if (dy < 0f) {
                                        totalDownDrag = 0f
                                    }
                                }
                            }
                        }
                ) {
                    when (state.viewMode) {
                        AppDrawerViewMode.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(state.gridColumns),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 8.dp)
                            ) {
                                items(
                                    items = state.filteredApps,
                                    key = { it.appId }
                                ) { app ->
                                    AppGridItem(
                                        app = app,
                                        iconSize = renderedIconSize,
                                        drawerItemAlpha = drawerItemAlpha,
                                        iconPackPackage = state.iconPackPackage,
                                        badgeCount = notificationBadges[
                                            NotificationBadgeStore.instanceKey(app.packageName, app.userSerial)
                                        ],
                                        badgeColor = badgeColor,
                                        showBadgeCount = state.showBadgeCount,
                                        onClick = {
                                            viewModel.onIntent(AppDrawerUiIntent.OpenApp(app.appId))
                                            NotificationBadgeStore.clearInstance(
                                                NotificationBadgeStore.instanceKey(app.packageName, app.userSerial)
                                            )
                                            launchApp(context, app)
                                        },
                                        onLongPress = {
                                            selectedAppForMenu = app
                                        }
                                    )
                                }
                            }
                        }

                        AppDrawerViewMode.LIST -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = sections,
                                    key = { it.letter }
                                ) { section ->
                                    AppSectionBlock(
                                        section = section,
                                        sectionMode = SectionMode.LIST,
                                        gridColumns = state.gridColumns,
                                        iconSize = renderedIconSize,
                                        drawerItemAlpha = drawerItemAlpha,
                                        iconPackPackage = state.iconPackPackage,
                                        badgeCounts = notificationBadges,
                                        badgeColor = badgeColor,
                                        showBadgeCount = state.showBadgeCount,
                                        onAppClick = { app ->
                                            viewModel.onIntent(AppDrawerUiIntent.OpenApp(app.appId))
                                            NotificationBadgeStore.clearInstance(
                                                NotificationBadgeStore.instanceKey(app.packageName, app.userSerial)
                                            )
                                            launchApp(context, app)
                                        },
                                        onAppLongPress = { app -> selectedAppForMenu = app }
                                    )
                                }
                            }
                        }

                        AppDrawerViewMode.LISTED_GRID -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = sections,
                                    key = { it.letter }
                                ) { section ->
                                    AppSectionBlock(
                                        section = section,
                                        sectionMode = SectionMode.GRID,
                                        gridColumns = state.gridColumns,
                                        iconSize = renderedIconSize,
                                        drawerItemAlpha = drawerItemAlpha,
                                        iconPackPackage = state.iconPackPackage,
                                        onAppClick = { app ->
                                            viewModel.onIntent(AppDrawerUiIntent.OpenApp(app.appId))
                                            NotificationBadgeStore.clearInstance(
                                                NotificationBadgeStore.instanceKey(app.packageName, app.userSerial)
                                            )
                                            launchApp(context, app)
                                        },
                                        onAppLongPress = { app -> selectedAppForMenu = app }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                AlphabetFastScroll(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 4.dp, bottom = 4.dp, end = 0.dp),
                    letters = listOf('#') + ('A'..'Z').toList(),
                    selectedLetter = currentAlphabet.value,
                    onLetterSelected = { letter ->
                        when (state.viewMode) {
                            AppDrawerViewMode.GRID -> {
                                val index = if (letter == '#') {
                                    state.filteredApps.indexOfFirst {
                                        it.label.firstOrNull()?.isLetter() != true
                                    }
                                } else {
                                    state.filteredApps.indexOfFirst {
                                        it.label.startsWith(letter, ignoreCase = true)
                                    }
                                }
                                if (index != -1) {
                                    coroutineScope.launch {
                                        gridState.animateScrollToItem(index)
                                    }
                                }
                            }
                            AppDrawerViewMode.LIST,
                            AppDrawerViewMode.LISTED_GRID -> {
                                val index = sections.indexOfFirst { it.letter == letter }
                                if (index != -1) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            }
                        }
                    }
                )
            }

            if (state.searchBarPosition == AppDrawerSearchBarPosition.BOTTOM) {
                Spacer(modifier = Modifier.height(10.dp))
                DrawerSearchHeader(
                    searchQuery = state.searchQuery,
                    searchFocusRequester = searchFocusRequester,
                    isSearchFocused = isSearchFocused,
                    onSearchFocusedChange = { isSearchFocused = it },
                    onSearchChange = { viewModel.onIntent(AppDrawerUiIntent.Search(it)) },
                    viewMode = state.viewMode,
                    onViewModeChange = { viewModel.onIntent(AppDrawerUiIntent.SetViewMode(it)) },
                    showDrawerSettingsMenu = showDrawerSettingsMenu,
                    onToggleSettingsMenu = { showDrawerSettingsMenu = !showDrawerSettingsMenu },
                    searchBarPosition = state.searchBarPosition,
                    showKeyboardOnOpen = state.showKeyboardOnOpen,
                    onToggleKeyboardOnOpen = {
                        viewModel.onIntent(AppDrawerUiIntent.SetShowKeyboardOnOpen(it))
                        if (it && isSearchFocused) {
                            keyboardController?.show()
                        }
                    },
                    onChangeSearchBarPosition = { viewModel.onIntent(AppDrawerUiIntent.SetSearchBarPosition(it)) },
                    theme = theme
                )
            }
        }

        AnimatedVisibility(
            visible = selectedAppForMenu != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        selectedAppForMenu = null
                    }
            )
        }

        AnimatedVisibility(
            visible = selectedAppForMenu != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }),
            exit = slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val app = selectedAppForMenu
            if (app != null) {
                CompactAppActionSheet(
                    app = app,
                    onDismiss = { selectedAppForMenu = null },
                    onPinToHome = {
                        viewModel.onIntent(AppDrawerUiIntent.PinToHome(app.appId))
                        selectedAppForMenu = null
                    },
                    onPinToDock = {
                        viewModel.onIntent(AppDrawerUiIntent.PinToDock(app.appId))
                        selectedAppForMenu = null
                    },
                    onHideSystem = {
                        viewModel.onIntent(AppDrawerUiIntent.HideApp(app.appId))
                        selectedAppForMenu = null
                    },
                    onSystemInfo = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                        }
                        context.startActivity(intent)
                        selectedAppForMenu = null
                    },
                    onUninstall = {
                        val pkg = app.packageName
                        try {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.fromParts("package", pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("AppDrawer", "Failed to uninstall app: $pkg", e)
                            try {
                                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                    data = Uri.fromParts("package", pkg, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (ex: Exception) {
                                android.util.Log.e("AppDrawer", "Final uninstall fallback failed", ex)
                            }
                        }
                        selectedAppForMenu = null
                    }
                )
            }
        }
    }
}

@Composable
private fun DrawerSearchHeader(
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    isSearchFocused: Boolean,
    onSearchFocusedChange: (Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    viewMode: AppDrawerViewMode,
    onViewModeChange: (AppDrawerViewMode) -> Unit,
    showDrawerSettingsMenu: Boolean,
    onToggleSettingsMenu: () -> Unit,
    searchBarPosition: AppDrawerSearchBarPosition,
    showKeyboardOnOpen: Boolean,
    onToggleKeyboardOnOpen: (Boolean) -> Unit,
    onChangeSearchBarPosition: (AppDrawerSearchBarPosition) -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawerSearchField(
            searchQuery = searchQuery,
            searchFocusRequester = searchFocusRequester,
            isSearchFocused = isSearchFocused,
            onSearchFocusedChange = onSearchFocusedChange,
            onSearchChange = onSearchChange,
            modifier = Modifier.weight(1f),
            theme = theme
        )

        Spacer(modifier = Modifier.width(10.dp))

        DrawerHeaderControls(
            viewMode = viewMode,
            onViewModeChange = onViewModeChange,
            showDrawerSettingsMenu = showDrawerSettingsMenu,
            onToggleSettingsMenu = onToggleSettingsMenu,
            searchBarPosition = searchBarPosition,
            showKeyboardOnOpen = showKeyboardOnOpen,
            onToggleKeyboardOnOpen = onToggleKeyboardOnOpen,
            onChangeSearchBarPosition = onChangeSearchBarPosition,
            theme = theme
        )
    }
}

@Composable
private fun DrawerHeaderControls(
    viewMode: AppDrawerViewMode,
    onViewModeChange: (AppDrawerViewMode) -> Unit,
    showDrawerSettingsMenu: Boolean,
    onToggleSettingsMenu: () -> Unit,
    searchBarPosition: AppDrawerSearchBarPosition,
    showKeyboardOnOpen: Boolean,
    onToggleKeyboardOnOpen: (Boolean) -> Unit,
    onChangeSearchBarPosition: (AppDrawerSearchBarPosition) -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DrawerViewModeSelector(
            viewMode = viewMode,
            onViewModeChange = onViewModeChange
        )

        Box {
            IconButton(
                onClick = onToggleSettingsMenu,
                modifier = Modifier
                    .size(44.dp)
                    .background(theme.bgPanel.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Drawer Settings",
                    tint = theme.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DrawerSettingsMenu(
                expanded = showDrawerSettingsMenu,
                onDismiss = onToggleSettingsMenu,
                searchBarPosition = searchBarPosition,
                showKeyboardOnOpen = showKeyboardOnOpen,
                onToggleKeyboardOnOpen = onToggleKeyboardOnOpen,
                onChangeSearchBarPosition = onChangeSearchBarPosition,
                theme = theme
            )
        }
    }
}

@Composable
private fun DrawerSearchField(
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    isSearchFocused: Boolean,
    onSearchFocusedChange: (Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    androidx.compose.foundation.text.BasicTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = modifier
            .height(44.dp)
            .focusRequester(searchFocusRequester)
            .onFocusChanged { onSearchFocusedChange(it.isFocused) }
            .background(theme.bgPanel.copy(alpha = if (isSearchFocused) 0.6f else 0.4f), CircleShape)
            .border(
                1.dp,
                if (isSearchFocused) theme.accentPrimary.copy(alpha = 0.5f) else theme.panelBorder.copy(alpha = 0.2f),
                CircleShape
            ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = theme.textPrimary, fontSize = 14.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.accentPrimary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = theme.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search system apps...",
                            color = theme.textSecondary.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = theme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DrawerSettingsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    searchBarPosition: AppDrawerSearchBarPosition,
    showKeyboardOnOpen: Boolean,
    onToggleKeyboardOnOpen: (Boolean) -> Unit,
    onChangeSearchBarPosition: (AppDrawerSearchBarPosition) -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(220.dp)
            .background(theme.bgPanel.copy(alpha = 0.95f))
            .border(1.dp, theme.panelBorder.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "DRAWER SETTINGS",
                color = theme.accentPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto-Keyboard", color = theme.textPrimary, fontSize = 13.sp)
                Switch(
                    checked = showKeyboardOnOpen,
                    onCheckedChange = onToggleKeyboardOnOpen,
                    modifier = Modifier.size(32.dp, 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("SEARCH BAR POSITION", color = theme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DrawerPositionItem(
                    label = "Top",
                    selected = searchBarPosition == AppDrawerSearchBarPosition.TOP,
                    onClick = { onChangeSearchBarPosition(AppDrawerSearchBarPosition.TOP) }
                )
                DrawerPositionItem(
                    label = "Bottom",
                    selected = searchBarPosition == AppDrawerSearchBarPosition.BOTTOM,
                    onClick = { onChangeSearchBarPosition(AppDrawerSearchBarPosition.BOTTOM) }
                )
                DrawerPositionItem(
                    label = "Hidden",
                    selected = searchBarPosition == AppDrawerSearchBarPosition.HIDDEN,
                    onClick = { onChangeSearchBarPosition(AppDrawerSearchBarPosition.HIDDEN) }
                )
            }
        }
    }
}

@Composable
private fun DrawerPositionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    Surface(
        onClick = onClick,
        color = if (selected) theme.accentPrimary.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (selected) theme.accentPrimary else theme.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

enum class SectionMode {
    LIST, GRID
}

@Composable
fun DrawerViewModeSelector(
    modifier: Modifier = Modifier,
    viewMode: AppDrawerViewMode,
    onViewModeChange: (AppDrawerViewMode) -> Unit
) {
    val theme = LocalUnfoldTheme.current
    Row(
        modifier = modifier
            .height(44.dp)
            .background(theme.bgPanel.copy(alpha = 0.5f), CircleShape)
            .border(1.dp, theme.panelBorder.copy(alpha = 0.2f), CircleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ViewModePill(
            glyph = ViewModeGlyph.GRID,
            label = "Grid",
            selected = viewMode == AppDrawerViewMode.GRID,
            onClick = { onViewModeChange(AppDrawerViewMode.GRID) },
            theme = theme
        )
        ViewModePill(
            glyph = ViewModeGlyph.LIST,
            label = "List",
            selected = viewMode == AppDrawerViewMode.LIST,
            onClick = { onViewModeChange(AppDrawerViewMode.LIST) },
            theme = theme
        )
        ViewModePill(
            glyph = ViewModeGlyph.LISTED_GRID,
            label = "Hybrid",
            selected = viewMode == AppDrawerViewMode.LISTED_GRID,
            onClick = { onViewModeChange(AppDrawerViewMode.LISTED_GRID) },
            theme = theme
        )
    }
}

@Composable
fun ViewModePill(
    glyph: ViewModeGlyph,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (selected) theme.accentPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        ModeGlyphIcon(
            glyph = glyph,
            label = label,
            tint = if (selected) theme.bgVoid else theme.textSecondary
        )
    }
}

enum class ViewModeGlyph {
    GRID, LIST, LISTED_GRID
}

@Composable
fun ModeGlyphIcon(
    glyph: ViewModeGlyph,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (glyph) {
        ViewModeGlyph.GRID -> GridGlyph(tint, label)
        ViewModeGlyph.LIST -> ListGlyph(tint, label)
        ViewModeGlyph.LISTED_GRID -> ListedGridGridGlyph(tint, label)
    }
}

@Composable
fun GridGlyph(color: Color, label: String) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val s = size.width / 3f
        val gap = 1.5.dp.toPx()
        val cellSize = s - gap
        for (i in 0..2) {
            for (j in 0..2) {
                drawRect(
                    color = color,
                    topLeft = Offset(i * s, j * s),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }
        }
    }
}

@Composable
fun ListGlyph(color: Color, label: String) {
    Column(modifier = Modifier.width(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(modifier = Modifier.size(3.dp).background(color, CircleShape))
                Box(modifier = Modifier.height(1.5.dp).weight(1f).background(color))
            }
        }
    }
}

@Composable
fun ListedGridGridGlyph(color: Color, label: String) {
    Column(modifier = Modifier.width(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.size(6.dp).background(color))
            Box(modifier = Modifier.size(6.dp).background(color))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.size(3.dp).background(color, CircleShape))
            Box(modifier = Modifier.height(1.5.dp).weight(1f).background(color))
        }
    }
}

@Composable
fun AppSectionBlock(
    section: AppAlphabetSection,
    sectionMode: SectionMode,
    gridColumns: Int,
    iconSize: Dp,
    drawerItemAlpha: Float,
    iconPackPackage: String = "",
    badgeCounts: Map<String, Int> = emptyMap(),
    badgeColor: Color = Color(0xFFF44336),
    showBadgeCount: Boolean = false,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(section.letter, section.apps.size)
        Spacer(modifier = Modifier.height(8.dp))
        when (sectionMode) {
            SectionMode.LIST -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.apps.forEach { app ->
                        AppListItem(
                            app = app,
                            iconSize = iconSize,
                            drawerItemAlpha = drawerItemAlpha,
                            iconPackPackage = iconPackPackage,
                            badgeCount = badgeCounts[
                                NotificationBadgeStore.instanceKey(app.packageName, app.userSerial)
                            ],
                            badgeColor = badgeColor,
                            showBadgeCount = showBadgeCount,
                            onClick = { onAppClick(app) },
                            onLongPress = { onAppLongPress(app) }
                        )
                    }
                }
            }
            SectionMode.GRID -> {
                SectionGrid(
                    apps = section.apps,
                    columns = gridColumns,
                    iconSize = iconSize,
                    drawerItemAlpha = drawerItemAlpha,
                    iconPackPackage = iconPackPackage,
                    badgeCounts = badgeCounts,
                    badgeColor = badgeColor,
                    showBadgeCount = showBadgeCount,
                    onAppClick = onAppClick,
                    onAppLongPress = onAppLongPress
                )
            }
        }
    }
}

@Composable
fun SectionHeader(letter: Char, count: Int) {
    val theme = LocalUnfoldTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        @OptIn(ExperimentalFoundationApi::class)
        Text(
            text = letter.toString(),
            color = theme.accentPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = theme.panelBorder.copy(alpha = 0.2f),
            thickness = 1.dp
        )
        Text(
            text = count.toString(),
            color = theme.textSecondary.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionGrid(
    apps: List<AppInfo>,
    columns: Int,
    iconSize: Dp,
    drawerItemAlpha: Float,
    iconPackPackage: String = "",
    badgeCounts: Map<String, Int> = emptyMap(),
    badgeColor: Color = Color(0xFFF44336),
    showBadgeCount: Boolean = false,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit
) {
    val rows = (apps.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(columns) { colIndex ->
                    val appIndex = rowIndex * columns + colIndex
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (appIndex < apps.size) {
                            val app = apps[appIndex]
                            AppGridItem(
                                app = app,
                                iconSize = iconSize,
                                drawerItemAlpha = drawerItemAlpha,
                                iconPackPackage = iconPackPackage,
                                badgeCount = badgeCounts[
                                    NotificationBadgeStore.instanceKey(app.packageName, app.userSerial)
                                ],
                                badgeColor = badgeColor,
                                showBadgeCount = showBadgeCount,
                                onClick = { onAppClick(app) },
                                onLongPress = { onAppLongPress(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    iconSize: Dp,
    drawerItemAlpha: Float,
    iconPackPackage: String = "",
    badgeCount: Int? = null,
    badgeColor: Color = Color(0xFFF44336),
    showBadgeCount: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.appId, iconPackPackage) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = com.unfold.core.ui.iconpack.IconPackResolver.resolveAppIconDrawable(
                    context,
                    app.packageName,
                    iconPackPackage.takeIf { it.isNotBlank() }
                )
                if (drawable != null) {
                    drawableToImageBitmap(drawable)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .width(iconSize + 24.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CarvedIcon(
            size = iconSize,
            raw = iconPackPackage.isNotBlank() &&
                !com.unfold.core.ui.iconpack.IconPackResolver.isLauncherRingEnabled(context),
            icon = {
                val bitmap = iconBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = app.label.take(2).uppercase(),
                        color = theme.accentPrimary,
                        fontSize = (iconSize.value * 0.25f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            contentDescription = app.label,
            badgeCount = badgeCount,
            badgeColor = badgeColor,
            showBadgeCount = showBadgeCount,
            onClick = onClick,
            onLongPress = onLongPress
        )
        Text(
            text = app.label,
            color = theme.textPrimary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alpha(drawerItemAlpha)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppInfo,
    iconSize: Dp,
    drawerItemAlpha: Float,
    iconPackPackage: String = "",
    badgeCount: Int? = null,
    badgeColor: Color = Color(0xFFF44336),
    showBadgeCount: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.appId, iconPackPackage) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = com.unfold.core.ui.iconpack.IconPackResolver.resolveAppIconDrawable(
                    context,
                    app.packageName,
                    iconPackPackage.takeIf { it.isNotBlank() }
                )
                if (drawable != null) {
                    drawableToImageBitmap(drawable)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CarvedIcon(
            size = iconSize.coerceAtMost(48.dp),
            raw = iconPackPackage.isNotBlank() &&
                !com.unfold.core.ui.iconpack.IconPackResolver.isLauncherRingEnabled(context),
            icon = {
                val bitmap = iconBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
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
            badgeCount = badgeCount,
            badgeColor = badgeColor,
            showBadgeCount = showBadgeCount,
            onClick = onClick,
            onLongPress = onLongPress
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = theme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alpha(drawerItemAlpha)
            )
            if (app.customLabel != null) {
                Text(
                    text = "System tag: ${app.label}",
                    color = theme.textSecondary.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = theme.textSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun CompactAppActionSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onPinToHome: () -> Unit,
    onPinToDock: () -> Unit,
    onHideSystem: () -> Unit,
    onSystemInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    com.unfold.core.ui.components.GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        cornerRadius = 32.dp,
        opacity = 0.94f, // Much higher opacity for solid frozen look
        blurRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(theme.textSecondary.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = app.label.uppercase(),
                color = theme.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DrawerContextMenuItem(
                    text = "PIN TO HOME",
                    icon = Icons.Default.Home,
                    iconColor = theme.accentPrimary,
                    onClick = onPinToHome
                )
                HorizontalDivider(color = theme.panelBorder, thickness = 0.5.dp)
                DrawerContextMenuItem(
                    text = "PIN TO DOCK",
                    icon = Icons.Default.Build,
                    iconColor = theme.accentPrimary,
                    onClick = onPinToDock
                )
                HorizontalDivider(color = theme.panelBorder, thickness = 0.5.dp)
                DrawerContextMenuItem(
                    text = "HIDE SYSTEM",
                    icon = Icons.Default.Close,
                    iconColor = theme.accentDanger,
                    onClick = onHideSystem
                )
                HorizontalDivider(color = theme.panelBorder, thickness = 0.5.dp)
                DrawerContextMenuItem(
                    text = "SYSTEM INFO",
                    icon = Icons.Default.Info,
                    iconColor = theme.accentPrimary,
                    onClick = onSystemInfo
                )
                HorizontalDivider(color = theme.panelBorder, thickness = 0.5.dp)
                DrawerContextMenuItem(
                    text = "UNINSTALL APP",
                    icon = Icons.Default.Delete,
                    iconColor = theme.accentDanger,
                    onClick = onUninstall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "TAP OUTSIDE TO DISMISS",
                color = theme.textSecondary,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun AlphabetFastScroll(
    modifier: Modifier = Modifier,
    letters: List<Char>,
    selectedLetter: Char,
    onLetterSelected: (Char) -> Unit
) {
    val theme = LocalUnfoldTheme.current
    Column(
        modifier = modifier
            .width(24.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        letters.forEach { char ->
            val isSelected = char == selectedLetter
            Text(
                text = char.toString(),
                color = if (isSelected) theme.accentPrimary else theme.textSecondary.copy(alpha = 0.4f),
                fontSize = if (isSelected) 11.sp else 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { onLetterSelected(char) }
                    )
                    .padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
fun DrawerContextMenuItem(
    text: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CarvedIcon(
            size = 38.dp,
            accentTint = theme.panelBorder,
            contentDescription = text,
            onClick = onClick,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        Text(
            text = text.uppercase(),
            color = theme.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
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
            WallpaperMode.PRESET,
            WallpaperMode.CUSTOM -> {
                if (imageUri.isNotBlank()) {
                    Image(
                        painter = coil.compose.rememberAsyncImagePainter(imageUri),
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

fun buildAlphabetSections(apps: List<AppInfo>): List<AppAlphabetSection> {
    return apps.groupBy { it.label.firstOrNull()?.uppercaseChar() ?: '#' }
        .map { (letter, sectionApps) -> AppAlphabetSection(letter, sectionApps.sortedBy { it.label.lowercase() }) }
        .sortedBy { if (it.letter == '#') '{' else it.letter }
}

private fun launchApp(context: Context, app: AppInfo) {
    try {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val userManager = context.getSystemService(UserManager::class.java)
        val userHandle = userManager?.getUserForSerialNumber(app.userSerial)
        if (launcherApps != null && userHandle != null && app.activityName.isNotBlank()) {
            launcherApps.startMainActivity(
                ComponentName(app.packageName, app.activityName),
                userHandle,
                null,
                null
            )
            return
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchIntent?.let { context.startActivity(it) }
    } catch (_: Exception) {
    }
}

private fun drawerIconSize(rawSize: Int, columns: Int, viewMode: AppDrawerViewMode): Dp {
    val base = rawSize.coerceIn(36, 96).dp
    return when (viewMode) {
        AppDrawerViewMode.GRID -> base
        AppDrawerViewMode.LIST -> 42.dp
        AppDrawerViewMode.LISTED_GRID -> base.coerceAtMost(48.dp)
    }
}

private fun drawableToImageBitmap(drawable: android.graphics.drawable.Drawable): ImageBitmap? {
    return try {
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
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
            }
            circularCanvas.drawARGB(0, 0, 0, 0)
            circularCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.setXfermode(android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN))
            
            val srcRect = android.graphics.Rect(
                (width - size) / 2,
                (height - size) / 2,
                (width + size) / 2,
                (height + size) / 2
            )
            val destRect = android.graphics.Rect(0, 0, size, size)
            circularCanvas.drawBitmap(bitmap, srcRect, destRect, paint)
            
            paint.setXfermode(null)
            fg.setBounds(0, 0, size, size)
            fg.draw(circularCanvas)
            
            circularBitmap.asImageBitmap()
        } else {
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            
            val size = Math.min(width, height)
            val circularBitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val circularCanvas = android.graphics.Canvas(circularBitmap)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
            }
            circularCanvas.drawARGB(0, 0, 0, 0)
            circularCanvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.setXfermode(android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN))
            
            val srcRect = android.graphics.Rect(
                (width - size) / 2,
                (height - size) / 2,
                (width + size) / 2,
                (height + size) / 2
            )
            val destRect = android.graphics.Rect(0, 0, size, size)
            circularCanvas.drawBitmap(bitmap, srcRect, destRect, paint)
            
            circularBitmap.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}
