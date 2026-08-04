package com.ragavan.unfold.ui.layers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ragavan.unfold.data.apps.AppInfo
import com.ragavan.unfold.ui.components.glass.GlassCapsule
import com.ragavan.unfold.ui.components.icon.DockIcon
import com.ragavan.unfold.ui.design.LauncherDimensions
import com.ragavan.unfold.ui.components.dock.DockState

@Composable
fun DockLayer(
    state: DockState
) {

    Box(

        modifier = Modifier
            .fillMaxSize(),

        contentAlignment = Alignment.BottomCenter

    ) {

        GlassCapsule(

            modifier = Modifier

                .navigationBarsPadding()

                .padding(
                    horizontal = 24.dp,
                    vertical = 18.dp
                )

                .fillMaxWidth()

                .height(LauncherDimensions.DockHeight)

        ) {

            Row(

                modifier = Modifier
                    .fillMaxSize(),

                horizontalArrangement = Arrangement.SpaceEvenly,

                verticalAlignment = Alignment.CenterVertically

            ) {

                state.apps.forEach {

                    DockIcon(it)

                }

            }

        }

    }

}