package com.ragavan.unfold.ui.layers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusLayer() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 48.dp,
                start = 24.dp,
                end = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = "Gesture Ready",
            color = Color.White,
            fontSize = 32.sp
        )

        Text(
            text = "Monday",
            color = Color.Gray
        )

    }

}