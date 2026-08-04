package com.volt.feature.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volt.core.domain.model.AppInfo
import com.volt.core.ui.components.*
import com.volt.core.ui.theme.LocalVoltTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onNavigateToDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalVoltTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val railNodes = listOf(
        RailNode("home", Icons.Default.Home, "Home"),
        RailNode("system", Icons.Default.Build, "System"),
        RailNode("media", Icons.Default.PlayArrow, "Media")
    )

    // Separate grid apps (positions < 100) and dock apps (positions >= 100)
    val gridApps = state.gridApps.filter { (it.gridPosition ?: 0) < 100 }
    val dockApps = state.gridApps.filter { (it.gridPosition ?: 0) >= 100 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bgVoid)
    ) {
        // 1. Top HUD Area (HUD panels nested inside NodeRail horizontally, takes ~260dp height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(top = 16.dp, start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation Rail on the left (only spans the height of the HUD area)
            NodeRail(
                nodes = railNodes,
                activeNodeId = when (pagerState.currentPage) {
                    0 -> "home"
                    1 -> "system"
                    else -> "media"
                },
                onNodeSelected = { id ->
                    coroutineScope.launch {
                        val targetPage = when (id) {
                            "home" -> 0
                            "system" -> 1
                            else -> 2
                        }
                        pagerState.animateScrollToPage(targetPage)
                    }
                },
                modifier = Modifier.fillMaxHeight()
            )

            // Active HUD panel on the right
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { page ->
                when (page) {
                    0 -> ClockWeatherPanel()
                    1 -> SystemHUDPanel(state = state)
                    2 -> MediaPanel()
                }
            }
        }

        // Pager indicator dots (under the HUD panel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 8.dp else 6.dp)
                        .background(
                            color = if (active) theme.accentPrimary else theme.textSecondary.copy(alpha = 0.4f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }

        // 2. Middle App Grid Area (occupies full width of the screen!)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val columns = 4
            val chunkedApps = gridApps.chunked(columns)
            chunkedApps.forEach { rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowApps.forEach { app ->
                        HomeAppGridItem(
                            app = app,
                            onClick = {
                                try {
                                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    intent?.let { context.startActivity(it) }
                                } catch (e: Exception) {
                                    // ignored
                                }
                            }
                        )
                    }
                    // Keep empty slots spaced properly
                    repeat(columns - rowApps.size) {
                        Spacer(modifier = Modifier.size(56.dp))
                    }
                }
            }
        }

        // 3. Floating App Dock (Bottom Panel - occupies full width!)
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search drawer navigation
                CarvedIcon(
                    size = 48.dp,
                    icon = { Icon(Icons.Default.Search, "App Drawer", tint = theme.accentSecondary) },
                    contentDescription = "Search Drawer",
                    onClick = onNavigateToDrawer
                )

                // Pinned applications
                dockApps.forEach { app ->
                    val iconDrawable = androidx.compose.runtime.remember(app.packageName) {
                        try {
                            context.packageManager.getApplicationIcon(app.packageName)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    CarvedIcon(
                        size = 48.dp,
                        icon = {
                            if (iconDrawable != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(iconDrawable),
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
                        onClick = {
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                intent?.let { context.startActivity(it) }
                            } catch (e: Exception) {
                                // ignored
                            }
                        }
                    )
                }

                // Launcher settings shortcut
                CarvedIcon(
                    size = 48.dp,
                    icon = { Icon(Icons.Default.Settings, "Launcher Settings", tint = theme.accentPrimary) },
                    contentDescription = "Settings",
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeAppGridItem(
    app: AppInfo,
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
            .padding(4.dp)
            .width(64.dp)
    ) {
        CarvedIcon(
            size = 48.dp,
            icon = {
                if (iconDrawable != null) {
                    Image(
                        painter = coil.compose.rememberAsyncImagePainter(iconDrawable),
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
        Text(
            text = app.label,
            color = theme.textSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
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
                text = "Volt Ambient Stream",
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
