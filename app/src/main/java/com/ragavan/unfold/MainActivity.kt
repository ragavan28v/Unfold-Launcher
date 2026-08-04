package com.ragavan.unfold

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.volt.core.domain.navigation.VoltRoute
import com.volt.core.ui.theme.VoltTheme
import com.volt.feature.home.HomeScreen
import com.volt.feature.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

import com.volt.feature.drawer.AppDrawerScreen
import com.volt.feature.drawer.AppDrawerViewModel
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

                    com.volt.feature.gestures.GestureDetectorOverlay(
                        onGestureDetected = { gestureType ->
                            scope.launch {
                                gestureActionResolver.execute(gestureType, navController)
                            }
                        }
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = VoltRoute.Home.route
                        ) {
                            composable(VoltRoute.Home.route) {
                                val homeViewModel: HomeViewModel = hiltViewModel()
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToDrawer = {
                                        navController.navigate(VoltRoute.AppDrawer.route)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(VoltRoute.Settings.route)
                                    }
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
                            // Placeholder for settings
                        }
                    }
                }
            }
        }
    }
}
}