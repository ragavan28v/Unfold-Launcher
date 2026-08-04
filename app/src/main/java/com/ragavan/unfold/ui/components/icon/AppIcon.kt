package com.ragavan.unfold.ui.components.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ragavan.unfold.data.apps.AppInfo
import com.ragavan.unfold.utils.AppLauncher
import androidx.compose.ui.graphics.Color

@Composable
fun AppIcon(
    app: AppInfo
) {

    val context = LocalContext.current

    Column(

        modifier = Modifier
            .height(92.dp)
            .clickable {

                AppLauncher.launch(
                    context,
                    app.packageName
                )

            },

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.spacedBy(6.dp)

    ) {

        Image(

            bitmap = app.icon
                .toBitmap(96,96)
                .asImageBitmap(),

            contentDescription = app.name,

            modifier = Modifier.size(56.dp)

        )

        Text(

            text = app.name,

            color = Color.White,

            maxLines = 2,

            overflow = TextOverflow.Ellipsis

        )

    }

}