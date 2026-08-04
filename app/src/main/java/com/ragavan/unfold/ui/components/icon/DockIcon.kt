package com.ragavan.unfold.ui.components.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ragavan.unfold.data.apps.AppInfo
import com.ragavan.unfold.utils.AppLauncher

@Composable
fun DockIcon(
    app: AppInfo
) {

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(60.dp)
            .clickable {

                AppLauncher.launch(
                    context,
                    app.packageName
                )

            },

        contentAlignment = Alignment.Center

    ) {

        Image(
            bitmap = app.icon
                .toBitmap(96,96)
                .asImageBitmap(),

            contentDescription = app.name,

            modifier = Modifier.size(42.dp)

        )

    }

}