package com.kompakt.calendar

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkRowTokens
import com.kompakt.calendar.ui.mmd.EinkTokens
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.HeaderTitle
import com.kompakt.calendar.ui.mmd.PanelPrimaryAction
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.mudita.mmd.components.text.TextMMD
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ChipShape = RoundedCornerShape(EinkTokens.ButtonCorner)
private val ChipBorder = 2.dp
private val CellHeight = 48.dp
private val CellGap = 4.dp

/** "Tue, 22 Sep 2026", or "Tue, Sep 22, 2026" with the American format. */
fun formatDraftDate(date: LocalDate, american: Boolean): String =
    date.format(DateTimeFormatter.ofPattern(if (american) "EEE, MMM d, yyyy" else "EEE, d MMM yyyy", Locale.getDefault()))

/** "3:00 PM", or "15:00" when the phone uses the 24-hour clock. */
fun formatDraftTime(time: LocalTime, is24Hour: Boolean): String =
    time.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault()))

/**
 * The "Starts" or "Ends" block of the event form: a bold label, then a date button and,
 * for timed events, a time button. Each button opens its own sheet.
 */
@Composable
fun DateTimeField(
    label: String,
    date: LocalDate,
    time: LocalTime,
    isAllDay: Boolean,
    useAmericanDateFormat: Boolean,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EinkRowTokens.Inset, vertical = 8.dp)
    ) {
        TextMMD(
            text = label,
            fontSize = EinkType.TitleSmall,
            fontWeight = FontWeight.Bold,
            color = EinkColors.Ink,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedChip(
                text = formatDraftDate(date, useAmericanDateFormat),
                onClick = onDateClick,
                modifier = Modifier.weight(1f),
            )
            if (!isAllDay) {
                OutlinedChip(
                    text = formatDraftTime(time, is24Hour),
                    onClick = onTimeClick,
                    modifier = Modifier.widthIn(min = 110.dp),
                )
            }
        }
    }
}

/** A 48dp outlined button with a bold label, used for the date and time values. */
@Composable
private fun OutlinedChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(CellHeight)
            .border(ChipBorder, EinkColors.Ink, ChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(text = text, fontSize = EinkType.Body, fontWeight = FontWeight.Bold, color = EinkColors.Ink, maxLines = 1)
    }
}

/** One grid cell: filled black when selected, outlined when marked, plain otherwise. */
@Composable
private fun GridCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    marked: Boolean = false,
    height: Dp = CellHeight,
    shape: Shape = ChipShape,
) {
    Box(
        modifier = modifier
            .height(height)
            .then(
                when {
                    selected -> Modifier.background(EinkColors.Ink, shape)
                    marked -> Modifier.border(ChipBorder, EinkColors.Ink, shape)
                    else -> Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(
            text = text,
            fontSize = EinkType.Body,
            fontWeight = if (selected || marked) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EinkColors.Paper else EinkColors.Ink,
        )
    }
}

@Composable
private fun SheetScaffold(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Surface(modifier = Modifier.fillMaxSize(), color = EinkColors.Paper) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(
                title = title,
                navigationIcon = { HeaderAction(Icons.Default.Close, "Close", onDismiss) },
            )
            content()
        }
    }
}

/**
 * A month grid. Tapping a day picks it and closes the sheet. The arrows change the month;
 * today is outlined and the picked day is filled.
 */
@Composable
fun DatePickerSheet(
    title: String,
    selected: LocalDate,
    startWeekOnMonday: Boolean,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var month by remember { mutableStateOf(YearMonth.from(selected)) }
    val today = remember { LocalDate.now() }
    val firstDay = if (startWeekOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val weekDays = remember(firstDay) { (0L..6L).map { firstDay.plus(it) } }

    SheetScaffold(title = title, onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderAction(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", { month = month.minusMonths(1) })
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                HeaderTitle("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}")
            }
            HeaderAction(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", { month = month.plusMonths(1) })
        }

        Column(modifier = Modifier.padding(horizontal = EinkRowTokens.Inset)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { day ->
                    TextMMD(
                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = EinkType.Small,
                        fontWeight = FontWeight.Bold,
                        color = EinkColors.Ink,
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            val lead = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
            val cells = lead + month.lengthOfMonth()
            val rows = (cells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = CellGap / 2)) {
                    for (col in 0 until 7) {
                        val dayNumber = row * 7 + col - lead + 1
                        if (dayNumber in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNumber)
                            GridCell(
                                text = dayNumber.toString(),
                                selected = date == selected,
                                marked = date == today,
                                onClick = { onPick(date) },
                                modifier = Modifier.weight(1f).padding(horizontal = CellGap / 2),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).height(CellHeight))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(EinkRowTokens.Inset),
        ) {
            PanelPrimaryAction(
                label = "Today",
                onClick = { onPick(today) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Hour and minute grids, no keyboard. Taps change the value at the top; Done saves it
 * and Close leaves the time as it was.
 */
@Composable
fun TimePickerSheet(
    title: String,
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    var time by remember { mutableStateOf(initial) }
    val isPm = time.hour >= 12

    SheetScaffold(title = title, onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = EinkRowTokens.Inset, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextMMD(
                text = if (is24Hour) formatDraftTime(time, true)
                    else String.format(Locale.US, "%d:%02d", (time.hour + 11) % 12 + 1, time.minute),
                fontSize = EinkType.Headline,
                fontWeight = FontWeight.Bold,
                color = EinkColors.Ink,
                modifier = Modifier.weight(1f),
            )
            if (!is24Hour) {
                GridCell(
                    text = "AM",
                    selected = !isPm,
                    marked = isPm,
                    onClick = { if (isPm) time = time.minusHours(12) },
                    modifier = Modifier.width(64.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                GridCell(
                    text = "PM",
                    selected = isPm,
                    marked = !isPm,
                    onClick = { if (!isPm) time = time.plusHours(12) },
                    modifier = Modifier.width(64.dp),
                )
            }
        }

        SheetLabel("Hour")
        val hours: List<Int> = if (is24Hour) (0..23).toList() else listOf(12) + (1..11).toList()
        CellGrid(items = hours, columns = 6) { hour, modifier ->
            val hour24 = if (is24Hour) hour else (hour % 12) + if (isPm) 12 else 0
            GridCell(
                text = hour.toString(),
                selected = time.hour == hour24,
                onClick = { time = time.withHour(hour24) },
                modifier = modifier,
            )
        }

        SheetLabel("Minute")
        CellGrid(items = (0..55 step 5).toList(), columns = 6) { minute, modifier ->
            GridCell(
                text = String.format(Locale.US, ":%02d", minute),
                selected = time.minute == minute,
                onClick = { time = time.withMinute(minute) },
                modifier = modifier,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth().padding(EinkRowTokens.Inset)) {
            PanelPrimaryAction(label = "Done", onClick = { onPick(time) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    TextMMD(
        text = text,
        fontSize = EinkType.TitleSmall,
        fontWeight = FontWeight.Bold,
        color = EinkColors.Ink,
        modifier = Modifier.padding(start = EinkRowTokens.Inset, end = EinkRowTokens.Inset, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun <T> CellGrid(
    items: List<T>,
    columns: Int,
    cell: @Composable (T, Modifier) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = EinkRowTokens.Inset - CellGap / 2)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = CellGap / 2)) {
                rowItems.forEach { item -> cell(item, Modifier.weight(1f).padding(horizontal = CellGap / 2)) }
                repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
