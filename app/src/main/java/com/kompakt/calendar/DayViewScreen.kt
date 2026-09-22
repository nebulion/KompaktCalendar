package com.kompakt.calendar

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kompakt.calendar.calendar.CalendarEvent
import com.kompakt.calendar.ui.mmd.DashedDividerMMD
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val agendaPage by viewModel.agendaPage.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val events by viewModel.dayEvents.collectAsState()
    val useAmericanDateFormat by viewModel.useAmericanDateFormat.collectAsState()

    var eventColumnOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedDate) { eventColumnOffset = 0 }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.refreshPermission() }

    Scaffold(
        topBar = {
            // Short month ("22 Sep") so the 24sp date fits between 48dp arrows
            // and three 48dp actions on the 360dp screen.
            ScreenHeader(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeaderAction(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Day", { viewModel.previousDay() })
                        Column {
                            TextMMD(
                                text = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                fontSize = EinkType.Label,
                                lineHeight = 16.sp
                            )
                            val month = selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            val dateText = if (useAmericanDateFormat) {
                                "$month ${selectedDate.dayOfMonth}"
                            } else {
                                "${selectedDate.dayOfMonth} $month"
                            }
                            TextMMD(
                                text = dateText,
                                fontSize = EinkType.Title,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp,
                                maxLines = 1
                            )
                        }
                        HeaderAction(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Day", { viewModel.nextDay() })
                    }
                },
                actions = {
                    HeaderAction(Icons.Outlined.CalendarMonth, "Calendar", { navController.navigate("calendar") })
                    HeaderAction(Icons.Default.Search, "Search", { navController.navigate("event_search") })
                    HeaderAction(Icons.Outlined.Settings, "Settings", { navController.navigate("settings") })
                }
            )
        },
        floatingActionButton = {
            FloatingActionButtonMMD(
                onClick = {
                    viewModel.beginNewEvent()
                    navController.navigate("add_event?fromCalendar=false")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CalendarPermissionGate(
                hasPermission = hasPermission,
                onPermissionGranted = { viewModel.refreshPermission() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(agendaPage) {
                            var totalDrag = 0f
                            detectVerticalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                },
                                onDragEnd = {
                                    if (totalDrag > 50) {
                                        if (agendaPage > 0) viewModel.setAgendaPage(agendaPage - 1)
                                    } else if (totalDrag < -50) {
                                        if (agendaPage < 2) viewModel.setAgendaPage(agendaPage + 1)
                                    }
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val allDay = events.filter { it.allDay }
                        if (allDay.isNotEmpty()) {
                            AllDayBar(allDay) { ev ->
                                val time = ev.start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                navController.navigate("event_detail/${ev.id}?instanceTime=$time")
                            }
                        }

                        val hours = getHoursForPage(agendaPage)
                        val timedEvents = events.filter { !it.allDay }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val slotHeight = this.maxHeight / 12

                            Column(modifier = Modifier.fillMaxSize()) {
                                hours.forEach { hour ->
                                    TimeSlotLabel(hour, slotHeight)
                                }
                            }

                            TimeGridOverlay(
                                hours = hours,
                                slotHeight = slotHeight,
                                date = selectedDate,
                                events = timedEvents,
                                columnOffset = eventColumnOffset,
                                onColumnOffsetChange = { eventColumnOffset = it },
                                onEventClick = { ev ->
                                    val time = ev.start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    navController.navigate("event_detail/${ev.id}?instanceTime=$time")
                                },
                                onEventMoved = { ev, newTime ->
                                    val newStart = selectedDate.atTime(newTime)
                                    scope.launch {
                                        viewModel.moveEvent(ev, newStart)
                                    }
                                },
                                onLongPress = { time ->
                                    viewModel.beginNewEvent(date = selectedDate, time = time)
                                    navController.navigate("add_event?fromCalendar=false")
                                }
                            )
                        }
                    }

                    DayViewScrollIndicator(
                        currentPage = agendaPage,
                        onPageSelected = { viewModel.setAgendaPage(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDayBar(events: List<CalendarEvent>, onEventClick: (CalendarEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        TextMMD("All-day", fontSize = EinkType.Label, fontWeight = FontWeight.Bold)
        events.forEach { ev ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .border(1.dp, EinkColors.Ink, RoundedCornerShape(4.dp))
                    .clickable { onEventClick(ev) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                TextMMD(ev.title, fontSize = EinkType.TitleSmall, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        DashedDividerMMD(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun TimeSlotLabel(hour: Int, height: Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    val displayHour = if (is24Hour) {
        String.format(Locale.US, "%02d:00", hour)
    } else {
        when {
            hour == 0 -> "12 am"
            hour < 12 -> "$hour am"
            hour == 12 -> "12 pm"
            else -> "${hour - 12} pm"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(start = 8.dp)
    ) {
        TextMMD(
            text = displayHour,
            fontSize = EinkType.Label,
            modifier = Modifier
                .width(52.dp)
                .align(Alignment.TopStart)
                .padding(top = 4.dp)
        )

        // The calibrated black dotted hairline, not a grey half-dp dash.
        DashedDividerMMD(
            modifier = Modifier
                .padding(start = 48.dp, end = 8.dp)
                .align(Alignment.TopStart)
                .offset(y = 14.dp)
        )
    }
}

@Composable
fun TimeGridOverlay(
    hours: List<Int>,
    slotHeight: Dp,
    date: LocalDate,
    events: List<CalendarEvent>,
    columnOffset: Int,
    onColumnOffsetChange: (Int) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onEventMoved: (CalendarEvent, LocalTime) -> Unit,
    onLongPress: (LocalTime) -> Unit
) {
    if (hours.isEmpty()) return
    val startHour = hours.first()
    val endHour = hours.last()
    val rangeStart = LocalTime.of(startHour, 0)
    val rangeEnd = if (endHour == 23) LocalTime.MAX else LocalTime.of(endHour + 1, 0)

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    // The now-line moves once a minute: each move is an E Ink repaint.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            currentTime = LocalTime.now()
        }
    }

    val visibleEvents = events.filter { ev ->
        val s = if (ev.start.toLocalDate().isBefore(date)) LocalTime.MIN else ev.start.toLocalTime()
        val e = if (ev.end.toLocalDate().isAfter(date)) LocalTime.MAX else ev.end.toLocalTime()
        s < rangeEnd && e > rangeStart
    }

    val groups = mutableListOf<MutableList<CalendarEvent>>()
    visibleEvents.sortedBy { it.start }.forEach { ev ->
        var added = false
        for (group in groups) {
            if (group.any { overlaps(it, ev, date) }) {
                group.add(ev)
                added = true
                break
            }
        }
        if (!added) groups.add(mutableListOf(ev))
    }

    // Max simultaneous overlap across all groups this page
    val maxGroupSize = groups.maxOfOrNull { it.size } ?: 0
    val hasOverflow = maxGroupSize > 2
    // positions: 0 .. (maxGroupSize - 2), each showing columns [pos, pos+1]
    val maxColumnOffset = (maxGroupSize - 2).coerceAtLeast(0)

    var isDraggingH by remember { mutableStateOf(false) }
    val latestColumnOffset by rememberUpdatedState(columnOffset)
    val latestMaxColumnOffset by rememberUpdatedState(maxColumnOffset)
    val latestHasOverflow by rememberUpdatedState(hasOverflow)
    val latestOnChange by rememberUpdatedState(onColumnOffsetChange)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 56.dp, end = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val minutes = ((offset.y - 14.dp.toPx()) / slotHeight.toPx()) * 60
                        val snappedMinutes = ((minutes + 7.5f) / 15).toInt() * 15
                        val time = rangeStart.plusMinutes(snappedMinutes.toLong())
                        onLongPress(time)
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { isDraggingH = false },
                    onDragCancel = { isDraggingH = false }
                ) { _, dragAmount ->
                    if (!isDraggingH && latestHasOverflow) {
                        isDraggingH = true
                        // swipe left (dragAmount < 0) → show columns further right
                        val newOffset = if (dragAmount < 0) {
                            (latestColumnOffset + 1).coerceAtMost(latestMaxColumnOffset)
                        } else {
                            (latestColumnOffset - 1).coerceAtLeast(0)
                        }
                        if (newOffset != latestColumnOffset) latestOnChange(newOffset)
                    }
                }
            }
    ) {
        val totalWidth = this.maxWidth

        groups.forEach { group ->
            val count = group.size
            group.forEachIndexed { index, ev ->
                val itemWidth: Dp
                val leftOffset: Dp

                if (count <= 2) {
                    // Normal: all events side by side
                    itemWidth = totalWidth / count
                    leftOffset = itemWidth * index
                } else {
                    // Overflow: show 2 columns at a time (each half-width)
                    val visiblePos = index - columnOffset
                    if (visiblePos !in 0..1) return@forEachIndexed
                    itemWidth = totalWidth / 2
                    leftOffset = itemWidth * visiblePos
                }

                val evStart = if (ev.start.toLocalDate().isBefore(date)) LocalTime.MIN else ev.start.toLocalTime()
                val evEnd = if (ev.end.toLocalDate().isAfter(date)) LocalTime.MAX else ev.end.toLocalTime()
                val actualStart = if (evStart.isBefore(rangeStart)) rangeStart else evStart
                val actualEnd = if (evEnd.isAfter(rangeEnd)) rangeEnd else evEnd

                val minutesFromStart = ChronoUnit.MINUTES.between(rangeStart, actualStart).coerceAtLeast(0)
                val topOffsetInitial = 14.dp + slotHeight * (minutesFromStart / 60f)
                val durationMinutes = ChronoUnit.MINUTES.between(actualStart, actualEnd)
                val height = slotHeight * (durationMinutes / 60f)

                var dragOffset by remember { mutableStateOf(0f) }
                var isHeld by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(height)
                        .offset(x = leftOffset, y = topOffsetInitial + dragOffset.dp)
                        .padding(1.dp)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        // A held event gets a heavy border, so it is clear what the drag will move.
                        .border(if (isHeld) 3.dp else 1.dp, Color.Black, RoundedCornerShape(4.dp))
                        .pointerInput(ev.id) {
                            // Hold first, then drag: a plain swipe over an event pages the day instead.
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragOffset = 0f; isHeld = true },
                                onDragEnd = {
                                    isHeld = false
                                    val minutesDragged = (dragOffset.dp.toPx() / slotHeight.toPx()) * 60
                                    // Held and let go without moving a quarter hour: the event stays put.
                                    if (kotlin.math.abs(minutesDragged) >= 7.5f) {
                                        val totalMinutes = (actualStart.toSecondOfDay() / 60f) + minutesDragged
                                        val snappedMinutes = ((totalMinutes + 7.5f) / 15).toInt() * 15
                                        val finalMinutes = snappedMinutes.coerceIn(0, 1439)
                                        onEventMoved(ev, LocalTime.of(finalMinutes / 60, finalMinutes % 60))
                                    }
                                    dragOffset = 0f
                                },
                                onDragCancel = { dragOffset = 0f; isHeld = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y / density
                                }
                            )
                        }
                        .clickable { onEventClick(ev) }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    // 14sp lines are about 17dp tall; an hour is about 44dp. The
                    // time and location lines only show when the block has room.
                    Column {
                        TextMMD(ev.title, fontSize = EinkType.Label, fontWeight = FontWeight.Bold, maxLines = 1, lineHeight = 17.sp)
                        if (height.value >= 38f) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val is24Hour = DateFormat.is24HourFormat(context)
                            val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
                            val timeFormatter = DateTimeFormatter.ofPattern(timePattern)
                            TextMMD(
                                "${ev.start.toLocalTime().format(timeFormatter)}",
                                fontSize = EinkType.Label,
                                lineHeight = 17.sp,
                                maxLines = 1
                            )
                        }
                        if (height.value >= 56f && !ev.location.isNullOrBlank()) {
                            TextMMD(
                                text = ev.location,
                                fontSize = EinkType.Label,
                                lineHeight = 17.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Current time indicator
        if (date == LocalDate.now()) {
            val now = currentTime
            if (now.isAfter(rangeStart) && now.isBefore(rangeEnd)) {
                val minutesFromStart = ChronoUnit.MINUTES.between(rangeStart, now)
                val topOffset = 14.dp + slotHeight * (minutesFromStart / 60f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = topOffset - 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Black, CircleShape))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                }
            }
        }

        // Horizontal column position indicator — only when overflow exists
        if (hasOverflow) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..maxColumnOffset) {
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 4.dp)
                            .background(
                                color = if (i == columnOffset) EinkColors.Ink else EinkColors.Paper,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .border(1.dp, EinkColors.Ink, RoundedCornerShape(2.dp))
                            .clickable {
                                if (i != columnOffset) onColumnOffsetChange(i)
                            }
                    )
                }
            }
        }
    }
}

fun overlaps(e1: CalendarEvent, e2: CalendarEvent, date: LocalDate): Boolean {
    val s1 = if (e1.start.toLocalDate().isBefore(date)) LocalTime.MIN else e1.start.toLocalTime()
    val f1 = if (e1.end.toLocalDate().isAfter(date)) LocalTime.MAX else e1.end.toLocalTime()
    val s2 = if (e2.start.toLocalDate().isBefore(date)) LocalTime.MIN else e2.start.toLocalTime()
    val f2 = if (e2.end.toLocalDate().isAfter(date)) LocalTime.MAX else e2.end.toLocalTime()
    return s1 < f2 && s2 < f1
}

@Composable
fun DayViewScrollIndicator(currentPage: Int, onPageSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.width(24.dp).fillMaxHeight().padding(vertical = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        for (i in 0..2) {
            // The bar is 6dp wide, but the whole 24dp column is the tap target.
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 80.dp)
                    .clickable { onPageSelected(i) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 80.dp)
                        .background(
                            color = if (i == currentPage) EinkColors.Ink else EinkColors.Paper,
                            shape = RoundedCornerShape(3.dp)
                        )
                        .border(width = 1.dp, color = EinkColors.Ink, shape = RoundedCornerShape(3.dp))
                )
            }
            if (i < 2) Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

fun getHoursForPage(page: Int): List<Int> {
    return when (page) {
        0 -> (0..11).toList()
        1 -> (8..19).toList()
        else -> (12..23).toList()
    }
}
