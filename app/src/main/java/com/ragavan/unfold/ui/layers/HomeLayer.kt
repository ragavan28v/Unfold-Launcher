package com.ragavan.unfold.ui.layers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ragavan.unfold.ui.components.icon.AppIcon
import com.ragavan.unfold.ui.design.LauncherDimensions
import com.ragavan.unfold.viewmodel.state.HomeState

@Composable
fun HomeLayer(
    state: HomeState
) {

    LazyVerticalGrid(

        columns = GridCells.Fixed(4),

        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = LauncherDimensions.ScreenPadding,
                end = LauncherDimensions.ScreenPadding,
                top = LauncherDimensions.HomeTopPadding,
                bottom = LauncherDimensions.HomeBottomPadding
            ),

        horizontalArrangement = Arrangement.spacedBy(
            LauncherDimensions.GridSpacing
        ),

        verticalArrangement = Arrangement.spacedBy(
            LauncherDimensions.GridSpacing
        ),

        contentPadding = PaddingValues(4.dp)

    ) {

        items(state.layout.items) { homeItem ->

            val app = state.installedApps
                .find { installed ->

                    installed.packageName ==
                            homeItem.packageName

                }

            app?.let { installedApp ->

                AppIcon(installedApp)

            }

        }

    }

}