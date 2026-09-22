package com.kompakt.calendar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Moves the list forward by one screen. The first row that the screen cuts off
 * becomes the top row, so no row is skipped. If one row is taller than the
 * screen, the list moves by the screen height.
 */
suspend fun LazyListState.pageDown() {
    val info = layoutInfo
    val viewportEnd = info.viewportEndOffset
    val cut = info.visibleItemsInfo.firstOrNull { it.offset + it.size > viewportEnd }
    if (cut != null && cut.index > firstVisibleItemIndex) {
        scrollToItem(cut.index)
    } else {
        scrollBy((viewportEnd - info.viewportStartOffset).toFloat())
    }
}

/** Moves the list back by one screen. */
suspend fun LazyListState.pageUp() {
    val info = layoutInfo
    scrollBy(-(info.viewportEndOffset - info.viewportStartOffset).toFloat())
}

@Composable
fun Modifier.eInkVerticalScroll(
    state: LazyListState,
    scope: CoroutineScope,
    isScrollable: Boolean
): Modifier {
    var isDragging by remember { mutableStateOf(false) }
    // pointerInput keeps its first lambda, so read the latest value through this state.
    val scrollable by rememberUpdatedState(isScrollable)
    return this.pointerInput(state) {
        detectVerticalDragGestures(
            onDragEnd = { isDragging = false },
            onDragCancel = { isDragging = false }
        ) { _, dragAmount ->
            if (!isDragging && (scrollable || state.canScrollForward || state.canScrollBackward)) {
                isDragging = true
                scope.launch { if (dragAmount > 0) state.pageUp() else state.pageDown() }
            }
        }
    }
}

@Composable
fun EInkScrollbar(
    state: LazyListState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    val canScrollForward by remember { derivedStateOf { state.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { state.canScrollBackward } }
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(bottom = bottomInset)
            .width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                scope.launch { state.pageUp() }
            },
            modifier = Modifier.size(width = 40.dp, height = 48.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll up",
                modifier = Modifier.size(28.dp),
                tint = if (canScrollBackward) Color.Black else MaterialTheme.colorScheme.outline
            )
        }

        Canvas(
            modifier = Modifier
                .width(8.dp)
                .weight(1f)
                .border(0.5.dp, Color.Black, RoundedCornerShape(4.dp))
        ) {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@Canvas

            val totalItems = layoutInfo.totalItemsCount
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()

            val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            if (viewportSize <= 0) return@Canvas

            // Estimate total content height based on average item size
            val visibleItemsHeight = lastItem.offset + lastItem.size - firstItem.offset
            val averageItemSize = visibleItemsHeight.toFloat() / visibleItems.size
            val estimatedTotalSize = (averageItemSize * totalItems)
                .coerceAtLeast(viewportSize.toFloat())

            // If we can scroll, we want the thumb to be smaller than the track
            val isActuallyScrollable = state.canScrollForward || state.canScrollBackward
            val sliderFraction = if (isActuallyScrollable) {
                (viewportSize.toFloat() / estimatedTotalSize).coerceIn(0.1f, 0.9f)
            } else {
                1f
            }

            val sliderHeight = (size.height * sliderFraction).coerceAtLeast(16.dp.toPx())
            val maxOffset = size.height - sliderHeight

            // Smooth scroll fraction calculation
            val currentScrollOffset = state.firstVisibleItemIndex * averageItemSize + state.firstVisibleItemScrollOffset
            val maxScrollOffset = (estimatedTotalSize - viewportSize).coerceAtLeast(1f)
            val scrollFraction = (currentScrollOffset / maxScrollOffset).coerceIn(0f, 1f)

            val sliderTop = maxOffset * scrollFraction

            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(0f, sliderTop),
                size = Size(size.width, sliderHeight),
                cornerRadius = CornerRadius(size.width / 2, size.width / 2)
            )
        }

        IconButton(
            onClick = {
                scope.launch { state.pageDown() }
            },
            modifier = Modifier.size(width = 40.dp, height = 48.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll down",
                modifier = Modifier.size(28.dp),
                tint = if (canScrollForward) Color.Black else MaterialTheme.colorScheme.outline
            )
        }
    }
}
