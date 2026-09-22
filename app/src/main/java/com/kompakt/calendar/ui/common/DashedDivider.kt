package com.kompakt.calendar.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.kompakt.calendar.ui.mmd.DashedDividerMMD
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkTokens

/**
 * The dotted row divider. Kept for the existing call sites; it draws [DashedDividerMMD], the
 * calibrated one-pixel black dotted line. The old thickness and dash parameters are accepted
 * and ignored, so every row divider in the app looks the same.
 */
@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") color: Color = EinkColors.Ink,
    @Suppress("UNUSED_PARAMETER") thickness: Dp = EinkTokens.DividerDot,
    @Suppress("UNUSED_PARAMETER") dashWidth: Dp = EinkTokens.DividerDot,
    @Suppress("UNUSED_PARAMETER") dashGap: Dp = EinkTokens.DividerGap,
) {
    DashedDividerMMD(modifier = modifier)
}
