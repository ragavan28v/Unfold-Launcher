package com.unfold.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unfold.core.ui.components.GlassPanel
import com.unfold.core.ui.iconpack.IconPackInfo
import com.unfold.core.ui.iconpack.IconPackResolver
import com.unfold.core.ui.theme.LocalUnfoldTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPackPickerScreen(
    onBack: () -> Unit,
    viewModel: LauncherSettingsViewModel = hiltViewModel()
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var iconPacks by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    var selectedPack by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            iconPacks = IconPackResolver.detectInstalledIconPacks(context)
            selectedPack = IconPackResolver.getSelectedIconPack(context)
            isLoading = false
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ICON MANAGER",
                        color = theme.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = "Global icon customization",
                        color = theme.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        IconPackGridItem(
                            label = "System Icons",
                            subtitle = "Default HUD look",
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
                            label = pack.label,
                            subtitle = "${pack.drawableCount} icons",
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

@Composable
private fun IconPackGridItem(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) theme.accentPrimary else theme.panelBorder.copy(alpha = 0.2f),
        label = "icon pack selection border"
    )
    val panelOpacity by animateFloatAsState(
        targetValue = if (isSelected) 0.85f else 0.6f,
        label = "icon pack selection opacity"
    )
    
    GlassPanel(
        modifier = Modifier
            .aspectRatio(0.9f)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                }
            ),
        cornerRadius = 16.dp,
        opacity = panelOpacity
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) theme.accentPrimary.copy(alpha = 0.2f) else theme.bgPanel.copy(alpha = 0.8f))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = theme.accentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = label.take(1).uppercase(),
                        color = theme.textSecondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = label.uppercase(),
                color = if (isSelected) theme.accentPrimary else theme.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 1.sp
            )
            
            Text(
                text = subtitle.lowercase(),
                color = theme.textSecondary,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
