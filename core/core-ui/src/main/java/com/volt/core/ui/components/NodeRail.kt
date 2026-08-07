package com.volt.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volt.core.ui.theme.LocalVoltTheme

data class RailNode(val id: String, val icon: ImageVector, val label: String)

@Composable
fun NodeRail(
    modifier: Modifier = Modifier,
    nodes: List<RailNode>,
    activeNodeId: String,
    onNodeSelected: (String) -> Unit
) {
    val theme = LocalVoltTheme.current

    Box(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxHeight()) {
            val startY = 24.dp.toPx()
            val endY = size.height - 24.dp.toPx()
            val x = size.width / 2f

            drawLine(
                color = theme.panelBorder.copy(alpha = 0.38f),
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = 2.dp.toPx()
            )

            nodes.forEachIndexed { index, _ ->
                val nodeY = startY + (endY - startY) * index / maxOf(1, nodes.size - 1)
                drawCircle(
                    color = theme.panelBorder.copy(alpha = 0.3f),
                    radius = 3.dp.toPx(),
                    center = Offset(x, nodeY)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            nodes.forEach { node ->
                val isActive = node.id == activeNodeId
                val nodeColor = if (isActive) theme.accentPrimary else theme.textSecondary
                val outerSize = if (isActive) 68.dp else 54.dp
                val innerSize = if (isActive) 52.dp else 42.dp
                val iconSize = if (isActive) 24.dp else 18.dp

                Box(
                    modifier = Modifier
                        .size(outerSize)
                        .clickable { onNodeSelected(node.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(outerSize)
                                .background(
                                    color = theme.accentPrimary.copy(alpha = 0.14f),
                                    shape = CircleShape
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(innerSize)
                            .background(
                                color = if (isActive) theme.accentPrimary.copy(alpha = 0.18f) else theme.bgPanel,
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = if (isActive) theme.accentPrimary else theme.panelBorder,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = node.icon,
                            contentDescription = node.label,
                            tint = nodeColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }
}
