package com.kompakt.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kompakt.calendar.ui.common.DashedDivider
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.HeaderTitle
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val events by viewModel.monthEvents.collectAsState()
    val showWeekNumbers by viewModel.showWeekNumbers.collectAsState()
    val startDayMonday by viewModel.startWeekOnMonday.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshPermission() }

    Scaffold(
        topBar = {
            // 360dp wide: two 48dp arrows + three 48dp actions leave room for a
            // short month name ("Sep 2026") at the 24sp title size.
            ScreenHeader(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeaderAction(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Month", { viewModel.previousMonth() })
                        HeaderTitle("${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${currentMonth.year}")
                        HeaderAction(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Month", { viewModel.nextMonth() })
                    }
                },
                actions = {
                    HeaderAction(Icons.Outlined.ViewAgenda, "Agenda", { navController.navigate("agenda") })
                    HeaderAction(Icons.Default.Search, "Search", { navController.navigate("event_search") })
                    HeaderAction(Icons.Outlined.Settings, "Settings", { navController.navigate("settings") })
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp)
            ) {
                if (currentMonth != YearMonth.now()) {
                    FloatingActionButtonMMD(
                        onClick = { viewModel.goToToday() },
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TextMMD(
                                text = "Today",
                                fontSize = EinkType.Body,
                                fontWeight = FontWeight.Bold,
                                color = EinkColors.Ink
                            )
                        }
                    }
                }
                FloatingActionButtonMMD(
                    onClick = {
                        viewModel.beginNewEvent()
                        navController.navigate("add_event?fromCalendar=true")
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Event", modifier = Modifier.size(32.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentMonth) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                },
                                onDragEnd = {
                                    if (totalDrag > 50) {
                                        viewModel.previousMonth()
                                    } else if (totalDrag < -50) {
                                        viewModel.nextMonth()
                                    }
                                }
                            )
                        }
                ) {
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    DaysOfWeekHeader(startDayMonday = startDayMonday, showWeekNumbers = showWeekNumbers)

                    val eventDays = remember(events) {
                        val set = mutableSetOf<LocalDate>()
                        events.forEach { ev ->
                            var d = ev.start.toLocalDate()
                            val last = if (ev.allDay) ev.end.toLocalDate().minusDays(1) else ev.end.toLocalDate()
                            while (!d.isAfter(last)) {
                                set.add(d)
                                d = d.plusDays(1)
                            }
                        }
                        set
                    }

                    CalendarGrid(
                        currentMonth = currentMonth,
                        eventDays = eventDays,
                        showWeekNumbers = showWeekNumbers,
                        startDayMonday = startDayMonday,
                        onDateSelected = { date ->
                            viewModel.onDateSelected(date)
                            navController.navigate("day_view")
                        },
                        onDateLongClick = { date ->
                            viewModel.beginNewEvent(date = date)
                            navController.navigate("add_event?fromCalendar=true")
                        }
                    )

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

@Composable
fun DaysOfWeekHeader(startDayMonday: Boolean, showWeekNumbers: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
    ) {
        if (showWeekNumbers) {
            Spacer(modifier = Modifier.width(32.dp)) // Same width as WeekNumberCell
        }
        val daysOfWeek = if (startDayMonday) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        } else {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        }
        daysOfWeek.forEach { day ->
            TextMMD(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontSize = EinkType.Small
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    eventDays: Set<LocalDate>,
    showWeekNumbers: Boolean,
    startDayMonday: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayOfWeekValue = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
    
    // Offset calculation: 
    // If starting on Monday, offset is dayOfWeekValue - 1
    // If starting on Sunday, offset is dayOfWeekValue % 7
    val offset = if (startDayMonday) {
        dayOfWeekValue - 1
    } else {
        dayOfWeekValue % 7
    }

    val days = mutableListOf<LocalDate>()

    val prevMonth = currentMonth.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()
    for (i in offset - 1 downTo 0) {
        days.add(prevMonth.atDay(daysInPrevMonth - i))
    }

    val daysInMonth = currentMonth.lengthOfMonth()
    for (i in 1..daysInMonth) {
        days.add(currentMonth.atDay(i))
    }

    val totalCells = 42
    val remainingCells = totalCells - days.size
    for (i in 1..remainingCells) {
        days.add(currentMonth.plusMonths(1).atDay(i))
    }

    val weeks = days.chunked(7)
    val today = LocalDate.now()

    Column {
        weeks.forEachIndexed { index, weekDays ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (showWeekNumbers) {
                    WeekNumberCell(weekDays.first())
                }
                weekDays.forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        DayCell(
                            date = date,
                            isActiveDay = date == today,
                            isCurrentMonth = YearMonth.from(date) == currentMonth,
                            hasEvent = eventDays.contains(date),
                            onDateSelected = onDateSelected,
                            onDateLongClick = onDateLongClick
                        )
                    }
                }
            }
            if (index < weeks.size - 1) {
                WeekRowDivider(showWeekNumbers = showWeekNumbers)
            }
        }
    }
}

@Composable
fun WeekNumberCell(firstDayOfWeek: LocalDate) {
    // ISO-8601 week number
    val weekFields = java.time.temporal.WeekFields.ISO
    val weekNum = firstDayOfWeek.get(weekFields.weekOfWeekBasedYear())

    Box(
        modifier = Modifier
            .width(32.dp)
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        TextMMD(
            text = weekNum.toString(),
            fontSize = EinkType.Label,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WeekRowDivider(showWeekNumbers: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (showWeekNumbers) {
            Spacer(modifier = Modifier.width(32.dp))
        }
        DashedDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    isActiveDay: Boolean,
    isCurrentMonth: Boolean,
    hasEvent: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongClick: (LocalDate) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(date) {
                detectTapGestures(
                    onTap = { onDateSelected(date) },
                    onLongPress = { onDateLongClick(date) }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = if (isActiveDay) Color.Black else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Black and white only: another month's days are regular weight,
                // this month's bold, today inverted.
                TextMMD(
                    text = date.dayOfMonth.toString(),
                    color = if (isActiveDay) EinkColors.Paper else EinkColors.Ink,
                    fontWeight = if (isActiveDay || isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                    fontSize = EinkType.Body
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (hasEvent) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color.Black, CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}
