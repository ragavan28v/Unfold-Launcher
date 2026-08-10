package com.unfold.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfold.core.ui.theme.LocalUnfoldTheme
import com.unfold.core.ui.util.drawCarvedBevel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarvedIcon(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    icon: @Composable () -> Unit,
    isPressed: Boolean = false,
    accentTint: Color = LocalUnfoldTheme.current.accentPrimary,
    bevelIntensity: Float = LocalUnfoldTheme.current.bevelIntensity,
    badgeCount: Int? = null,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    contentDescription: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedState = interactionSource.collectIsPressedAsState()
    val activePressed = isPressed || pressedState.value

    val theme = LocalUnfoldTheme.current

    Box(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick?.invoke() },
                onLongClick = { onLongPress?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Draw the neumorphic background and bevel
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.toPx() / 2f
            // 1. Draw flat base
            drawCircle(color = Color.Transparent, radius = radius)
            // 2 & 3. Inner shadow and highlight & 4. 1px rim stroke
            drawCarvedBevel(
                radius = radius,
                intensity = bevelIntensity,
                isInverted = activePressed,
                accentColor = accentTint
            )
        }

        // 5. App icon / foreground glyph centered, slightly inset (~14% padding)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding((size.value * 0.14f).dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        // Notification badge
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = theme.accentWarn)
                    }
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}


