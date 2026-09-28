package com.roleta.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val FULL_ROTATIONS = 6
private const val ANIMATION_DURATION_MS = 2400
private const val FADE_FRACTION = 0.28f

@Composable
fun SlotMachineAnimation(
    items: List<String>,
    targetIndex: Int,
    spinId: Long,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val itemHeightPx = with(density) { 64.dp.toPx() }
    val highlightInsetPx = with(density) { 24.dp.toPx() }
    val highlightRadiusPx = with(density) { 20.dp.toPx() }
    val textInsetPx = with(density) { 40.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val highlightTextColor = MaterialTheme.colorScheme.onPrimaryContainer
    val dimmedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val startOffset = (FULL_ROTATIONS * items.size).toFloat()
    val endOffset = startOffset + FULL_ROTATIONS * items.size + targetIndex

    val offset = remember { Animatable(startOffset) }

    LaunchedEffect(spinId) {
        offset.snapTo(startOffset)
        offset.animateTo(
            targetValue = endOffset,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        )
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onSettled()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val currentOffset = offset.value
        val centerY = size.height / 2f

        // Center highlight
        drawRoundRect(
            color = highlightColor,
            topLeft = Offset(highlightInsetPx, centerY - itemHeightPx / 2f),
            size = Size(size.width - highlightInsetPx * 2, itemHeightPx),
            cornerRadius = CornerRadius(highlightRadiusPx)
        )

        // Items
        val halfVisible = (size.height / itemHeightPx / 2).toInt() + 2
        val baseIdx = currentOffset.toInt()
        for (di in -halfVisible..(halfVisible + 1)) {
            val pos = baseIdx + di
            val itemIdx = ((pos % items.size) + items.size) % items.size
            val yCenter = centerY + (pos - currentOffset) * itemHeightPx

            if (yCenter < -itemHeightPx || yCenter > size.height + itemHeightPx) continue

            val isCenter = yCenter >= centerY - itemHeightPx / 2f &&
                    yCenter < centerY + itemHeightPx / 2f
            drawItemText(
                text = items[itemIdx],
                yCenter = yCenter,
                horizontalInset = textInsetPx,
                textMeasurer = textMeasurer,
                style = if (isCenter) {
                    typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, color = highlightTextColor)
                } else {
                    typography.titleMedium.copy(fontSize = 18.sp, color = dimmedTextColor)
                }
            )
        }

        // Edge fade — top
        val fadeHeight = size.height * FADE_FRACTION
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(surfaceColor, Color.Transparent),
                startY = 0f,
                endY = fadeHeight
            ),
            size = Size(size.width, fadeHeight)
        )

        // Edge fade — bottom
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, surfaceColor),
                startY = size.height - fadeHeight,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height - fadeHeight),
            size = Size(size.width, fadeHeight)
        )
    }
}

private fun DrawScope.drawItemText(
    text: String,
    yCenter: Float,
    horizontalInset: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
) {
    // Long items are cut off with an ellipsis instead of running past the highlight.
    val maxWidth = (size.width - horizontalInset * 2).toInt().coerceAtLeast(0)
    val measured = textMeasurer.measure(
        text = text,
        style = style,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth)
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            x = (size.width - measured.size.width) / 2f,
            y = yCenter - measured.size.height / 2f
        )
    )
}
