// SPDX-License-Identifier: GPL-3.0-or-later
// Ported from the AnkiDroid MMD fork (com.ichi2.compose.mmd), GPL-3.0-or-later.
// Rules: docs/mmd/eink-design.md in that project (black and white only, the MMD
// type scale and nothing below 14sp, bold title over a 3dp rule, 48dp targets,
// dotted row dividers, bottom panels, nothing animates).

package com.kompakt.calendar.ui.mmd

import android.view.Gravity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The MMD type scale (TypographyMMD). Every text size in the app comes from here; nothing is
 * below 14sp.
 */
object EinkType {
    val Headline = 28.sp // headlineLarge
    val Title = 24.sp // titleLarge: screen titles, panel titles
    val TitleMedium = 20.sp // titleMedium / bodyLarge: row labels
    val Body = 18.sp // bodyMedium / labelLarge: secondary lines, buttons
    val TitleSmall = 16.sp // titleSmall: section headings
    val Small = 15.sp // bodySmall / labelMedium
    val Label = 14.sp // labelSmall: the smallest text allowed
}

/** Black and white only. Emphasis is inversion or weight, never grey. */
object EinkColors {
    val Ink = Color.Black
    val Paper = Color.White
}

/** Sizes measured on the Kompakt's own apps (see eink-design.md, Calibration → KompaktSystem). */
object EinkTokens {
    val HeaderTouchTarget: Dp = 48.dp
    val HeaderGlyph: Dp = 28.dp
    val HeaderRule: Dp = 3.dp
    val DividerDot: Dp = 2.dp
    val DividerGap: Dp = 2.dp
    val ButtonHeight: Dp = 56.dp
    val ButtonCorner: Dp = 9.dp
    val PanelRuleGap: Dp = 2.dp
}

/**
 * A screen header: bold title, actions always visible, then the heavy 3dp rule.
 * `TopAppBarMMD(showDivider = true)` draws only 1dp, so the rule is drawn here.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.background(EinkColors.Paper)) {
        TopAppBarMMD(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = WindowInsets(0, 0, 0, 0),
            showDivider = false,
        )
        HorizontalDividerMMD(thickness = EinkTokens.HeaderRule, color = EinkColors.Ink)
    }
}

/** [ScreenHeader] with a plain bold title at the title size. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    ScreenHeader(
        title = { HeaderTitle(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
    )
}

/** The bold one-line header title. */
@Composable
fun HeaderTitle(text: String, modifier: Modifier = Modifier) {
    TextMMD(
        text = text,
        fontSize = EinkType.Title,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A header icon: a 48dp touch target around a 28dp black glyph. */
@Composable
fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier.size(EinkTokens.HeaderTouchTarget), enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(EinkTokens.HeaderGlyph),
            tint = EinkColors.Ink,
        )
    }
}

/**
 * The dotted hairline between list rows: one device pixel tall, 2dp dots and gaps snapped to
 * whole pixels, so the panel never gets an anti-aliased grey edge.
 */
@Composable
fun DashedDividerMMD(
    modifier: Modifier = Modifier,
    dash: Dp = EinkTokens.DividerDot,
    gap: Dp = EinkTokens.DividerGap,
    color: Color = EinkColors.Ink,
) {
    val onePixel = with(LocalDensity.current) { (1 / density).dp }
    Canvas(modifier.fillMaxWidth().height(onePixel)) {
        val dashPx = dash.toPx().roundToInt().coerceAtLeast(1).toFloat()
        val gapPx = gap.toPx().roundToInt().coerceAtLeast(1).toFloat()
        val y = floor(size.height / 2) + 0.5f
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
            cap = StrokeCap.Butt,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx)),
        )
    }
}

/** A bottom-anchored panel: 3dp black rule and 2dp white across the top, content centred. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().background(EinkColors.Paper)) {
        HorizontalDividerMMD(thickness = EinkTokens.HeaderRule, color = EinkColors.Ink)
        Spacer(Modifier.height(EinkTokens.PanelRuleGap))
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

/**
 * Shows [Panel] at the bottom of the screen with no grey dim and no window animation: the
 * default dim is a flat grey the panel cannot render and which ghosts on repaint.
 */
@Composable
fun PanelDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(0f)
            window?.setWindowAnimations(0)
            window?.setGravity(Gravity.BOTTOM)
        }
        Panel(content = content)
    }
}

/** Pattern P5: a confirmation. The committing action is solid, the escape outlined. */
@Composable
fun ConfirmPanel(
    title: String,
    body: String?,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PanelDialog(onDismissRequest = onDismiss) {
        PanelTitle(title)
        if (body != null) PanelBody(body)
        PanelActions {
            PanelSecondaryAction(label = dismissLabel, onClick = onDismiss)
            PanelPrimaryAction(label = confirmLabel, onClick = onConfirm)
        }
    }
}

@Composable
fun PanelTitle(text: String) {
    TextMMD(
        text = text,
        fontSize = EinkType.Title,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    )
}

@Composable
fun PanelBody(text: String) {
    TextMMD(
        text = text,
        fontSize = EinkType.TitleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * A panel's buttons, stacked full width 12dp apart below 24dp of space. Write the escape first
 * and the committing action last; the last is placed on top (solid above outlined).
 */
@Composable
fun PanelActions(content: @Composable () -> Unit) {
    val gap = with(LocalDensity.current) { 12.dp.roundToPx() }
    Layout(content = content, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { measurables, constraints ->
        val width = constraints.maxWidth
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(width)) }.reversed()
        val height = placeables.sumOf { it.height } + gap * (placeables.size - 1).coerceAtLeast(0)
        layout(width, height) {
            var y = 0
            placeables.forEach {
                it.placeRelative(0, y)
                y += it.height + gap
            }
        }
    }
}

@Composable
fun PanelPrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    ButtonMMD(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = EinkTokens.ButtonHeight),
        shape = RoundedCornerShape(EinkTokens.ButtonCorner),
    ) { TextMMD(text = label, fontSize = EinkType.TitleMedium, fontWeight = FontWeight.Bold) }
}

@Composable
fun PanelSecondaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButtonMMD(
        onClick = onClick,
        modifier = modifier.heightIn(min = EinkTokens.ButtonHeight),
        shape = RoundedCornerShape(EinkTokens.ButtonCorner),
    ) { TextMMD(text = label, fontSize = EinkType.TitleMedium, fontWeight = FontWeight.Bold) }
}

/**
 * A page that is still loading: blank at first (most loads finish within a moment, and a label
 * shown only to be replaced is two repaints), then a static bold label. Never a spinner.
 */
@Composable
fun PageLoading(modifier: Modifier = Modifier, label: String = "Loading", delayMillis: Long = 500) {
    val shown by produceState(false) {
        delay(delayMillis)
        value = true
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (shown) {
            TextMMD(text = label, fontSize = EinkType.TitleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** A short static message in place of a Toast (a Toast fades in and out). */
@Composable
fun InlineMessage(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
        TextMMD(text = text, fontSize = EinkType.Body, fontWeight = FontWeight.Bold, color = EinkColors.Ink)
    }
}
