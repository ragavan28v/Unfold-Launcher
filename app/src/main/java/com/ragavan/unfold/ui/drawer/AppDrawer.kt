package com.ragavan.unfold.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ragavan.unfold.ui.components.icon.AppIcon
import com.ragavan.unfold.viewmodel.state.HomeState

@Composable
fun AppDrawer(

    state: HomeState,

    modifier: Modifier = Modifier

) {

    LazyVerticalGrid(

        columns = GridCells.Fixed(4),

        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF081019))
            .padding(top = 96.dp),

        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),

        horizontalArrangement = Arrangement.spacedBy(12.dp),

        verticalArrangement = Arrangement.spacedBy(20.dp)

    ) {

        items(state.installedApps) {

            AppIcon(it)

        }

    }

}