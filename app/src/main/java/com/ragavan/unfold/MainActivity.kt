package com.ragavan.unfold

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.volt.core.domain.navigation.VoltRoute
import com.volt.core.ui.theme.VoltTheme
import com.volt.feature.drawer.AppDrawerScreen
import com.volt.feature.drawer.AppDrawerViewModel
import com.volt.feature.home.HomeScreen
import com.volt.feature.home.HomeViewModel
import com.volt.feature.search.UniversalSearchScreen
import com.volt.feature.search.UniversalSearchViewModel
import com.volt.feature.settings.LauncherSettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var gestureActionResolver: com.volt.feature.gestures.GestureActionResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoltTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoltTheme.current.bgVoid
                ) {
                    val navController = rememberNavController()
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                    val launcherContent: @Composable () -> Unit = {
                        NavHost(
                            navController = navController,
                            startDestination = VoltRoute.Home.route
                        ) {
                            composable(VoltRoute.Home.route) {
                                val homeViewModel: HomeViewModel = hiltViewModel()
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToSearch = {
                                        navController.navigate(VoltRoute.UniversalSearch.route)
                                    },
                                    onNavigateToDrawer = {
                                        navController.navigate(VoltRoute.AppDrawer.route)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(VoltRoute.Settings.route)
                                    }
                                )
                            }

                            composable(VoltRoute.UniversalSearch.route) {
                                val searchViewModel: UniversalSearchViewModel = hiltViewModel()
                                UniversalSearchScreen(
                                    viewModel = searchViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(VoltRoute.AppDrawer.route) {
                                val drawerViewModel: AppDrawerViewModel = hiltViewModel()
                                AppDrawerScreen(
                                    viewModel = drawerViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(VoltRoute.Settings.route) {
                                LauncherSettingsScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenGestureControl = {
                                        navController.navigate(VoltRoute.GestureSettings.route)
                                    }
                                )
                            }

                            composable(VoltRoute.GestureSettings.route) {
                                com.volt.feature.settings.GestureControlSettingsScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    if (currentRoute == VoltRoute.Home.route) {
                        com.volt.feature.gestures.GestureDetectorOverlay(
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
                    }
                }
            }
        }
    }
}
