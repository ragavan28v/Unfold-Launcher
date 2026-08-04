package com.ragavan.unfold.ui.workspace

import androidx.compose.runtime.Composable
import com.ragavan.unfold.viewmodel.state.HomeState
import com.ragavan.unfold.ui.layers.HomeLayer

@Composable
fun WorkspacePage(
    state: HomeState
) {
    HomeLayer(state)
}