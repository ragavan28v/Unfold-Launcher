package com.volt.core.ui.components

import androidx.compose.foundation.Canvas
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
            .width(72.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Draw the vertical connector line in background
        Canvas(modifier = Modifier.fillMaxHeight()) {
            val startY = 40.dp.toPx()
            val endY = size.height - 40.dp.toPx()
            val x = size.width / 2f
            drawLine(
                color = theme.panelBorder,
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = 2.dp.toPx()
            )
        }

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            nodes.forEach { node ->
                val isActive = node.id == activeNodeId
                val nodeColor = if (isActive) theme.accentPrimary else theme.textSecondary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNodeSelected(node.id) }
                        .padding(vertical = 12.dp)
                ) {
                    CarvedIcon(
                        size = 48.dp,
                        accentTint = if (isActive) theme.accentPrimary else theme.panelBorder,
                        contentDescription = node.label,
                        isPressed = isActive,
                        icon = {
                            Icon(
                                imageVector = node.icon,
                                contentDescription = null,
                                tint = nodeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    Text(
                        text = node.label.uppercase(),
                        color = nodeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
