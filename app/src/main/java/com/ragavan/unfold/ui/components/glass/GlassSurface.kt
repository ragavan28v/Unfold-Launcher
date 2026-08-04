package com.ragavan.unfold.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ragavan.unfold.ui.design.glass

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    val shape = RoundedCornerShape(GlassStyle.Radius)

    Box(
        modifier = modifier
            .glass()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    GlassStyle.Stroke,
                    GlassStyle.InnerBorder,
                    shape
                )
        )

        content()

    }

}