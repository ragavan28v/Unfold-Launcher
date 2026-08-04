package com.volt.feature.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.volt.core.ui.components.CarvedIcon
import com.volt.core.ui.components.GlassPanel
import com.volt.core.ui.theme.LocalVoltTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    modifier: Modifier = Modifier,
    viewModel: AppDrawerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalVoltTheme.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bgVoid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar
            TextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onIntent(AppDrawerUiIntent.Search(it)) },
                placeholder = { Text("SEARCH SYSTEMS", color = theme.textSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = theme.bgPanel,
                    unfocusedContainerColor = theme.bgPanel,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    cursorColor = theme.accentPrimary,
                    focusedIndicatorColor = theme.accentPrimary,
                    unfocusedIndicatorColor = theme.panelBorder
                ),
                singleLine = true
            )

            Row(modifier = Modifier.weight(1f)) {
                // Apps Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.filteredApps) { app ->
                        AppItem(
                            app = app,
                            onClick = {
                                try {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    }
                                } catch (e: Exception) {
                                    // Ignored
                                }
                            },
                            onLongPress = {
                                selectedAppForMenu = app
                            }
                        )
                    }
                }

                // Fast scroll alphabet rail
                AlphabetFastScroll(
                    modifier = Modifier.width(32.dp),
                    letters = ('A'..'Z').toList(),
                    onLetterSelected = { letter ->
                        val index = state.filteredApps.indexOfFirst {
                            it.label.startsWith(letter, ignoreCase = true)
                        }
                        if (index != -1) {
                            coroutineScope.launch {
                                gridState.animateScrollToItem(index)
                            }
                        }
                    }
                )
            }
        }

        // Context Sheet
        if (selectedAppForMenu != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedAppForMenu = null },
                sheetState = sheetState,
                containerColor = theme.bgPanel
            ) {
                val app = selectedAppForMenu!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = app.label.uppercase(),
                        color = theme.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.onIntent(AppDrawerUiIntent.HideApp(app.packageName))
                            selectedAppForMenu = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentDanger),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("HIDE SYSTEM")
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                            selectedAppForMenu = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("SYSTEM INFO")
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                            selectedAppForMenu = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentDanger),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("UNINSTALL SYSTEM")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val theme = LocalVoltTheme.current
    val context = LocalContext.current
    val iconDrawable = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(8.dp)
    ) {
        CarvedIcon(
            contentDescription = app.label,
            onClick = onClick,
            onLongPress = onLongPress,
            icon = {
                if (iconDrawable != null) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(iconDrawable),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
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

@Composable
fun AlphabetFastScroll(
    modifier: Modifier = Modifier,
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit
) {
    val theme = LocalVoltTheme.current
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                color = theme.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onLetterSelected(letter) }
                    .padding(vertical = 2.dp)
            )
        }
    }
}
