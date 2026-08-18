package com.ragavan.unfold

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
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
                    }
                }
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


