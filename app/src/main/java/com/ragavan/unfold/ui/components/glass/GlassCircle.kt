package com.ragavan.unfold.ui.components.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun GlassCircle(
    size: Dp,
    content: @Composable () -> Unit
) {

    GlassSurface(
        modifier = Modifier.size(size)
    ) {

        Box {

            content()

        }

    }

}