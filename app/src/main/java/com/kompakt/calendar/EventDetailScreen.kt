package com.kompakt.calendar

import android.text.format.DateFormat
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kompakt.calendar.calendar.CalendarEvent
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.PageLoading
import com.kompakt.calendar.ui.mmd.PanelActions
import com.kompakt.calendar.ui.mmd.PanelBody
import com.kompakt.calendar.ui.mmd.PanelDialog
import com.kompakt.calendar.ui.mmd.PanelPrimaryAction
import com.kompakt.calendar.ui.mmd.PanelSecondaryAction
import com.kompakt.calendar.ui.mmd.PanelTitle
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    navController: NavController,
    eventId: Long,
    instanceTime: Long? = null,
    viewModel: CalendarViewModel = viewModel()
) {
    var event by remember { mutableStateOf<CalendarEvent?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val useAmericanDateFormat by viewModel.useAmericanDateFormat.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        scope.launch {
            event = viewModel.loadEventById(eventId)
            loading = false
        }
    }

    // Pattern P5: every delete confirms in a bottom panel, and a repeating
    // event asks which occurrences first. No drop-down menus.
    val instanceMillis = instanceTime ?: event?.start?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    if (showDeleteConfirm) {
        val recurring = !event?.rrule.isNullOrBlank()
        PanelDialog(onDismissRequest = { showDeleteConfirm = false }) {
            PanelTitle(if (recurring) "Delete repeating event?" else "Delete event?")
            PanelBody(event?.title?.ifBlank { "Untitled event" } ?: "")
            PanelActions {
                PanelSecondaryAction("Cancel", onClick = { showDeleteConfirm = false })
                if (recurring) {
                    PanelSecondaryAction("All events", onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            viewModel.deleteEventById(eventId, null)
                            navController.popBackStack()
                        }
                    })
                }
                PanelPrimaryAction(if (recurring) "This event only" else "Delete", onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        if (recurring) viewModel.deleteEventById(eventId, instanceMillis) else viewModel.deleteEventById(eventId)
                        navController.popBackStack()
                    }
                })
            }
        }
    }
    if (showEditConfirm) {
        PanelDialog(onDismissRequest = { showEditConfirm = false }) {
            PanelTitle("Edit repeating event")
            PanelBody("Change this event only, or every event in the series?")
            PanelActions {
                PanelSecondaryAction("Cancel", onClick = { showEditConfirm = false })
                PanelSecondaryAction("All events", onClick = {
                    showEditConfirm = false
                    event?.let { ev ->
                        viewModel.beginEditEvent(ev, null)
                        navController.navigate("add_event?fromCalendar=false")
                    }
                })
                PanelPrimaryAction("This event only", onClick = {
                    showEditConfirm = false
                    event?.let { ev ->
                        viewModel.beginEditEvent(ev, instanceMillis)
                        navController.navigate("add_event?fromCalendar=false")
                    }
                })
            }
        }
    }

    Scaffold(
        topBar = {
            ScreenHeader(
                title = "Event",
                navigationIcon = {
                    HeaderAction(Icons.Default.Close, "Close", { navController.popBackStack() })
                },
                actions = {
                    HeaderAction(Icons.Default.DeleteOutline, "Delete", { if (event != null) showDeleteConfirm = true })
                    HeaderAction(Icons.Default.Edit, "Edit", {
                        if (event?.rrule.isNullOrBlank()) {
                            event?.let { ev ->
                                viewModel.beginEditEvent(ev)
                                navController.navigate("add_event?fromCalendar=false")
                            }
                        } else {
                            showEditConfirm = true
                        }
                    })
                }
            )
        }
    ) { paddingValues ->
        if (loading) {
            PageLoading(Modifier.padding(paddingValues))
        } else if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                TextMMD("Event not found", fontSize = EinkType.TitleMedium)
            }
        } else {
            val ev = event!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                TextMMD(
                    text = ev.title.ifBlank { "Untitled event" },
                    fontSize = EinkType.Headline,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                val context = androidx.compose.ui.platform.LocalContext.current
                val is24Hour = DateFormat.is24HourFormat(context)
                val timePattern = if (is24Hour) "HH:mm" else "h:mm a"

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(2.dp, EinkColors.Ink, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        val pattern = if (useAmericanDateFormat) "EEEE, MMMM d" else "EEEE, d MMMM"
                        TextMMD(
                            text = ev.start.format(DateTimeFormatter.ofPattern(pattern, Locale.US)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                        val timeRange = if (ev.allDay) {
                            "All day"
                        } else {
                            "${ev.start.format(DateTimeFormatter.ofPattern(timePattern, Locale.US))} – ${ev.end.format(DateTimeFormatter.ofPattern(timePattern, Locale.US))}"
                        }
                        TextMMD(
                            text = timeRange,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                val rrule = ev.rrule
                if (!rrule.isNullOrBlank()) {
                    val recurrenceText = when {
                        rrule.contains("FREQ=DAILY") -> "Daily"
                        rrule.contains("INTERVAL=2") && rrule.contains("FREQ=WEEKLY") -> "Bi-weekly"
                        rrule.contains("FREQ=WEEKLY") -> "Weekly"
                        rrule.contains("FREQ=MONTHLY") -> "Monthly"
                        rrule.contains("FREQ=YEARLY") -> "Yearly"
                        else -> "Recurring"
                    }
                    DetailRow(label = "Repeat", value = recurrenceText)
                }

                val location = ev.location
                if (!location.isNullOrBlank()) {
                    DetailRow(label = "Location", value = location)
                }

                DetailRow(label = "Calendar", value = ev.calendarDisplayName)

                if (ev.hasReminder) {
                    val reminderText = ev.reminders.joinToString(", ") { mins ->
                        when (mins) {
                            0 -> "At time of event"
                            in 1..59 -> "$mins minutes before"
                            in 60..1439 -> {
                                val hours = mins / 60
                                val m = mins % 60
                                if (m == 0) "$hours ${if (hours == 1) "hour" else "hours"} before"
                                else "${hours}h ${m}m before"
                            }
                            else -> {
                                val days = mins / 1440
                                "$days ${if (days == 1) "day" else "days"} before"
                            }
                        }
                    }
                    DetailRow(label = if (ev.reminders.size > 1) "Reminders" else "Reminder", value = reminderText)
                }

                val description = ev.description
                if (!description.isNullOrBlank()) {
                    DetailRow(label = "Notes", value = description)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        TextMMD(
            text = label,
            fontSize = EinkType.TitleSmall,
            fontWeight = FontWeight.Bold
        )
        TextMMD(
            text = value,
            fontSize = EinkType.Body,
            fontWeight = FontWeight.Normal
        )
    }
}
