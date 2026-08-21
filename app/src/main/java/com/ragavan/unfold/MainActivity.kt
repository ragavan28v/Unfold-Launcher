package com.ragavan.unfold

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.app.role.RoleManager
import android.os.Build
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unfold.core.domain.navigation.UnfoldRoute
import com.unfold.core.domain.model.GestureType
import com.unfold.core.ui.theme.UnfoldTheme
import com.unfold.feature.drawer.AppDrawerScreen
import com.unfold.feature.drawer.AppDrawerViewModel
import com.unfold.feature.home.HomeScreen
import com.unfold.feature.home.HomeViewModel
import com.unfold.feature.hiddenspace.HiddenAppsScreen
import com.unfold.feature.search.UniversalSearchScreen
import com.unfold.feature.search.UniversalSearchViewModel
import com.unfold.feature.settings.LauncherSettingsScreen
import com.unfold.feature.settings.AboutScreen
import com.unfold.feature.settings.LicenseScreen
import com.unfold.feature.settings.ThirdPartyNoticesScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    @javax.inject.Inject
    lateinit var gestureActionResolver: com.unfold.feature.gestures.GestureActionResolver

    private val _newIntentFlow = kotlinx.coroutines.flow.MutableSharedFlow<Intent>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        _newIntentFlow.tryEmit(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = AndroidColor.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightNavigationBars = false
        setContent {
            UnfoldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UnfoldTheme.current.bgVoid
                ) {
                    val navController = rememberNavController()
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    val permissionPrefs = remember {
                        getSharedPreferences("first_run_permissions", Context.MODE_PRIVATE)
                    }
                    var showNotificationAccessPrompt by remember { mutableStateOf(false) }
                    var showDefaultLauncherPrompt by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        _newIntentFlow.collect { intent ->
                            if (intent.hasCategory(Intent.CATEGORY_HOME)) {
                                scope.launch {
                                    gestureActionResolver.execute(GestureType.EDGE_SWIPE, navController)
                                }
                            }
                        }
                    }

                    fun isNotificationAccessEnabled(): Boolean {
                        val enabledListeners = Settings.Secure.getString(
                            contentResolver,
                            "enabled_notification_listeners"
                        )
                        return enabledListeners?.contains(packageName) == true
                    }

                    fun isDefaultLauncher(): Boolean {
                        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            getSystemService(RoleManager::class.java)
                                ?.isRoleHeld(RoleManager.ROLE_HOME) == true
                        } else {
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                            }
                            packageManager.resolveActivity(
                                homeIntent,
                                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                            )?.activityInfo?.packageName == packageName
                        }
                    }

                    fun openDefaultLauncherSettings() {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val roleManager = getSystemService(RoleManager::class.java)
                                if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
                                    startActivityForResult(
                                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                                        DEFAULT_HOME_REQUEST_CODE
                                    )
                                    return
                                }
                            }
                            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                        } catch (_: Exception) {
                            startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }

                    fun shouldShowNotificationPrompt(): Boolean {
                        return !permissionPrefs.getBoolean("notification_access_prompt_seen", false) &&
                            !isNotificationAccessEnabled()
                    }

                    LaunchedEffect(Unit) {
                        if (!permissionPrefs.getBoolean("default_launcher_prompt_seen", false) &&
                            !isDefaultLauncher()
                        ) {
                            showDefaultLauncherPrompt = true
                        } else if (shouldShowNotificationPrompt()) {
                            showNotificationAccessPrompt = true
                        }
                    }

                    DisposableEffect(Unit) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME && isNotificationAccessEnabled()) {
                                showNotificationAccessPrompt = false
                                permissionPrefs.edit()
                                    .putBoolean("notification_access_prompt_seen", true)
                                    .apply()
                            }
                            if (event == Lifecycle.Event.ON_RESUME && isDefaultLauncher()) {
                                showDefaultLauncherPrompt = false
                                permissionPrefs.edit()
                                    .putBoolean("default_launcher_prompt_seen", true)
                                    .apply()
                                if (shouldShowNotificationPrompt()) {
                                    showNotificationAccessPrompt = true
                                }
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose { lifecycle.removeObserver(observer) }
                    }

                    val launcherContent: @Composable () -> Unit = {
                        NavHost(
                            navController = navController,
                            startDestination = UnfoldRoute.Home.route,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    tween(220)
                                ) + fadeIn(tween(220))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    tween(220)
                                ) + fadeOut(tween(220))
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    tween(220)
                                ) + fadeIn(tween(220))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    tween(220)
                                ) + fadeOut(tween(220))
                            }
                        ) {
                            composable(UnfoldRoute.Home.route) {
                                val homeViewModel: HomeViewModel = hiltViewModel()
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToSearch = {
                                        navController.navigate(UnfoldRoute.UniversalSearch.route)
                                    },
                                    onNavigateToDrawer = {
                                        navController.navigate(UnfoldRoute.AppDrawer.route)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(UnfoldRoute.Settings.route)
                                    },
                                    onDockSwipeHold = {
                                        scope.launch {
                                            gestureActionResolver.execute(GestureType.DOCK_SWIPE_HOLD, navController)
                                        }
                                    }
                                )
                            }

                            composable(UnfoldRoute.UniversalSearch.route) {
                                val searchViewModel: UniversalSearchViewModel = hiltViewModel()
                                UniversalSearchScreen(
                                    viewModel = searchViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(UnfoldRoute.AppDrawer.route) {
                                val drawerViewModel: AppDrawerViewModel = hiltViewModel()
                                AppDrawerScreen(
                                    viewModel = drawerViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(UnfoldRoute.Settings.route) {
                                LauncherSettingsScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenGestureControl = {
                                        navController.navigate(UnfoldRoute.GestureSettings.route)
                                    },
                                    onOpenSearch = {
                                        navController.navigate(UnfoldRoute.UniversalSearch.route)
                                    },
                                    onOpenHiddenSpace = {
                                        navController.navigate(UnfoldRoute.HiddenSpace.route)
                                    },
                                    onOpenDefaultLauncherSettings = {
                                        openDefaultLauncherSettings()
                                    },
                                    onOpenAbout = {
                                        navController.navigate(UnfoldRoute.About.route)
                                    }
                                )
                            }

                            composable(UnfoldRoute.About.route) {
                                AboutScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenLicense = { navController.navigate(UnfoldRoute.License.route) },
                                    onOpenThirdPartyNotices = { navController.navigate(UnfoldRoute.ThirdPartyNotices.route) }
                                )
                            }

                            composable(UnfoldRoute.License.route) {
                                LicenseScreen(onBack = { navController.popBackStack() })
                            }

                            composable(UnfoldRoute.ThirdPartyNotices.route) {
                                ThirdPartyNoticesScreen(onBack = { navController.popBackStack() })
                            }

                            composable(UnfoldRoute.GestureSettings.route) {
                                com.unfold.feature.settings.GestureControlSettingsScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(UnfoldRoute.HiddenSpace.route) {
                                HiddenAppsScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (currentRoute == UnfoldRoute.Home.route) {
                            com.unfold.feature.gestures.GestureDetectorOverlay(
                                onGestureDetected = { gestureType ->
                                    scope.launch {
                                        gestureActionResolver.execute(gestureType, navController)
                                    }
                                }
                            ) {
                                launcherContent()
                            }
                        } else {
                            BottomEdgeHomeSwipeOverlay(
                                onSwipeHome = {
                                    scope.launch {
                                        gestureActionResolver.execute(GestureType.EDGE_SWIPE, navController)
                                    }
                                }
                            ) {
                                launcherContent()
                            }
                        }

                        if (showDefaultLauncherPrompt) {
                            DefaultLauncherPrompt(
                                onOpenSettings = {
                                    showDefaultLauncherPrompt = false
                                    openDefaultLauncherSettings()
                                },
                                onLater = {
                                    showDefaultLauncherPrompt = false
                                    permissionPrefs.edit()
                                        .putBoolean("default_launcher_prompt_seen", true)
                                        .apply()
                                    if (shouldShowNotificationPrompt()) {
                                        showNotificationAccessPrompt = true
                                    }
                                }
                            )
                        } else if (showNotificationAccessPrompt) {
                            FirstRunNotificationAccessPrompt(
                                onOpenSettings = {
                                    showNotificationAccessPrompt = false
                                    try {
                                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                    } catch (_: Exception) {
                                        startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                                onNotNow = {
                                    showNotificationAccessPrompt = false
                                    permissionPrefs.edit()
                                        .putBoolean("notification_access_prompt_seen", true)
                                        .apply()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_HOME_REQUEST_CODE = 4101
    }
}

@Composable
private fun DefaultLauncherPrompt(
    onOpenSettings: () -> Unit,
    onLater: () -> Unit
) {
    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1600),
        label = "welcome glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02070D))
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .border(1.dp, Color(0xFF0879C9).copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                .background(Color(0xFF030B14).copy(alpha = 0.96f), RoundedCornerShape(26.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "WELCOME TO",
                    color = Color(0xFF9AAABD),
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("U N F ", color = Color.White, fontSize = 40.sp, letterSpacing = 4.sp)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(5.dp, Color(0xFF10A7F5), RoundedCornerShape(50))
                    )
                    Text(" L D", color = Color.White, fontSize = 40.sp, letterSpacing = 4.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.size(42.dp, 2.dp).background(Color(0xFF0EA5F7)))
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "SIMPLIFY. FOCUS. UNFOLD.",
                    color = Color.White,
                    fontSize = 18.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "A clean, minimal launcher that keeps what matters, front and center.",
                    color = Color(0xFF9AAABD),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            WelcomePhoneIllustration(glow = pulse)

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DESIGNED FOR YOU",
                    color = Color(0xFF0EA5F7),
                    fontSize = 17.sp,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                WelcomeFeature("⌂", "Minimal Home", "A clean and focused home screen.", Color(0xFF10A7F5))
                WelcomeFeature("▦", "Smart Dock", "Quick access to your favorite apps.", Color(0xFF00D6A3))
                WelcomeFeature(Icons.Default.TouchApp, "Gestures", "Intuitive gestures for a smoother experience.", Color(0xFFA56BFF))
                WelcomeFeature("□", "App Drawer", "All your apps, organized and easy to find.", Color(0xFFFFAE2B))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087DF0)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("SET UNFOLD AS DEFAULT    ›", color = Color.White, fontSize = 15.sp, letterSpacing = 1.sp)
                }
                TextButton(onClick = onLater) {
                    Text("I'LL SET IT LATER", color = Color(0xFF0EA5F7), fontSize = 13.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun WelcomePhoneIllustration(glow: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val arcWidth = size.width * 0.84f
            val arcHeight = size.height * 0.72f
            val arcTop = size.height * 0.50f
            val arcLeft = (size.width - arcWidth) / 2f
            val arcSize = Size(arcWidth, arcHeight)
            val arcTopLeft = Offset(arcLeft, arcTop)
            val arcColor = Color(0xFF0B8FFF)

            drawArc(
                color = arcColor.copy(alpha = 0.08f * glow),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = arcColor.copy(alpha = 0.14f * glow),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = arcColor.copy(alpha = 0.58f * glow),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = arcColor.copy(alpha = 0.72f * glow),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight(0.88f)
                .aspectRatio(0.56f)
                .border(2.dp, Color(0xFF2E5B88), RoundedCornerShape(28.dp))
                .background(Color(0xFF02070D), RoundedCornerShape(28.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(34.dp, 4.dp).background(Color(0xFF0B1A2A), RoundedCornerShape(4.dp)))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFF0B1828), RoundedCornerShape(11.dp))
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF071726), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(4) {
                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF1C4165), RoundedCornerShape(50)))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeature(icon: String, title: String, subtitle: String, color: Color) {
    WelcomeFeatureContent(icon, title, subtitle, color)
}

@Composable
private fun WelcomeFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, color: Color) {
    WelcomeFeatureContent(icon, title, subtitle, color)
}

@Composable
private fun WelcomeFeatureContent(icon: Any, title: String, subtitle: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            if (icon is String) {
                Text(icon, color = color, fontSize = 24.sp, textAlign = TextAlign.Center)
            } else if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF9AAABD), fontSize = 11.sp)
        }
    }
}

@Composable
private fun FirstRunNotificationAccessPrompt(
    onOpenSettings: () -> Unit,
    onNotNow: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onNotNow) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0F14), RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ENABLE UNFOLD NOTIFICATION ACCESS",
                color = Color(0xFF38BDF8),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "This permission enables notification badges, unread counts, live music track details, and media controls.",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp
            )
            Text(
                text = "Android will open Notification access. Select Unfold and turn it on.",
                color = Color.White.copy(alpha = 0.64f),
                fontSize = 12.sp,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "OPEN NOTIFICATION ACCESS",
                    color = Color(0xFF071018),
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(
                onClick = onNotNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("NOT NOW", color = Color.White.copy(alpha = 0.62f))
            }
        }
    }
}

@Composable
private fun BottomEdgeHomeSwipeOverlay(
    onSwipeHome: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val edgeHeightPx = with(density) { BOTTOM_EDGE_GESTURE_HEIGHT.toPx() }
    val swipeThresholdPx = with(density) { BOTTOM_EDGE_SWIPE_THRESHOLD.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onSwipeHome, edgeHeightPx, swipeThresholdPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    if (down.position.y < size.height - edgeHeightPx) {
                        return@awaitEachGesture
                    }

                    var gestureCancelled = false
                    val startTime = System.currentTimeMillis()
                    var lastDisplacement = Offset.Zero

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        
                        if (!change.pressed) {
                            // Finger released
                            val duration = System.currentTimeMillis() - startTime
                            val verticalDistance = -lastDisplacement.y
                            val horizontalDistance = kotlin.math.abs(lastDisplacement.x)
                            
                            val hasMovedPastThreshold = lastDisplacement.getDistance() >= swipeThresholdPx
                            val isClearlyVertical = verticalDistance >= horizontalDistance * 1.5f
                            val isQuickSwipe = duration < 300 // Quick swipe threshold (300ms)
                            
                            if (!gestureCancelled && hasMovedPastThreshold && verticalDistance >= swipeThresholdPx && isClearlyVertical && isQuickSwipe) {
                                change.consume()
                                onSwipeHome()
                            }
                            break
                        }

                        lastDisplacement = change.position - down.position
                        
                        val verticalDistance = -lastDisplacement.y
                        val horizontalDistance = kotlin.math.abs(lastDisplacement.x)
                        val hasMovedPastThreshold = lastDisplacement.getDistance() >= swipeThresholdPx
                        val isClearlyVertical = verticalDistance >= horizontalDistance * 1.5f

                        if (hasMovedPastThreshold && !isClearlyVertical && verticalDistance <= 0f) {
                            gestureCancelled = true
                        }
                    }
                }
            }
    ) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BOTTOM_EDGE_GESTURE_HEIGHT)
                .systemGestureExclusion()
        )
    }
}

private val BOTTOM_EDGE_GESTURE_HEIGHT = 144.dp
private val BOTTOM_EDGE_SWIPE_THRESHOLD = 40.dp


