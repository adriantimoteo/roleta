package com.roleta.app.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val OuterRadius = 20.dp
private val InnerRadius = 4.dp

/**
 * Shape for a row inside a vertically grouped block: the block's outer corners are
 * rounded and the joins between rows are nearly square.
 */
fun groupedItemShape(index: Int, count: Int): Shape = when {
    count == 1 -> RoundedCornerShape(OuterRadius)
    index == 0 -> RoundedCornerShape(
        topStart = OuterRadius, topEnd = OuterRadius,
        bottomStart = InnerRadius, bottomEnd = InnerRadius
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = InnerRadius, topEnd = InnerRadius,
        bottomStart = OuterRadius, bottomEnd = OuterRadius
    )
    else -> RoundedCornerShape(InnerRadius)
}
