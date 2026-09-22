package com.kompakt.calendar.ui.mmd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD

/**
 * List-row measurements taken from the Kompakt system Settings screen
 * (docs/mmd/eink-design.md, "Settings (list screen)").
 */
object EinkRowTokens {
    val MinHeight: Dp = 64.dp
    val Inset: Dp = 16.dp
    val VerticalPadding: Dp = 8.dp
    val TrailingGap: Dp = 12.dp
    val Chevron: Dp = 32.dp
    val StatusGlyph: Dp = 28.dp
    val SectionTop: Dp = 24.dp
    val SectionBottom: Dp = 4.dp
}

/** A bold section heading above a group of rows. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    TextMMD(
        text = text,
        fontSize = EinkType.TitleSmall,
        fontWeight = FontWeight.Bold,
        color = EinkColors.Ink,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = EinkRowTokens.Inset,
                end = EinkRowTokens.Inset,
                top = EinkRowTokens.SectionTop,
                bottom = EinkRowTokens.SectionBottom,
            ),
    )
}

/**
 * One settings row: a 20sp label, an optional 18sp second line, and a trailing control.
 * The whole row is the touch target when [onClick] is set.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EinkRowTokens.MinHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = EinkRowTokens.Inset, vertical = EinkRowTokens.VerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(
                text = title,
                fontSize = EinkType.TitleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = EinkColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                TextMMD(
                    text = subtitle,
                    fontSize = EinkType.Body,
                    color = EinkColors.Ink,
                )
            }
        }
        Spacer(modifier = Modifier.width(EinkRowTokens.TrailingGap))
        trailing()
    }
}

/** The dotted hairline between two rows of one group. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    DashedDividerMMD(modifier = modifier.padding(horizontal = EinkRowTokens.Inset))
}

/** The thin trailing arrow of a row that opens another page. */
@Composable
fun RowChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = EinkColors.Ink,
        modifier = Modifier.size(EinkRowTokens.Chevron),
    )
}
