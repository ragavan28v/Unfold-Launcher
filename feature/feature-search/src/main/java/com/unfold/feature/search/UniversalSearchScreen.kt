package com.unfold.feature.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfold.core.ui.notification.NotificationBadgeStore
import coil.compose.rememberAsyncImagePainter
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.ui.components.CarvedIcon
import com.unfold.core.ui.theme.LocalUnfoldTheme
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: UniversalSearchViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val requiredPermissions = remember {
        buildList {
            add(android.Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.READ_MEDIA_IMAGES)
                add(android.Manifest.permission.READ_MEDIA_VIDEO)
                add(android.Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshDeviceSources()
    }

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
        isFocused = true

        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            viewModel.refreshDeviceSources()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bgVoid)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.textPrimary
                    )
                }

                TextField(
                    value = state.query,
                    onValueChange = { viewModel.onIntent(UniversalSearchUiIntent.QueryChanged(it)) },
                    placeholder = {
                        Text(
                            text = "Search apps, contacts, files",
                            color = theme.textSecondary
                                .copy(alpha = 0.95f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = theme.textSecondary
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = theme.bgPanel.copy(alpha = 0.72f),
                        unfocusedContainerColor = theme.bgPanel.copy(alpha = 0.56f),
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        cursorColor = theme.accentPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = state.recentSearches.isNotEmpty() && (state.query.isBlank() || isFocused),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    SectionLabel(text = "RECENT SEARCHES")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.recentSearches) { recent ->
                            RecentSearchChip(
                                text = recent,
                                onClick = {
                                    viewModel.onIntent(UniversalSearchUiIntent.RecentSelected(recent))
                                    keyboardController?.show()
                                },
                                theme = theme
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    SearchResultSection(
                        title = "APPS",
                        emptyHint = "No apps found",
                        count = state.filteredApps.size
                    ) {
                        state.filteredApps.forEach { app ->
                            itemResult(
                                app = app,
                                iconPackPackage = state.iconPackPackage,
                                onClick = {
                                    viewModel.onIntent(UniversalSearchUiIntent.QuerySubmitted(state.query))
                                    NotificationBadgeStore.clearInstance(
                                        NotificationBadgeStore.instanceKey(
                                            app.packageName,
                                            app.userSerial
                                        )
                                    )
                                    launchApp(context, app)
                                }
                            )
                        }
                    }
                }

                item {
                    SearchResultSection(
                        title = "CONTACTS",
                        emptyHint = "No contacts found",
                        count = state.filteredContacts.size
                    ) {
                        state.filteredContacts.forEach { contact ->
                            itemContact(
                                contact = contact,
                                onClick = {
                                    launchContacts(context)
                                }
                            )
                        }
                    }
                }

                item {
                    SearchResultSection(
                        title = "FILES",
                        emptyHint = "No files found",
                        count = state.filteredFiles.size
                    ) {
                        state.filteredFiles.forEach { file ->
                            itemFile(
                                name = file.name,
                                subtitle = file.folderPath ?: "File",
                                iconGlyph = "F",
                                onClick = {
                                    launchFile(context, file.uri, file.mimeType)
                                }
                            )
                        }
                    }
                }

                item {
                    SearchResultSection(
                        title = "FOLDERS",
                        emptyHint = "No folders found",
                        count = state.filteredFolders.size
                    ) {
                        state.filteredFolders.forEach { folder ->
                            itemFile(
                                name = folder.name,
                                subtitle = folder.path,
                                iconGlyph = "D",
                                onClick = {
                                    launchFolderPicker(context)
                                }
                            )
                        }
                    }
                }

                if (state.query.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SearchResultSection(
                            title = "WEB",
                            emptyHint = "Search the web",
                            count = 1
                        ) {
                            itemFile(
                                name = "Search \"${state.query.trim()}\"",
                                subtitle = "Open in browser",
                                iconGlyph = "W",
                                onClick = { launchWebSearch(context, state.query.trim()) }
                            )
                        }
                    }
                }

                if (!state.hasAnyResults) {
                    item {
                        Text(
                            text = "No results found",
                            color = theme.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultSection(
    title: String,
    emptyHint: String,
    count: Int,
    content: @Composable () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    if (count > 0) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(text = title)
            content()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(text = title)
            Text(
                text = emptyHint,
                color = theme.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val theme = LocalUnfoldTheme.current
    Text(
        text = text,
        color = theme.textSecondary,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RecentSearchChip(
    text: String,
    onClick: () -> Unit,
    theme: com.unfold.core.ui.theme.UnfoldThemeColors
) {
    Surface(
        shape = CircleShape,
        color = theme.bgPanel.copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.35f)),
        onClick = onClick
    ) {
        Text(
            text = text,
            color = theme.textPrimary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun itemResult(
    app: AppInfo,
    iconPackPackage: String = "",
    onClick: () -> Unit
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
                if (drawable != null) drawableToImageBitmap(drawable) else null
            } catch (_: Exception) {
                null
            }
        }
    }

    ResultSurface(
        title = app.label,
        subtitle = "",
        glyph = null,
        rawIcon = iconPackPackage.isNotBlank() &&
            !com.unfold.core.ui.iconpack.IconPackResolver.isLauncherRingEnabled(context),
        onClick = onClick
    ) {
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
    }
}

private fun drawableToImageBitmap(drawable: android.graphics.drawable.Drawable): ImageBitmap? {
    return try {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun itemContact(
    contact: UniversalSearchContact,
    onClick: () -> Unit
) {
    ResultSurface(
        title = contact.name,
        subtitle = contact.phoneNumber ?: "Contact",
        glyph = contact.name.take(1).uppercase(),
        onClick = onClick
    )
}

@Composable
private fun itemFile(
    name: String,
    subtitle: String,
    iconGlyph: String,
    onClick: () -> Unit
) {
    ResultSurface(
        title = name,
        subtitle = subtitle,
        glyph = iconGlyph,
        onClick = onClick
    )
}

@Composable
private fun ResultSurface(
    title: String,
    subtitle: String,
    glyph: String?,
    rawIcon: Boolean = false,
    onClick: () -> Unit,
    iconContent: (@Composable () -> Unit)? = null
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = theme.bgPanel.copy(alpha = 0.70f),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.30f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CarvedIcon(
                size = 42.dp,
                raw = rawIcon && !com.unfold.core.ui.iconpack.IconPackResolver.isLauncherRingEnabled(context),
                contentDescription = title,
                icon = {
                    if (iconContent != null) {
                        iconContent()
                    } else {
                        Text(
                            text = glyph ?: title.take(1).uppercase(),
                            color = theme.accentPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = theme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = theme.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun launchApp(context: Context, app: AppInfo) {
    try {
        val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)
        val userManager = context.getSystemService(android.os.UserManager::class.java)
        val userHandle = userManager?.getUserForSerialNumber(app.userSerial)
        if (launcherApps != null && userHandle != null && app.activityName.isNotBlank()) {
            launcherApps.startMainActivity(
                android.content.ComponentName(app.packageName, app.activityName),
                userHandle,
                null,
                null
            )
            return
        }

        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent != null) context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun launchContacts(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun launchFile(context: Context, uri: Uri, mimeType: String?) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open file"))
    } catch (_: Exception) {
    }
}

private fun launchFolderPicker(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun launchWebSearch(context: Context, query: String) {
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=$encoded")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}


