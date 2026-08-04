package com.ragavan.unfold.ui.components.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GlassCapsule(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    GlassSurface(
        modifier = modifier,
        content = content
    )

}