package com.unfold.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unfold.core.ui.theme.LocalUnfoldTheme
import com.unfold.core.ui.util.drawCarvedBevel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarvedIcon(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    icon: @Composable () -> Unit,
    raw: Boolean = false,
    isPressed: Boolean = false,
    accentTint: Color = LocalUnfoldTheme.current.accentPrimary,
    bevelIntensity: Float = LocalUnfoldTheme.current.bevelIntensity,
    badgeCount: Int? = null,
    badgeColor: Color = Color(0xFFF44336),
    showBadgeCount: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    contentDescription: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedState = interactionSource.collectIsPressedAsState()
    val activePressed = isPressed || pressedState.value

    Box(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick?.invoke() },
                onLongClick = { onLongPress?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!raw) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.toPx() / 2f
                drawCircle(color = Color.Transparent, radius = radius)
                drawCarvedBevel(
                    radius = radius,
                    intensity = bevelIntensity,
                    isInverted = activePressed,
                    accentColor = accentTint
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { baseModifier ->
                    if (raw) baseModifier else baseModifier
                        .padding((size.value * 0.14f).dp)
                        .clip(CircleShape)
                },
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        val badgeVisible = badgeCount != null && badgeCount > 0
        val badgeAlpha by animateFloatAsState(
            targetValue = if (badgeVisible) 1f else 0f,
            label = "badge visibility"
        )
        val animatedBadgeColor by animateColorAsState(
            targetValue = badgeColor,
            label = "badge color"
        )

        if (badgeVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
                    .alpha(badgeAlpha)
                    .zIndex(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (showBadgeCount) 20.dp else 14.dp)
                        .clip(CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = animatedBadgeColor)
                    }
                    if (showBadgeCount) {
                        Text(
                            text = if (badgeCount!! > 99) "99+" else badgeCount.toString(),
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
}


