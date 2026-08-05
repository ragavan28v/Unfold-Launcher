@file:OptIn(ExperimentalMaterial3Api::class)

package com.volt.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt
import com.volt.core.domain.model.ActionType
import com.volt.core.domain.model.AppInfo
import com.volt.core.domain.model.AppDrawerLayoutMode
import com.volt.core.domain.model.AppDrawerSortingMode
import com.volt.core.domain.model.AppDrawerStyleMode
import com.volt.core.domain.model.DockBackgroundMode
import com.volt.core.domain.model.DockRowsMode
import com.volt.core.domain.model.GestureBinding
import com.volt.core.domain.model.GestureType
import com.volt.core.domain.model.HomeAppPlacementMode
import com.volt.core.ui.components.CarvedIcon
import com.volt.core.ui.components.GlassPanel
import com.volt.core.ui.components.PillBadge
import com.volt.core.ui.theme.LocalVoltTheme
import com.volt.core.ui.theme.VoltThemeColors
import androidx.compose.ui.platform.LocalContext
import com.volt.core.domain.model.AppDrawerSearchBarPosition
import com.volt.core.domain.model.AppDrawerViewMode

private data class SettingsSectionInfo(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val clickable: Boolean = false,
    val onClick: (() -> Unit)? = null
)

private enum class LauncherSettingsPage {
    HOME,
    DOCK,
    APP_DRAWER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherSettingsScreen(
    onBack: () -> Unit,
    onOpenGestureControl: () -> Unit,
    viewModel: LauncherSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalVoltTheme.current
    var selectedPage by rememberSaveable { mutableStateOf<LauncherSettingsPage?>(null) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val pageTitle = when (selectedPage) {
        LauncherSettingsPage.HOME -> "HOME"
        LauncherSettingsPage.DOCK -> "DOCK"
        LauncherSettingsPage.APP_DRAWER -> "APP DRAWER"
        null -> "LAUNCHER SETTINGS"
    }
    val pageSubtitle = when (selectedPage) {
        LauncherSettingsPage.HOME -> "Adjust the home screen grid and placement behavior."
        LauncherSettingsPage.DOCK -> "Adjust dock rows, icon count, icon sizing, and background behavior."
        LauncherSettingsPage.APP_DRAWER -> "Adjust drawer layout, grid, sorting, search position and keyboard behavior."
        null -> "Manage launcher preferences and controls."
    }
    val pageBackAction: () -> Unit = when (selectedPage) {
        null -> onBack
        else -> { { selectedPage = null } }
    }

    val sections = remember {
        listOf(
            SettingsSectionInfo(
                title = "Home",
                subtitle = "Grid rows 1-3, columns 3-6, icon size 30-100, labels on/off, app placement, wallpaper behavior, and pages.",
                badge = "Live",
                icon = Icons.Default.KeyboardArrowUp,
                clickable = true,
                onClick = { selectedPage = LauncherSettingsPage.HOME }
            ),
            SettingsSectionInfo(
                title = "Dock",
                subtitle = "Dock rows 1-2 or hidden, 0-6 icons, icon size 30-100, background style, gestures, and dock pages.",
                badge = "Live",
                icon = Icons.Default.KeyboardArrowDown,
                clickable = true,
                onClick = { selectedPage = LauncherSettingsPage.DOCK }
            ),
            SettingsSectionInfo(
                title = "App Drawer",
                subtitle = "Vertical list, horizontal pages, alphabetic grid, icon columns, icon size, sorting, drawer style, search, and categories.",
                badge = "Live",
                icon = Icons.Default.Menu,
                clickable = true,
                onClick = { selectedPage = LauncherSettingsPage.APP_DRAWER }
            ),
            SettingsSectionInfo(
                title = "Restore Defaults",
                subtitle = "Reset launcher theme, gestures, drawer preferences, and layout tuning back to the app defaults.",
                badge = "Reset",
                icon = Icons.Default.Settings,
                clickable = true,
                onClick = { showResetDialog = true }
            ),
            SettingsSectionInfo(
                title = "Icons",
                subtitle = "Icon pack, adaptive icons, shape, labels, badges, icon size, and shadow or glow styling.",
                badge = "Soon",
                icon = Icons.Default.Settings
            ),
            SettingsSectionInfo(
                title = "Gestures",
                subtitle = "Swipe up, swipe down, swipe left, swipe right, double tap, pinch, and two-finger actions.",
                badge = "Open",
                icon = Icons.Default.KeyboardArrowRight,
                clickable = true
            ),
            SettingsSectionInfo(
                title = "Search",
                subtitle = "Instant search, app suggestions, and search bar position. Drawer search bar position is wired already.",
                badge = "Soon",
                icon = Icons.Default.Search
            ),
            SettingsSectionInfo(
                title = "Folders",
                subtitle = "Folder style, shape, preview, behavior, and background treatment.",
                badge = "Soon",
                icon = Icons.Default.KeyboardArrowLeft
            ),
            SettingsSectionInfo(
                title = "Notifications and Badges",
                subtitle = "Badge style, per-app badges, notification dots, and unread count source.",
                badge = "Soon",
                icon = Icons.Default.Build
            ),
            SettingsSectionInfo(
                title = "Appearance",
                subtitle = "Theme, transparency, animation speed, font choices, and system bar styling.",
                badge = "Soon",
                icon = Icons.Default.Settings
            ),
            SettingsSectionInfo(
                title = "Desktop Extras",
                subtitle = "Widgets, hidden apps, app lock, backup and restore, and launcher import.",
                badge = "Soon",
                icon = Icons.Default.Build
            ),
            SettingsSectionInfo(
                title = "Advanced",
                subtitle = "Custom gestures, app shortcuts, backup format, drawer options, and icon normalization.",
                badge = "Soon",
                icon = Icons.Default.Build
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bgVoid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            SettingsPageHeader(
                title = pageTitle,
                subtitle = pageSubtitle,
                onBack = pageBackAction
            )

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedPage) {
                LauncherSettingsPage.HOME -> LauncherSettingsPageScaffold(
                    content = {
                        HomeSettingsPanel(
                            config = state.themeConfig,
                            onUpdate = { updated ->
                                viewModel.updateThemeConfig(updated)
                            }
                        )
                    }
                )
                LauncherSettingsPage.DOCK -> LauncherSettingsPageScaffold(
                    content = {
                        DockSettingsPanel(
                            config = state.themeConfig,
                            onUpdate = { updated ->
                                viewModel.updateThemeConfig(updated)
                            }
                        )
                    }
                )
                LauncherSettingsPage.APP_DRAWER -> LauncherSettingsPageScaffold(
                    content = {
                        AppDrawerSettingsPanel(
                            config = state.themeConfig,
                            drawerViewMode = state.drawerViewMode,
                            drawerSearchBarPosition = state.drawerSearchBarPosition,
                            drawerShowKeyboardOnOpen = state.drawerShowKeyboardOnOpen,
                            onUpdateTheme = { updated ->
                                viewModel.updateThemeConfig(updated)
                            },
                            onSetDrawerViewMode = viewModel::setDrawerViewMode,
                            onSetDrawerSearchBarPosition = viewModel::setDrawerSearchBarPosition,
                            onSetDrawerShowKeyboardOnOpen = viewModel::setDrawerShowKeyboardOnOpen
                        )
                    }
                )
                null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sections) { section ->
                            val cardOnClick: () -> Unit = section.onClick ?: if (section.clickable) {
                                onOpenGestureControl
                            } else {
                                {}
                            }
                            SettingsEntryCard(
                                title = section.title,
                                subtitle = section.subtitle,
                                badge = section.badge,
                                icon = section.icon,
                                onClick = cardOnClick
                            )
                        }
                    }
                }
            }

        }
        if (showResetDialog) {
            Dialog(onDismissRequest = { showResetDialog = false }) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.bgPanel.copy(alpha = 0.96f))
                        .border(1.dp, theme.panelBorder, RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Restore defaults?",
                        color = theme.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This will reset launcher theme, gestures, drawer preferences, and layout tuning back to the default setup.",
                        color = theme.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showResetDialog = false
                                viewModel.resetLauncherSettings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary)
                        ) {
                            Text("Restore", color = theme.bgVoid)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    val theme = LocalVoltTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = theme.textPrimary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = theme.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Text(
                text = subtitle,
                color = theme.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LauncherSettingsPageScaffold(
    content: @Composable () -> Unit
) {
    val theme = LocalVoltTheme.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun HomeSettingsPanel(
    config: com.volt.core.domain.model.ThemeConfig,
    onUpdate: (com.volt.core.domain.model.ThemeConfig) -> Unit
) {
    val theme = LocalVoltTheme.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "HOME CONTROLS",
                color = theme.accentSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChoiceRow(
                label = "Grid rows",
                options = listOf("1", "2", "3"),
                selected = config.homeGridRows.toString(),
                onSelected = { onUpdate(config.copy(homeGridRows = it.toInt())) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                label = "Columns",
                options = listOf("3", "4", "5", "6"),
                selected = config.homeGridColumns.toString(),
                onSelected = { onUpdate(config.copy(homeGridColumns = it.toInt())) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SliderSettingRow(
                label = "Icon size",
                value = config.homeIconSize,
                valueRange = 30f..100f,
                stepSize = 5f,
                onValueChange = { onUpdate(config.copy(homeIconSize = it.toInt())) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ToggleSettingRow(
                label = "Labels",
                checked = config.homeLabelsEnabled,
                onCheckedChange = { onUpdate(config.copy(homeLabelsEnabled = it)) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                label = "App placement",
                options = listOf("Auto-arrange", "Manual", "Lock"),
                selected = when (config.homeAppPlacementMode) {
                    HomeAppPlacementMode.AUTO_ARRANGE -> "Auto-arrange"
                    HomeAppPlacementMode.MANUAL_PLACEMENT -> "Manual"
                    HomeAppPlacementMode.LOCK_LAYOUT -> "Lock"
                },
                onSelected = {
                    onUpdate(
                        config.copy(
                            homeAppPlacementMode = when (it) {
                                "Manual" -> HomeAppPlacementMode.MANUAL_PLACEMENT
                                "Lock" -> HomeAppPlacementMode.LOCK_LAYOUT
                                else -> HomeAppPlacementMode.AUTO_ARRANGE
                            }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun DockSettingsPanel(
    config: com.volt.core.domain.model.ThemeConfig,
    onUpdate: (com.volt.core.domain.model.ThemeConfig) -> Unit
) {
    val theme = LocalVoltTheme.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "DOCK CONTROLS",
                color = theme.accentSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChoiceRow(
                label = "Dock size",
                options = listOf("1 row", "2 rows", "Hidden"),
                selected = when (config.dockRowsMode) {
                    DockRowsMode.ONE_ROW -> "1 row"
                    DockRowsMode.TWO_ROWS -> "2 rows"
                    DockRowsMode.HIDDEN -> "Hidden"
                },
                onSelected = {
                    onUpdate(
                        config.copy(
                            dockRowsMode = when (it) {
                                "2 rows" -> DockRowsMode.TWO_ROWS
                                "Hidden" -> DockRowsMode.HIDDEN
                                else -> DockRowsMode.ONE_ROW
                            }
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SliderSettingRow(
                label = "Icons count",
                value = config.dockIconCount,
                valueRange = 0f..6f,
                stepSize = 1f,
                onValueChange = { onUpdate(config.copy(dockIconCount = it.toInt())) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SliderSettingRow(
                label = "Icon size",
                value = config.dockIconSize,
                valueRange = 30f..100f,
                stepSize = 5f,
                onValueChange = { onUpdate(config.copy(dockIconSize = it.toInt())) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                label = "Background",
                options = listOf("Default", "Solid", "Transparent", "Blur"),
                selected = when (config.dockBackgroundMode) {
                    DockBackgroundMode.DEFAULT -> "Default"
                    DockBackgroundMode.SOLID -> "Solid"
                    DockBackgroundMode.TRANSPARENT -> "Transparent"
                    DockBackgroundMode.BLUR -> "Blur"
                },
                onSelected = {
                    onUpdate(
                        config.copy(
                            dockBackgroundMode = when (it) {
                                "Solid" -> DockBackgroundMode.SOLID
                                "Transparent" -> DockBackgroundMode.TRANSPARENT
                                "Blur" -> DockBackgroundMode.BLUR
                                else -> DockBackgroundMode.DEFAULT
                            }
                        )
                    )
                }
            )
            if (config.dockBackgroundMode == DockBackgroundMode.SOLID) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Solid color",
                    color = theme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    dockBackgroundPalette.forEach { hex ->
                        val swatchColor = remember(hex) { colorFromHex(hex) }
                        val selected = config.dockBackgroundHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) theme.accentPrimary else theme.panelBorder.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clip(RoundedCornerShape(999.dp))
                                .background(swatchColor)
                                .clickable {
                                    onUpdate(config.copy(dockBackgroundHex = hex))
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawerSettingsPanel(
    config: com.volt.core.domain.model.ThemeConfig,
    drawerViewMode: AppDrawerViewMode,
    drawerSearchBarPosition: AppDrawerSearchBarPosition,
    drawerShowKeyboardOnOpen: Boolean,
    onUpdateTheme: (com.volt.core.domain.model.ThemeConfig) -> Unit,
    onSetDrawerViewMode: (AppDrawerViewMode) -> Unit,
    onSetDrawerSearchBarPosition: (AppDrawerSearchBarPosition) -> Unit,
    onSetDrawerShowKeyboardOnOpen: (Boolean) -> Unit
) {
    val theme = LocalVoltTheme.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "APP DRAWER CONTROLS",
                color = theme.accentSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChoiceRow(
                label = "Layout",
                options = listOf("Grid", "List", "Listed grid"),
                selected = when (drawerViewMode) {
                    AppDrawerViewMode.GRID -> "Grid"
                    AppDrawerViewMode.LIST -> "List"
                    AppDrawerViewMode.LISTED_GRID -> "Listed grid"
                },
                onSelected = {
                    val mappedViewMode = when (it) {
                        "List" -> AppDrawerViewMode.LIST
                        "Listed grid" -> AppDrawerViewMode.LISTED_GRID
                        else -> AppDrawerViewMode.GRID
                    }
                    val mappedLayoutMode = when (it) {
                        "List" -> AppDrawerLayoutMode.VERTICAL_LIST
                        "Listed grid" -> AppDrawerLayoutMode.HORIZONTAL_PAGES
                        else -> AppDrawerLayoutMode.ALPHABETIC_GRID
                    }
                    onSetDrawerViewMode(mappedViewMode)
                    onUpdateTheme(config.copy(appDrawerLayoutMode = mappedLayoutMode))
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                label = "Columns",
                options = listOf("3", "4", "5", "6"),
                selected = config.appDrawerGridColumns.toString(),
                onSelected = { selectedValue ->
                    val updatedConfig = config.copy(appDrawerGridColumns = selectedValue.toInt())
                    onUpdateTheme(updatedConfig)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SliderSettingRow(
                label = "Icon size",
                value = config.appDrawerIconSize,
                valueRange = 30f..100f,
                stepSize = 5f,
                onValueChange = { newValue ->
                    val updatedConfig = config.copy(appDrawerIconSize = newValue.toInt())
                    onUpdateTheme(updatedConfig)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                label = "Sorting",
                options = listOf("Alphabetical", "Recent", "Custom"),
                selected = when (config.appDrawerSortingMode) {
                    AppDrawerSortingMode.RECENT -> "Recent"
                    AppDrawerSortingMode.CUSTOM -> "Custom"
                    AppDrawerSortingMode.ALPHABETICAL -> "Alphabetical"
                },
                onSelected = {
                    val selectedValue = it
                    val updatedConfig = config.copy(
                        appDrawerSortingMode = when (selectedValue) {
                            "Recent" -> AppDrawerSortingMode.RECENT
                            "Custom" -> AppDrawerSortingMode.CUSTOM
                            else -> AppDrawerSortingMode.ALPHABETICAL
                        }
                    )
                    onUpdateTheme(updatedConfig)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                label = "Drawer style",
                options = listOf("Open", "Closed", "Transparent", "Blur", "Card"),
                selected = when (config.appDrawerStyleMode) {
                    AppDrawerStyleMode.OPEN -> "Open"
                    AppDrawerStyleMode.CLOSED -> "Closed"
                    AppDrawerStyleMode.TRANSPARENT -> "Transparent"
                    AppDrawerStyleMode.BLUR -> "Blur"
                    AppDrawerStyleMode.CARD -> "Card"
                },
                onSelected = {
                    val selectedValue = it
                    val updatedConfig = config.copy(
                        appDrawerStyleMode = when (selectedValue) {
                            "Open" -> AppDrawerStyleMode.OPEN
                            "Closed" -> AppDrawerStyleMode.CLOSED
                            "Transparent" -> AppDrawerStyleMode.TRANSPARENT
                            "Blur" -> AppDrawerStyleMode.BLUR
                            else -> AppDrawerStyleMode.CARD
                        }
                    )
                    onUpdateTheme(updatedConfig)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                label = "Search bar position",
                options = listOf("Top", "Bottom", "Hidden"),
                selected = when (drawerSearchBarPosition) {
                    AppDrawerSearchBarPosition.TOP -> "Top"
                    AppDrawerSearchBarPosition.BOTTOM -> "Bottom"
                    AppDrawerSearchBarPosition.HIDDEN -> "Hidden"
                },
                onSelected = {
                    onSetDrawerSearchBarPosition(
                        when (it) {
                            "Bottom" -> AppDrawerSearchBarPosition.BOTTOM
                            "Hidden" -> AppDrawerSearchBarPosition.HIDDEN
                            else -> AppDrawerSearchBarPosition.TOP
                        }
                    )
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ToggleSettingRow(
                label = "Show keyboard on open",
                checked = drawerShowKeyboardOnOpen,
                onCheckedChange = onSetDrawerShowKeyboardOnOpen
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    val theme = LocalVoltTheme.current
    Column {
        Text(
            text = label,
            color = theme.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val active = option == selected
                TextButton(
                    onClick = { onSelected(option) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = option,
                        color = if (active) theme.accentPrimary else theme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderSettingRow(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    stepSize: Float = 5f,
    onValueChange: (Float) -> Unit
) {
    val theme = LocalVoltTheme.current
    val rangeStart = valueRange.start
    val rangeEnd = valueRange.endInclusive
    val snappedValue = snapToStep(value.toFloat(), rangeStart, rangeEnd, stepSize)
    val totalSteps = ((rangeEnd - rangeStart) / stepSize).roundToInt().coerceAtLeast(1)
    val sliderSteps = (totalSteps - 1).coerceAtLeast(0)
    val normalized = ((snappedValue - rangeStart) / (rangeEnd - rangeStart)).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.bgPanel.copy(alpha = 0.38f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = theme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value.toString(),
                color = theme.accentPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            )
            {
                val trackY = size.height / 2f
                val trackStart = 16.dp.toPx()
                val trackEnd = size.width - 16.dp.toPx()
                val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)
                val activeX = trackStart + trackWidth * normalized

                drawLine(
                    color = theme.bgVoid.copy(alpha = 0.66f),
                    start = Offset(trackStart, trackY),
                    end = Offset(trackEnd, trackY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = theme.accentPrimary.copy(alpha = 0.95f),
                    start = Offset(trackStart, trackY),
                    end = Offset(activeX, trackY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                repeat(totalSteps + 1) { index ->
                    val fraction = index / totalSteps.toFloat()
                    val x = trackStart + trackWidth * fraction
                    val isActive = fraction <= normalized + 0.0001f
                    drawCircle(
                        color = if (isActive) theme.accentPrimary else theme.panelBorder.copy(alpha = 0.65f),
                        radius = if (isActive) 4.2.dp.toPx() else 3.2.dp.toPx(),
                        center = Offset(x, trackY)
                    )
                }
            }
            Slider(
                value = snappedValue,
                onValueChange = { onValueChange(snapToStep(it, rangeStart, rangeEnd, stepSize)) },
                valueRange = valueRange,
                steps = sliderSteps,
                thumb = {},
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun snapToStep(value: Float, start: Float, end: Float, stepSize: Float): Float {
    if (stepSize <= 0f) return value.coerceIn(start, end)
    val snapped = ((value - start) / stepSize).roundToInt() * stepSize + start
    return snapped.coerceIn(start, end)
}

@Composable
private fun ToggleSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val theme = LocalVoltTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = theme.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private val dockBackgroundPalette = listOf(
    "#12161E",
    "#1F2937",
    "#0F172A",
    "#312E81",
    "#0F766E"
)

private fun colorFromHex(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { Color(0xFF12161E) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureControlSettingsScreen(
    onBack: () -> Unit,
    viewModel: LauncherSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalVoltTheme.current

    var editingGesture by remember { mutableStateOf<GestureType?>(null) }
    var draftActionType by remember { mutableStateOf(ActionType.LAUNCH_APP) }
    var draftTargetPackage by remember { mutableStateOf<String?>(null) }
    var draftShortcutId by remember { mutableStateOf("") }
    var appSearchQuery by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(editingGesture) {
        val binding = editingGesture
            ?.let { state.gestureBindings[it] }
            ?.takeIf { it.isUserModified }

        draftActionType = binding?.actionType?.takeIf { it == ActionType.LAUNCH_APP || it == ActionType.SHORTCUT }
            ?: ActionType.LAUNCH_APP
        draftTargetPackage = binding?.targetPackage
        draftShortcutId = binding?.targetShortcutId.orEmpty()
        appSearchQuery = ""
    }

    val installedApps = state.installedApps
    val filteredApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { it.label.contains(appSearchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bgVoid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GESTURE CONTROL",
                        color = theme.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = "Customize launcher swipe actions.",
                        color = theme.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Fixed gestures",
                        subtitle = "These launcher actions are already mapped and not editable here."
                    )
                }
                item {
                    GestureStaticRow(
                        title = "Swipe up",
                        subtitle = "Open app drawer",
                        value = "Drawer",
                        icon = Icons.Default.Menu,
                        theme = theme
                    )
                }
                item {
                    GestureStaticRow(
                        title = "Swipe down",
                        subtitle = "Open global search",
                        value = "Search",
                        icon = Icons.Default.Search,
                        theme = theme
                    )
                }
                item {
                    SectionHeader(
                        title = "Custom gestures",
                        subtitle = "Pick an app or shortcut for the side gestures."
                    )
                }
                item {
                    GestureEditableRow(
                        title = "Swipe left",
                        subtitle = "Assign an app or shortcut",
                        binding = state.gestureBindings[GestureType.SWIPE_LEFT_1F],
                        apps = state.installedApps,
                        icon = Icons.Default.KeyboardArrowLeft,
                        theme = theme,
                        onEdit = { editingGesture = GestureType.SWIPE_LEFT_1F }
                    )
                }
                item {
                    GestureEditableRow(
                        title = "Swipe right",
                        subtitle = "Assign an app or shortcut",
                        binding = state.gestureBindings[GestureType.SWIPE_RIGHT_1F],
                        apps = state.installedApps,
                        icon = Icons.Default.KeyboardArrowRight,
                        theme = theme,
                        onEdit = { editingGesture = GestureType.SWIPE_RIGHT_1F }
                    )
                }
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "NOTES",
                                color = theme.accentSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "More gesture types will be added later. For now, only the side swipe actions are configurable.",
                                color = theme.textSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        if (editingGesture != null) {
            ModalBottomSheet(
                onDismissRequest = { editingGesture = null },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                containerColor = theme.bgVoid.copy(alpha = 0.94f),
                scrimColor = Color.Black.copy(alpha = 0.58f),
                tonalElevation = 0.dp,
                dragHandle = null
            ) {
                GestureEditorSheet(
                    gestureType = editingGesture!!,
                    actionType = draftActionType,
                    targetPackage = draftTargetPackage,
                    shortcutId = draftShortcutId,
                    apps = filteredApps,
                    searchQuery = appSearchQuery,
                    onSearchQueryChange = { appSearchQuery = it },
                    onActionTypeChange = { draftActionType = it },
                    onTargetPackageChange = { draftTargetPackage = it },
                    onShortcutIdChange = { draftShortcutId = it },
                    onDismiss = { editingGesture = null },
                    onSave = {
                        viewModel.saveGestureBinding(
                            gestureType = editingGesture!!,
                            actionType = draftActionType,
                            targetPackage = draftTargetPackage,
                            targetShortcutId = draftShortcutId.trim().ifBlank { null }
                        )
                        editingGesture = null
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    val theme = LocalVoltTheme.current
    Column(modifier = Modifier.padding(horizontal = 2.dp)) {
        Text(
            text = title.uppercase(),
            color = theme.accentSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = theme.textSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SettingsEntryCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val theme = LocalVoltTheme.current
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarvedIcon(
                size = 42.dp,
                accentTint = theme.accentPrimary,
                contentDescription = title,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = theme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = theme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            PillBadge(
                text = badge,
                tint = theme.accentSecondary
            )
        }
    }
}

@Composable
private fun GestureStaticRow(
    title: String,
    subtitle: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: VoltThemeColors
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarvedIcon(
                size = 42.dp,
                accentTint = theme.accentPrimary,
                contentDescription = title,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = theme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = theme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
            }

            PillBadge(
                text = value,
                tint = theme.accentPrimary
            )
        }
    }
}

@Composable
private fun GestureEditableRow(
    title: String,
    subtitle: String,
    binding: GestureBinding?,
    apps: List<AppInfo>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: VoltThemeColors,
    onEdit: () -> Unit
) {
    val isConfigured = binding?.isUserModified == true &&
        (binding.actionType == ActionType.LAUNCH_APP || binding.actionType == ActionType.SHORTCUT)
    val appLabel = binding?.targetPackage?.let { packageName ->
        apps.firstOrNull { it.packageName == packageName }?.label
    }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarvedIcon(
                size = 42.dp,
                accentTint = if (isConfigured) theme.accentSecondary else theme.panelBorder,
                contentDescription = title,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isConfigured) theme.accentSecondary else theme.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = theme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                PillBadge(
                    text = when {
                        !isConfigured -> "Unassigned"
                        binding!!.actionType == ActionType.LAUNCH_APP -> appLabel ?: "App"
                        binding.actionType == ActionType.SHORTCUT -> "Shortcut"
                        else -> "Custom"
                    },
                    tint = if (isConfigured) theme.accentSecondary else theme.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Edit",
                    color = theme.accentPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureEditorSheet(
    gestureType: GestureType,
    actionType: ActionType,
    targetPackage: String?,
    shortcutId: String,
    apps: List<AppInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onActionTypeChange: (ActionType) -> Unit,
    onTargetPackageChange: (String?) -> Unit,
    onShortcutIdChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val theme = LocalVoltTheme.current
    val context = LocalContext.current
    val selectedAppLabel = targetPackage?.let { packageName ->
        apps.firstOrNull { it.packageName == packageName }?.label
    }
    val canSave = when (actionType) {
        ActionType.LAUNCH_APP -> targetPackage != null
        ActionType.SHORTCUT -> targetPackage != null
        else -> false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 10.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(theme.panelBorder.copy(alpha = 0.7f))
        )

        Text(
            text = when (gestureType) {
                GestureType.SWIPE_LEFT_1F -> "Configure swipe left"
                GestureType.SWIPE_RIGHT_1F -> "Configure swipe right"
                else -> "Configure gesture"
            },
            color = theme.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pick an app to launch when this gesture is used.",
            color = theme.textSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill(
                label = "App",
                selected = actionType == ActionType.LAUNCH_APP,
                onClick = { onActionTypeChange(ActionType.LAUNCH_APP) }
            )
            ActionPill(
                label = "Shortcut",
                selected = actionType == ActionType.SHORTCUT,
                onClick = { onActionTypeChange(ActionType.SHORTCUT) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (actionType == ActionType.SHORTCUT) {
            Text(
                text = "Shortcut id is optional for now. If you leave it blank, the selected app will open normally.",
                color = theme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = shortcutId,
                onValueChange = onShortcutIdChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                label = { Text("Shortcut ID") },
                placeholder = { Text("Optional for now") }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            label = { Text("Search apps") },
            placeholder = { Text("Type an app name") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedAppLabel != null) {
            PillBadge(
                text = "Selected: $selectedAppLabel",
                tint = theme.accentPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        LazyColumn(
            modifier = Modifier.height(280.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                val selected = app.packageName == targetPackage
                val appIcon = remember(app.packageName) {
                    runCatching {
                        drawableToImageBitmap(context.packageManager.getApplicationIcon(app.packageName))
                    }.getOrNull()
                }

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTargetPackageChange(app.packageName) },
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CarvedIcon(
                            size = 40.dp,
                            accentTint = if (selected) theme.accentPrimary else theme.panelBorder,
                            contentDescription = app.label,
                            icon = {
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = if (selected) theme.accentPrimary else theme.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                color = theme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (selected) {
                            PillBadge(
                                text = "Chosen",
                                tint = theme.accentPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary)
            ) {
                Text("Save", color = theme.bgVoid, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ActionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalVoltTheme.current
    val background = if (selected) theme.accentPrimary.copy(alpha = 0.18f) else theme.bgPanel.copy(alpha = 0.4f)
    val contentColor = if (selected) theme.accentPrimary else theme.textSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun drawableToImageBitmap(drawable: android.graphics.drawable.Drawable): androidx.compose.ui.graphics.ImageBitmap? {
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
    } catch (_: Exception) {
        null
    }
}
