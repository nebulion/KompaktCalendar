// SPDX-License-Identifier: GPL-3.0-or-later

package com.kompakt.calendar.ui.mmd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.lazy.LazyDefaultsMMD

/**
 * A list that pages instead of scrolling, with MMD's own scrollbar: an 8dp track and 24dp
 * triangle arrows in a 40dp column, filled when they can be used and dotted when not.
 * Ported from the AnkiDroid MMD fork (`com.ichi2.compose.mmd.PagedList`).
 *
 * How `LazyColumnMMD` behaves (read from its bytecode):
 * - A swipe or an arrow tap moves **by item**, [scrollStep] items at a time. Emit one item per
 *   row, and put a row's divider inside the row's item, so a page is a steady number of rows.
 * - The list is drawn at `alpha = 0` until its items are counted, so it appears in one paint.
 * - MMD composes its scrollbar only while the list can scroll. With [reserveScrollbar], an
 *   inactive one is drawn in its place, so the rows never shift sideways when data arrives
 *   and the list grows past one page.
 */
@Composable
fun PagedList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollStep: Int = PagedListDefaults.SCROLL_STEP,
    reserveScrollbar: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    content: LazyListScope.() -> Unit,
) {
    val canScroll by remember(state) { derivedStateOf { state.canScrollForward || state.canScrollBackward } }
    Row(modifier) {
        LazyColumnMMD(
            modifier = Modifier.weight(1f),
            state = state,
            contentPadding = contentPadding,
            scrollStep = scrollStep,
            isScrollbarVisible = true,
            content = content,
        )
        if (reserveScrollbar && !canScroll) {
            InactiveScrollbar()
        }
    }
}

/**
 * MMD's scrollbar with nothing to scroll: the library's track and its dotted arrows, the form
 * MMD uses for a control that cannot be used now.
 */
@Composable
private fun InactiveScrollbar() {
    Column(
        Modifier.fillMaxHeight().padding(horizontal = PagedListDefaults.ScrollbarSidePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScrollArrow(pointingUp = true)
        Box(
            Modifier
                .width(PagedListDefaults.ScrollbarTrackWidth)
                .weight(1f)
                .background(EinkColors.Paper, LazyDefaultsMMD.sliderBackgroundCorners)
                .border(
                    width = LazyDefaultsMMD.sliderBackgroundBorderWidth,
                    color = EinkColors.Ink,
                    shape = LazyDefaultsMMD.sliderBackgroundCorners,
                ),
        )
        ScrollArrow(pointingUp = false)
    }
}

@Composable
private fun ScrollArrow(pointingUp: Boolean) {
    Icon(
        painter = painterResource(
            if (pointingUp) com.mudita.mmd.R.drawable.chevron_dotted_up
            else com.mudita.mmd.R.drawable.chevron_dotted_down
        ),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier
            .padding(vertical = PagedListDefaults.ScrollbarArrowPadding)
            .size(PagedListDefaults.ScrollbarArrowSize),
    )
}

/**
 * Space a list leaves at the bottom of a screen with a floating + button: the 56dp button,
 * its 16dp margin, and an 8dp gap. The list and its scrollbar end above the button.
 */
val FabClearance = 80.dp

object PagedListDefaults {
    /** Rows per page turn. MMD's default of 4 felt slow in the Loop and AnkiDroid forks; 6 did not. */
    const val SCROLL_STEP = 6

    /** MMD's scrollbar column: 24dp arrows with 8dp either side. Rows end this far from the edge. */
    val ScrollbarGutter = 40.dp

    val ScrollbarSidePadding = 8.dp
    val ScrollbarArrowPadding = 16.dp

    /** `LazyDefaultsMMD.sliderBackgroundWidth`, which MMD keeps internal. */
    val ScrollbarTrackWidth = 8.dp

    /** `LazyDefaultsMMD.navigateIconSize`, which MMD keeps internal. */
    val ScrollbarArrowSize = 24.dp
}
