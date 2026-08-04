package com.ragavan.unfold.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.ragavan.unfold.viewmodel.state.HomeState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.ragavan.unfold.engine.gesture.GestureState

@Composable
fun Workspace(

    state: HomeState

) {

    val workspaceState = remember {
        WorkspaceState()
    }

    val pagerState = rememberPagerState(
        initialPage = workspaceState.currentPage,
        pageCount = { 3 }
    )

    HorizontalPager(

        state = pagerState,

        modifier = Modifier

            .fillMaxSize()

    ) { page ->

        when (page) {

            0 -> {

                WorkspacePage(state)

            }

            1 -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Widgets",
                        color = Color.White,
                        fontSize = 28.sp
                    )

                }

            }

            2 -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Productivity",
                        color = Color.White,
                        fontSize = 28.sp
                    )

                }

            }

        }

    }

    LaunchedEffect(
        pagerState.currentPage
    ) {

        workspaceState.currentPage =
            pagerState.currentPage

    }

}