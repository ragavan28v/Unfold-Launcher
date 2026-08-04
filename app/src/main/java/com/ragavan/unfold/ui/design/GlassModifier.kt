package com.ragavan.unfold.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.glass() : Modifier {

    val shape = RoundedCornerShape(32.dp)

    return this
        .clip(shape)
        .background(
            Color.White.copy(alpha = 0.05f)
        )
        .border(
            1.dp,
            Color.White.copy(alpha = 0.10f),
            shape
        )

}