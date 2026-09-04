package com.iumrah.beta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iumrah.beta.core.design.IumrahMotion

@Composable
fun IumrahPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = IumrahMotion.CardPressedScale,
    cornerRadius: Dp = 30.dp,
    background: Color = MaterialTheme.colorScheme.surface,
    pressedBackgroundAlpha: Float = 1f,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = IumrahMotion.press,
        label = "iumrah-press-scale",
    )
    val animatedBackground by animateColorAsState(
        targetValue = if (pressed && enabled) background.copy(alpha = background.alpha * pressedBackgroundAlpha) else background,
        animationSpec = IumrahMotion.fastColor,
        label = "iumrah-press-background",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, RoundedCornerShape(cornerRadius), clip = false) else Modifier)
            .clip(RoundedCornerShape(cornerRadius))
            .background(animatedBackground)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        content()
    }
}

@Composable
fun IumrahPrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IumrahPressable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        pressedScale = IumrahMotion.PressedScale,
        cornerRadius = 28.dp,
        background = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        pressedBackgroundAlpha = .82f,
        shadowElevation = if (enabled) 7.dp else 0.dp,
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun IumrahSecondaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IumrahPressable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        pressedScale = IumrahMotion.PressedScale,
        cornerRadius = 27.dp,
        background = MaterialTheme.colorScheme.surfaceVariant,
        pressedBackgroundAlpha = .72f,
    ) {
        Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun IumrahSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun IumrahPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceVariant,
    foreground: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = foreground, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun IumrahDot(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val width by animateFloatAsState(
        targetValue = if (selected) 22f else 7f,
        animationSpec = IumrahMotion.selection,
        label = "indicator-width",
    )
    Box(
        modifier = modifier
            .size(width = width.dp, height = 7.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.20f),
            ),
    )
}
