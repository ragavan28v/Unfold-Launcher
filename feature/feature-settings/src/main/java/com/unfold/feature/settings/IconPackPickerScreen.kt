package com.unfold.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.unfold.core.ui.components.CarvedIcon
import com.unfold.core.ui.iconpack.IconPackInfo
import com.unfold.core.ui.iconpack.IconPackResolver
import com.unfold.core.ui.theme.LocalUnfoldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun IconPackPickerScreen(
    viewModel: LauncherSettingsViewModel = hiltViewModel()
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val cachedPacks = remember { IconPackResolver.getCachedIconPacks(context) }
    var iconPacks by remember { mutableStateOf(cachedPacks) }
    var selectedPack by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(cachedPacks.isEmpty()) }
    val settingsState by viewModel.uiState.collectAsState()
    val previewPackages = remember {
        context.packageManager.queryIntentActivities(
            android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER),
            0
        ).asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .take(5)
            .toList()
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val refreshed = IconPackResolver.refreshInstalledIconPacks(context)
            withContext(Dispatchers.Main) {
                iconPacks = refreshed
                isLoading = false
            }
        }
        selectedPack = settingsState.themeConfig.iconPackPackage
            .ifBlank { IconPackResolver.getSelectedIconPack(context) }
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SCANNING SYSTEM...",
                        color = theme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "PREVIEW",
                        color = theme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    IconPackPreview(
                        packages = previewPackages,
                        iconPackPackage = selectedPack,
                        applyRing = settingsState.themeConfig.applyIconPackRing,
                        theme = theme
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "APPLY LAUNCHER THEME RING",
                                color = theme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Wrap third-party icons in the HUD ring.",
                                color = theme.textSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settingsState.themeConfig.applyIconPackRing,
                            onCheckedChange = viewModel::setIconPackRing
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            IconPackGridItem(
                                previewPackageName = "",
                                iconPackPackage = "",
                                applyRing = settingsState.themeConfig.applyIconPackRing,
                                label = "System Icons",
                                isSelected = selectedPack.isBlank(),
                                onClick = {
                                    selectedPack = ""
                                    viewModel.setIconPack("")
                                },
                                theme = theme
                            )
                        }
                        items(iconPacks) { pack ->
                            IconPackGridItem(
                                previewPackageName = pack.previewPackageName.ifBlank {
                                    previewPackages.firstOrNull().orEmpty()
                                },
                                iconPackPackage = pack.packageName,
                                applyRing = settingsState.themeConfig.applyIconPackRing,
                                label = pack.label,
                                isSelected = selectedPack == pack.packageName,
                                onClick = {
                                    selectedPack = pack.packageName
                                    viewModel.setIconPack(pack.packageName)
                                },
                                theme = theme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackPreview(
    packages: List<String>,
    iconPackPackage: String,
    applyRing: Boolean,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.bgPanel.copy(alpha = 0.35f))
            .border(1.dp, theme.panelBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        packages.forEach { packageName ->
            val context = LocalContext.current
            val bitmap by produceState<ImageBitmap?>(null, packageName, iconPackPackage) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    IconPackResolver.resolveAppIconDrawable(
                        context,
                        packageName,
                        iconPackPackage.takeIf { it.isNotBlank() }
                    )?.let { drawable ->
                        runCatching { drawable.toBitmap(72, 72).asImageBitmap() }.getOrNull()
                    }
                }
            }
            Crossfade(targetState = bitmap, label = "icon pack preview") { previewBitmap ->
                if (previewBitmap != null) {
                    CarvedIcon(
                        size = 42.dp,
                        raw = iconPackPackage.isNotBlank() && !applyRing,
                        contentDescription = packageName,
                        icon = {
                            androidx.compose.foundation.Image(
                                bitmap = previewBitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                } else {
                    Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Text("?", color = theme.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackGridItem(
    previewPackageName: String,
    iconPackPackage: String,
    applyRing: Boolean,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val previewBitmap by produceState<ImageBitmap?>(null, previewPackageName, iconPackPackage) {
            value = withContext(Dispatchers.IO) {
                if (iconPackPackage.isBlank()) {
                    null
                } else {
                    runCatching {
                        context.packageManager.getApplicationIcon(iconPackPackage)
                            .toBitmap(96, 96)
                            .asImageBitmap()
                    }.getOrNull()
                }
            }
        }
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (iconPackPackage.isBlank()) {
                CarvedIcon(
                    size = 52.dp,
                    contentDescription = label,
                    onClick = onClick,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = theme.textPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                )
            } else if (previewBitmap != null) {
                CarvedIcon(
                    size = 52.dp,
                    raw = iconPackPackage.isNotBlank() && !applyRing,
                    contentDescription = label,
                    onClick = onClick,
                    icon = {
                        androidx.compose.foundation.Image(
                            bitmap = previewBitmap!!,
                            contentDescription = label,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            } else {
                Text(
                    text = label.take(1).uppercase(),
                    color = theme.textSecondary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(theme.accentPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = theme.bgVoid,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            color = if (isSelected) theme.accentPrimary else theme.textSecondary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
