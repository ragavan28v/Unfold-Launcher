package com.ragavan.unfold.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragavan.unfold.ui.components.dock.DockState
import com.ragavan.unfold.ui.layers.DockLayer
import com.ragavan.unfold.ui.layers.GridLayer
import com.ragavan.unfold.ui.layers.StatusLayer
import com.ragavan.unfold.ui.layers.WallpaperLayer
import com.ragavan.unfold.ui.workspace.Workspace
import com.ragavan.unfold.viewmodel.HomeViewModel
import androidx.compose.runtime.remember
import com.ragavan.unfold.engine.gesture.GestureState
import com.ragavan.unfold.ui.layers.DrawerLayer

@Composable
fun LauncherScreen() {

    val vm: HomeViewModel = viewModel()

    val state by vm.state.collectAsState()

    val gestureState = remember {
        GestureState()
    }

    LauncherScaffold {

            WallpaperLayer()

            GridLayer()

            DrawerLayer(

                gestureState,

                state

            )

            LauncherSurface(
                gestureState
            ) {

                Workspace(state)

                StatusLayer()

                DockLayer(
                    DockState(
                        apps = state.dockLayout.items.mapNotNull { dockItem ->

                            state.installedApps.find {

                                it.packageName == dockItem.packageName

                            }

                        }

                    )

                )

            }

    }
}