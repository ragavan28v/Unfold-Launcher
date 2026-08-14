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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.unfold.core.domain.model.AppDrawerSearchBarPosition
import com.unfold.core.domain.model.AppDrawerSortingMode
import com.unfold.core.domain.model.AppDrawerStyleMode
import com.unfold.core.domain.model.AppDrawerViewMode
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.model.WallpaperMode
import com.unfold.core.domain.model.WallpaperPatternMode
import com.unfold.core.ui.components.CarvedIcon
import com.unfold.core.ui.components.PillBadge
import com.unfold.core.ui.theme.LocalUnfoldTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class AppAlphabetSection(
    val letter: Char,
    val apps: List<AppInfo>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    modifier: Modifier = Modifier,
    viewModel: AppDrawerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val sections = remember(state.filteredApps) { buildAlphabetSections(state.filteredApps) }
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

    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var showDrawerSettingsMenu by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
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
                                        onClick = {
                                            viewModel.onIntent(AppDrawerUiIntent.OpenApp(app.appId))
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
                                        onAppClick = { app ->
                                            viewModel.onIntent(AppDrawerUiIntent.OpenApp(app.appId))
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
                                        onAppClick = { app ->
                                            viewModel.onIntent(AppDrawerUiIntent.OpenApp(app.appId))
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
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                        }
                        context.startActivity(intent)
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        DrawerViewModeSelector(
            modifier = Modifier.height(54.dp),
            currentMode = viewMode,
            onModeSelected = onViewModeChange
        )

        Spacer(modifier = Modifier.width(8.dp))

        DrawerSettingsMenu(
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
private fun DrawerSearchField(
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    isSearchFocused: Boolean,
    onSearchFocusedChange: (Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search apps", color = theme.textSecondary) },
        modifier = modifier
            .height(54.dp)
            .focusRequester(searchFocusRequester)
            .onFocusChanged { onSearchFocusedChange(it.isFocused) },
        shape = RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = theme.bgPanel.copy(alpha = 0.76f),
            unfocusedContainerColor = theme.bgPanel.copy(alpha = 0.58f),
            focusedTextColor = theme.textPrimary,
            unfocusedTextColor = theme.textPrimary,
            cursorColor = theme.accentPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Composable
private fun DrawerSettingsMenu(
    showDrawerSettingsMenu: Boolean,
    onToggleSettingsMenu: () -> Unit,
    searchBarPosition: AppDrawerSearchBarPosition,
    showKeyboardOnOpen: Boolean,
    onToggleKeyboardOnOpen: (Boolean) -> Unit,
    onChangeSearchBarPosition: (AppDrawerSearchBarPosition) -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    Box {
        IconButton(
            onClick = onToggleSettingsMenu,
            modifier = Modifier.size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Drawer settings",
                tint = if (showDrawerSettingsMenu) theme.accentPrimary else theme.textSecondary
            )
        }

        DropdownMenu(
            expanded = showDrawerSettingsMenu,
            onDismissRequest = onToggleSettingsMenu,
            shape = RoundedCornerShape(18.dp),
            containerColor = theme.bgPanel.copy(alpha = 0.94f),
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.border(
                1.dp,
                theme.panelBorder.copy(alpha = 0.72f),
                RoundedCornerShape(18.dp)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Show keyboard on open",
                    color = theme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Focus search when the drawer opens",
                    color = theme.textSecondary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showKeyboardOnOpen) "Enabled" else "Disabled",
                        color = theme.textSecondary,
                        fontSize = 10.sp
                    )
                    Switch(
                        checked = showKeyboardOnOpen,
                        onCheckedChange = onToggleKeyboardOnOpen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = theme.panelBorder.copy(alpha = 0.45f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Search bar position",
                    color = theme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (selected) theme.textPrimary else theme.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        if (selected) {
            PillBadge(text = "Active", tint = theme.accentPrimary)
        }
    }
}

private enum class SectionMode {
    LIST,
    GRID
}

@Composable
private fun DrawerViewModeSelector(
    modifier: Modifier = Modifier,
    currentMode: AppDrawerViewMode,
    onModeSelected: (AppDrawerViewMode) -> Unit
) {
    val theme = LocalUnfoldTheme.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = theme.bgPanel.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.45f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewModePill(
                glyph = ViewModeGlyph.GRID,
                contentDescription = "Grid view",
                selected = currentMode == AppDrawerViewMode.GRID,
                modifier = Modifier.size(32.dp),
                onClick = { onModeSelected(AppDrawerViewMode.GRID) },
                theme = theme
            )
            ViewModePill(
                glyph = ViewModeGlyph.LIST,
                contentDescription = "List view",
                selected = currentMode == AppDrawerViewMode.LIST,
                modifier = Modifier.size(32.dp),
                onClick = { onModeSelected(AppDrawerViewMode.LIST) },
                theme = theme
            )
            ViewModePill(
                glyph = ViewModeGlyph.LISTED_GRID,
                contentDescription = "Listed grid view",
                selected = currentMode == AppDrawerViewMode.LISTED_GRID,
                modifier = Modifier.size(32.dp),
                onClick = { onModeSelected(AppDrawerViewMode.LISTED_GRID) },
                theme = theme
            )
        }
    }
}

@Composable
private fun ViewModePill(
    glyph: ViewModeGlyph,
    contentDescription: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(theme.accentPrimary.copy(alpha = 0.96f), CircleShape)
                    .border(BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.2f)), CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                ModeGlyphIcon(
                    glyph = glyph,
                    contentDescription = contentDescription,
                    tint = theme.bgVoid,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                ModeGlyphIcon(
                    glyph = glyph,
                    contentDescription = contentDescription,
                    tint = theme.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private enum class ViewModeGlyph {
    GRID,
    LIST,
    LISTED_GRID
}

@Composable
private fun ModeGlyphIcon(
    glyph: ViewModeGlyph,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Transparent)
    ) {
        when (glyph) {
            ViewModeGlyph.GRID -> GridGlyph(
                tint = tint,
                contentDescription = contentDescription
            )
            ViewModeGlyph.LIST -> ListGlyph(
                tint = tint,
                contentDescription = contentDescription
            )
            ViewModeGlyph.LISTED_GRID -> ListedGridGlyph(
                tint = tint,
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
private fun GridGlyph(
    tint: Color,
    contentDescription: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(tint, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun ListGlyph(
    tint: Color,
    contentDescription: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(tint, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun ListedGridGlyph(
    tint: Color,
    contentDescription: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(2.dp)
                        .background(tint, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(tint, RoundedCornerShape(999.dp))
                )
            }
        }
    }
}

@Composable
private fun AppSectionBlock(
    section: AppAlphabetSection,
    sectionMode: SectionMode,
    gridColumns: Int = 4,
    iconSize: Dp = 64.dp,
    drawerItemAlpha: Float = 0.72f,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(letter = section.letter, count = section.apps.size)

        when (sectionMode) {
            SectionMode.LIST -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.apps.forEach { app ->
                        AppListItem(
                            app = app,
                            iconSize = iconSize,
                            drawerItemAlpha = drawerItemAlpha,
                            onClick = { onAppClick(app) },
                            onLongPress = { onAppLongPress(app) }
                        )
                    }
                }
            }

            SectionMode.GRID -> {
                SectionGrid(
                    apps = section.apps,
                    gridColumns = gridColumns,
                    iconSize = iconSize,
                    drawerItemAlpha = drawerItemAlpha,
                    onAppClick = onAppClick,
                    onAppLongPress = onAppLongPress
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    letter: Char,
    count: Int
) {
    val theme = LocalUnfoldTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = letter.toString(),
            color = theme.accentPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = theme.panelBorder.copy(alpha = 0.65f),
            thickness = 0.5.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            color = theme.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionGrid(
    apps: List<AppInfo>,
    gridColumns: Int,
    iconSize: Dp,
    drawerItemAlpha: Float,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        apps.chunked(gridColumns).forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowApps.forEach { app ->
                    Box(modifier = Modifier.weight(1f)) {
                        AppGridItem(
                            app = app,
                            iconSize = iconSize,
                            drawerItemAlpha = drawerItemAlpha,
                            onClick = { onAppClick(app) },
                            onLongPress = { onAppLongPress(app) }
                        )
                    }
                }

                repeat(gridColumns - rowApps.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: AppInfo,
    iconSize: Dp,
    drawerItemAlpha: Float,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.appId) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                drawableToImageBitmap(drawable)
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(drawerItemAlpha)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(8.dp)
    ) {
        CarvedIcon(
            size = iconSize.coerceIn(32.dp, 72.dp),
            contentDescription = app.label,
            onClick = onClick,
            onLongPress = onLongPress,
            icon = {
                if (iconBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = iconBitmap!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        text = app.label.take(2).uppercase(),
                        color = theme.accentPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: AppInfo,
    iconSize: Dp,
    drawerItemAlpha: Float,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.appId) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                drawableToImageBitmap(drawable)
            } catch (e: Exception) {
                null
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(20.dp),
        color = theme.bgPanel.copy(alpha = drawerItemAlpha),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CarvedIcon(
                size = iconSize.coerceIn(36.dp, 72.dp),
                contentDescription = app.label,
                onClick = onClick,
                onLongPress = onLongPress,
                icon = {
                    if (iconBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = iconBitmap!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = app.label.take(2).uppercase(),
                            color = theme.accentPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    color = theme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompactAppActionSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onPinToHome: () -> Unit,
    onPinToDock: () -> Unit,
    onHideSystem: () -> Unit,
    onSystemInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    val theme = LocalUnfoldTheme.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .heightIn(min = 0.dp, max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            color = theme.bgPanel.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(38.dp, 4.dp)
                        .background(theme.panelBorder, CircleShape)
                )

                Text(
                    text = app.label.uppercase(),
                    color = theme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
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
                        text = "UNINSTALL SYSTEM",
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
            .fillMaxHeight()
            .width(28.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            val active = letter == selectedLetter
            Box(
                modifier = Modifier
                    .size(if (active) 28.dp else 20.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) theme.accentPrimary else Color.Transparent
                    )
                    .clickable { onLetterSelected(letter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    color = if (active) theme.bgVoid else theme.textSecondary,
                    fontSize = if (active) 12.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DrawerContextMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                                    val radius = 48f + index * 12f
                                    drawCircle(
                                        color = tint,
                                        radius = radius,
                                        center = Offset((index * 130f) % size.width, (index * 170f) % size.height)
                                    )
                                }
                            }

                            WallpaperPatternMode.ABSTRACT -> {
                                repeat(6) { index ->
                                    val y = 90f + index * 150f
                                    drawLine(
                                        color = tint,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y + 32f),
                                        strokeWidth = 12f
                                    )
                                }
                            }

                            WallpaperPatternMode.MINIMAL -> {
                                repeat(24) { index ->
                                    drawCircle(
                                        color = tint.copy(alpha = 0.03f),
                                        radius = 14f + (index % 4) * 3f,
                                        center = Offset(
                                            (index * 61f) % size.width,
                                            (index * 97f) % size.height
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

private fun buildAlphabetSections(apps: List<AppInfo>): List<AppAlphabetSection> {
    return apps
        .groupBy { app ->
            app.label.firstOrNull()
                ?.takeIf { it.isLetter() }
                ?.uppercaseChar()
                ?: '#'
        }
        .toSortedMap()
        .map { (letter, groupedApps) ->
            AppAlphabetSection(letter = letter, apps = groupedApps)
        }
}

private fun launchApp(context: Context, app: com.unfold.core.domain.model.AppInfo) {
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
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    } catch (_: Exception) {
        // Ignored
    }
}

private fun drawerIconSize(
    rawSize: Int,
    columns: Int,
    viewMode: AppDrawerViewMode
): Dp {
    val snappedSize = ((rawSize / 5f).roundToInt() * 5).coerceIn(30, 100)
    val maxForLayout = when (viewMode) {
        AppDrawerViewMode.LIST -> 58
        AppDrawerViewMode.GRID,
        AppDrawerViewMode.LISTED_GRID -> when (columns.coerceIn(3, 6)) {
            3 -> 58
            4 -> 52
            5 -> 46
            else -> 40
        }
    }
    val minForLayout = when (viewMode) {
        AppDrawerViewMode.LIST -> 42
        AppDrawerViewMode.GRID,
        AppDrawerViewMode.LISTED_GRID -> when (columns.coerceIn(3, 6)) {
            3 -> 36
            4 -> 34
            5 -> 32
            else -> 30
        }
    }
    return snappedSize.coerceIn(minForLayout, maxForLayout).dp
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


