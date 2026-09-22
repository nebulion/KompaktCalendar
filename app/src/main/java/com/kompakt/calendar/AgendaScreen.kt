package com.kompakt.calendar

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kompakt.calendar.calendar.CalendarEvent
import com.kompakt.calendar.ui.common.DashedDivider
import com.kompakt.calendar.ui.mmd.FabClearance
import com.kompakt.calendar.ui.EInkScrollbar
import com.kompakt.calendar.ui.eInkVerticalScroll
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.HeaderTitle
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val events by viewModel.upcomingEvents.collectAsState()
    val useAmericanDateFormat by viewModel.useAmericanDateFormat.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshPermission() }

    Scaffold(
        topBar = {
            ScreenHeader(
                title = { HeaderTitle("Agenda", Modifier.padding(start = 8.dp)) },
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
                if (events.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TextMMD("No upcoming events", fontSize = EinkType.TitleMedium)
                    }
                } else {
                    val groupedEvents = remember(events) {
                        val today = LocalDate.now()
                        val horizon = today.plusMonths(3)
                        val map = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()

                        events.forEach { ev ->
                            val start = ev.start.toLocalDate()
                            val end = if (ev.allDay) ev.end.toLocalDate().minusDays(1) else ev.end.toLocalDate()

                            var current = if (start.isBefore(today)) today else start
                            while (!current.isAfter(end) && !current.isAfter(horizon)) {
                                map.getOrPut(current) { mutableListOf() }.add(ev)
                                current = current.plusDays(1)
                            }
                        }
                        map.toSortedMap()
                    }
                    AgendaList(
                        groupedEvents = groupedEvents,
                        useAmericanDateFormat = useAmericanDateFormat,
                        onEventClick = { event ->
                            val time = event.start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            navController.navigate("event_detail/${event.id}?instanceTime=$time")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaList(
    groupedEvents: Map<LocalDate, List<CalendarEvent>>,
    useAmericanDateFormat: Boolean,
    onEventClick: (CalendarEvent) -> Unit
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val canScrollForward by remember { derivedStateOf { state.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { state.canScrollBackward } }
    val isScrollable by remember { derivedStateOf { canScrollForward || canScrollBackward } }

    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .eInkVerticalScroll(state, scope, isScrollable),
            userScrollEnabled = false,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            groupedEvents.forEach { (date, dayEvents) ->
                item(key = date) {
                    AgendaHeader(date, useAmericanDateFormat)
                }
                itemsIndexed(dayEvents, key = { _, e -> "${date}_${e.id}_${e.start}" }) { index, event ->
                    AgendaItem(event) { onEventClick(event) }
                    if (index < dayEvents.size - 1) {
                        DashedDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }

        if (isScrollable) {
            EInkScrollbar(state = state, scope = scope, bottomInset = FabClearance)
        }
    }
}

@Composable
fun AgendaHeader(date: LocalDate, useAmericanDateFormat: Boolean) {
    val today = LocalDate.now()
    val pattern = if (useAmericanDateFormat) "EEEE, MMMM d" else "EEEE, d MMMM"
    val label = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    Column {
        // Day heading: bold, over a solid one-pixel-wide black line, so it reads
        // apart from the dotted lines between events.
        TextMMD(
            text = label,
            fontSize = EinkType.Body,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        HorizontalDividerMMD(thickness = 1.dp, color = EinkColors.Ink)
    }
}

@Composable
fun AgendaItem(event: CalendarEvent, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
    val timeFormatter = DateTimeFormatter.ofPattern(timePattern, Locale.getDefault())
    val timeTxt = if (event.allDay) "All day" else {
        "${event.start.format(timeFormatter)} - ${event.end.format(timeFormatter)}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Two-line row as in Kompakt Notes: bold title, regular black detail lines.
        TextMMD(
            text = event.title,
            fontSize = EinkType.TitleMedium,
            fontWeight = FontWeight.Bold
        )
        TextMMD(
            text = timeTxt,
            fontSize = EinkType.Body,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (!event.location.isNullOrBlank()) {
            TextMMD(
                text = event.location,
                fontSize = EinkType.Body,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
