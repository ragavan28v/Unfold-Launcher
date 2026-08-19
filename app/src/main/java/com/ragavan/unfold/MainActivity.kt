package com.ragavan.unfold

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import com.unfold.core.ui.theme.UnfoldTheme
import com.unfold.feature.drawer.AppDrawerScreen
import com.unfold.feature.drawer.AppDrawerViewModel
import com.unfold.feature.home.HomeScreen
import com.unfold.feature.home.HomeViewModel
import com.unfold.feature.hiddenspace.HiddenAppsScreen
import com.unfold.feature.search.UniversalSearchScreen
import com.unfold.feature.search.UniversalSearchViewModel
import com.unfold.feature.settings.LauncherSettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    @javax.inject.Inject
    lateinit var gestureActionResolver: com.unfold.feature.gestures.GestureActionResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                    fun isNotificationAccessEnabled(): Boolean {
                        val enabledListeners = Settings.Secure.getString(
                            contentResolver,
                            "enabled_notification_listeners"
                        )
                        return enabledListeners?.contains(packageName) == true
                    }

                    LaunchedEffect(Unit) {
                        if (!permissionPrefs.getBoolean("notification_access_prompt_seen", false) &&
                            !isNotificationAccessEnabled()
                        ) {
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
                        }
                        lifecycle.addObserver(observer)
                        onDispose { lifecycle.removeObserver(observer) }
                    }

                    val launcherContent: @Composable () -> Unit = {
                        NavHost(
                            navController = navController,
                            startDestination = UnfoldRoute.Home.route
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
                                    onNavigateToHiddenSpace = {
                                        navController.navigate(UnfoldRoute.HiddenSpace.route)
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
                                    }
                                )
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
                            launcherContent()
                            BottomEdgeHomeSwipeOverlay(
                                onSwipeHome = {
                                    navController.popBackStack(UnfoldRoute.Home.route, false)
                                }
                            )
                        }

                        if (showNotificationAccessPrompt) {
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
    onSwipeHome: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(48.dp)
                .pointerInput(onSwipeHome) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDx = 0f
                        var totalDy = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                break
                            }
                            val delta = change.positionChange()
                            totalDx += delta.x
                            totalDy += delta.y

                            if (totalDy < -96f && kotlin.math.abs(totalDx) < 72f) {
                                change.consume()
                                onSwipeHome()
                                break
                            }
                        }
                    }
                }
        )
    }
}


