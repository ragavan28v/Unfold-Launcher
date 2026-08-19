package com.unfold.feature.hiddenspace

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserManager
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.ui.components.CarvedIcon
import com.unfold.core.ui.components.GlassPanel
import com.unfold.core.ui.theme.LocalUnfoldTheme
import com.unfold.core.ui.theme.UnfoldThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HiddenAppsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: HiddenSpaceViewModel = hiltViewModel()
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val uiState by viewModel.uiState.collectAsState()

    var isUnlocked by rememberSaveable { mutableStateOf(activity == null) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    var authAttempt by rememberSaveable { mutableStateOf(0) }
    
    var showManageSheet by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(activity, authAttempt) {
        val host = activity
        if (host == null) {
            isUnlocked = true
            return@LaunchedEffect
        }

        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    isUnlocked = true
                    authError = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authError = errString.toString()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("UNFOLD SECURE STORAGE")
            .setSubtitle("Use your PIN, pattern, fingerprint, or face unlock")
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    val hiddenApps = uiState.hiddenApps
    val managedApps = uiState.allApps

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bgVoid)
            .statusBarsPadding()
    ) {
        if (!isUnlocked) {
            LockedView(
                onBack = onBack,
                authError = authError,
                onRetry = { authAttempt += 1 },
                theme = theme
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderRow(
                    onBack = onBack,
                    hiddenCount = hiddenApps.size,
                    theme = theme,
                    onManageClick = { showManageSheet = true }
                )

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "HIDDEN CABINET",
                            color = theme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Secure applications stored here are invisible to the main drawer. Tap an icon to launch.",
                            color = theme.textSecondary,
                            fontSize = 12.sp
                        )

                        if (hiddenApps.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Cabinet is empty",
                                        color = theme.accentPrimary.copy(alpha = 0.6f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showManageSheet = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("ADD APPS", color = theme.accentPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 76.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(hiddenApps, key = { it.appId }) { app ->
                                    HiddenAppIcon(
                                        app = app,
                                        onClick = { 
                                            launchApp(context, app)
                                            onBack()
                                        },
                                        theme = theme,
                                        iconPackPackage = uiState.iconPackPackage
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Management Layer
            if (showManageSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showManageSheet = false }
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(theme.bgPanel)
                        .clickable(enabled = false) {}
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MANAGE VISIBILITY",
                            color = theme.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showManageSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = theme.textPrimary)
                        }
                    }
                    
                    Text(
                        text = "Apps toggled OFF will appear in your hidden cabinet.",
                        color = theme.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(managedApps, key = { it.appId }) { app ->
                            HiddenAppRow(
                                app = app,
                                isHidden = app.isHidden,
                                onToggleHidden = { hidden ->
                                    viewModel.setHidden(app.appId, hidden)
                                },
                                theme = theme,
                                iconPackPackage = uiState.iconPackPackage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenAppIcon(
    app: AppInfo,
    onClick: () -> Unit,
    theme: UnfoldThemeColors,
    iconPackPackage: String = ""
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.appId) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = com.unfold.core.ui.iconpack.IconPackResolver.resolveAppIconDrawable(
                    context,
                    app.packageName,
                    iconPackPackage.takeIf { it.isNotBlank() }
                )
                if (drawable != null) drawableToImageBitmap(drawable) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .width(76.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CarvedIcon(
            size = 56.dp,
            raw = iconPackPackage.isNotBlank(),
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
            onClick = onClick
        )
        Text(
            text = app.label,
            color = theme.textPrimary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        )
    }
}

@Composable
private fun HeaderRow(
    onBack: () -> Unit,
    hiddenCount: Int,
    theme: UnfoldThemeColors,
    onManageClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "HIDDEN SPACE",
                color = theme.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "$hiddenCount Secure Applications",
                color = theme.accentSecondary,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onManageClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(theme.bgPanel.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Manage", tint = theme.accentPrimary, modifier = Modifier.size(20.dp))
            }
            
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(theme.accentPrimary, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = theme.bgVoid, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun LockedView(
    onBack: () -> Unit,
    authError: String?,
    onRetry: () -> Unit,
    theme: UnfoldThemeColors
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(0.85f),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SECURE CABINET LOCKED",
                    color = theme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Authenticate with your device security to access hidden applications.",
                    color = theme.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                if (authError != null) {
                    Text(
                        text = authError,
                        color = theme.accentDanger,
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.accentPrimary,
                            contentColor = theme.bgVoid
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RETRY")
                    }
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.bgPanel,
                            contentColor = theme.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("BACK")
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenAppRow(
    app: AppInfo,
    isHidden: Boolean,
    onToggleHidden: (Boolean) -> Unit,
    theme: UnfoldThemeColors,
    iconPackPackage: String = ""
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.appId) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = com.unfold.core.ui.iconpack.IconPackResolver.resolveAppIconDrawable(
                    context,
                    app.packageName,
                    iconPackPackage.takeIf { it.isNotBlank() }
                )
                if (drawable != null) drawableToImageBitmap(drawable) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, theme.panelBorder.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .background(theme.bgPanel.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, theme.accentPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = iconBitmap
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        text = app.label.take(2).uppercase(),
                        color = theme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = app.label,
                color = theme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Switch(
            checked = isHidden,
            onCheckedChange = onToggleHidden,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.accentPrimary,
                checkedTrackColor = theme.accentPrimary.copy(alpha = 0.4f)
            )
        )
    }
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

        // Fallback to launch intent
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchIntent?.let { context.startActivity(it) }
    } catch (e: Exception) {
        android.util.Log.e("HiddenSpace", "Failed to launch app: ${app.packageName}", e)
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
