package com.ragavan.unfold.ui.layers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.ragavan.unfold.engine.gesture.GestureState
import com.ragavan.unfold.ui.drawer.AppDrawer
import com.ragavan.unfold.viewmodel.state.HomeState

@Composable
fun DrawerLayer(

    gestureState: GestureState,

    state: HomeState

) {

    AppDrawer(

        state = state,

        modifier = Modifier

            .fillMaxSize()

            .offset {

                IntOffset(

                    0,

                    ((1f - gestureState.progress) * 1200f)
                        .toInt()

                )

            }

    )

}