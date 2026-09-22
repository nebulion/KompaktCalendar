package com.kompakt.calendar

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kompakt.calendar.calendar.CalendarAccount
import com.kompakt.calendar.ui.common.DashedDivider
import com.kompakt.calendar.ui.mmd.RowDivider
import com.kompakt.calendar.calendar.pickDefaultCalendar
import com.kompakt.calendar.ui.mmd.PagedList
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkTokens
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.InlineMessage
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.radio_button.RadioButtonMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel(),
    useToday: Boolean = false
) {
    val title by viewModel.draftTitle.collectAsState()
    val startDate by viewModel.draftStartDate.collectAsState()
    val endDate by viewModel.draftEndDate.collectAsState()
    val startTime by viewModel.draftStartTime.collectAsState()
    val endTime by viewModel.draftEndTime.collectAsState()
    val isAllDay by viewModel.draftIsAllDay.collectAsState()
    val selectedCalendarId by viewModel.draftCalendarId.collectAsState()
    val selectedReminders by viewModel.draftReminders.collectAsState()
    val rrule by viewModel.draftRrule.collectAsState()
    val useAmericanDateFormat by viewModel.useAmericanDateFormat.collectAsState()
    val rruleUntil by viewModel.draftRruleUntil.collectAsState()
    val rruleDays by viewModel.draftRruleDays.collectAsState()
    val location by viewModel.draftLocation.collectAsState()

    val eventNote by viewModel.eventNote.collectAsState()
    val editingId by viewModel.editingEventId.collectAsState()
    val calendars by viewModel.calendarsLive.collectAsState()
    val defaultCalendarId by viewModel.defaultCalendarId.collectAsState()
    
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // A save problem shows as a static line under the header (no Toast).
    var saveError by remember { mutableStateOf<String?>(null) }

    val isEdit = editingId != null

    // Ensure calendar ID is initialized if null
    LaunchedEffect(calendars, defaultCalendarId, selectedCalendarId) {
        if (selectedCalendarId == null) {
            pickDefaultCalendar(calendars, defaultCalendarId)?.let { viewModel.updateDraftCalendarId(it.id) }
        }
    }

    var showReminderPicker by remember { mutableStateOf(false) }
    var showCustomReminderPicker by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var showRecurrencePicker by remember { mutableStateOf(false) }
    var dateTimeSheet by remember { mutableStateOf(DateTimeSheet.None) }
    val startWeekOnMonday by viewModel.startWeekOnMonday.collectAsState()

    val reminderOptions = remember(isAllDay) {
        if (isAllDay) {
            listOf(
                "On the day (8:00 am)" to -480,
                "On the day (9:00 am)" to -540,
                "Day before (6:00 pm)" to 360,
                "Day before (8:00 pm)" to 240,
                "1 day before" to 1440,
                "2 days before" to 2880,
                "1 week before" to 10080
            )
        } else {
            listOf(
                "5 minutes before" to 5,
                "10 minutes before" to 10,
                "15 minutes before" to 15,
                "30 minutes before" to 30,
                "1 hour before" to 60,
                "1 day before" to 1440,
                "1 week before" to 10080
            )
        }
    }

    val recurrenceOptions = listOf(
        "Does not repeat" to null,
        "Daily" to "FREQ=DAILY",
        "Weekly" to "FREQ=WEEKLY",
        "Bi-weekly" to "FREQ=WEEKLY;INTERVAL=2",
        "Monthly" to "FREQ=MONTHLY",
        "Yearly" to "FREQ=YEARLY"
    )

    val writableCalendars = remember(calendars) { calendars.filter { it.isWritable } }
    val selectedCalendar = calendars.firstOrNull { it.id == selectedCalendarId }

    val listState = rememberLazyListState()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { listState.canScrollBackward } }
    val isScrollable by remember { derivedStateOf { canScrollForward || canScrollBackward } }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color.White,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(28.dp))
                        }

                        TextField(
                            value = title,
                            onValueChange = { viewModel.updateDraftTitle(it) },
                            placeholder = { TextMMD("Add Title", fontSize = EinkType.Title, color = EinkColors.Ink) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = EinkType.Title, fontWeight = FontWeight.Bold, color = EinkColors.Ink)
                        )

                        ButtonMMD(
                            onClick = {
                                if (selectedCalendarId == null) {
                                    saveError = "No writable calendar. Tick a calendar in DecSync CC, or add an account in DAVx5 or Google first."
                                    return@ButtonMMD
                                }
                                saveError = null
                                scope.launch {
                                    val ok = viewModel.saveEvent(
                                        EventDraft(
                                            title = title,
                                            startDate = startDate,
                                            endDate = endDate,
                                            startTime = startTime,
                                            endTime = endTime,
                                            allDay = isAllDay,
                                            location = location,
                                            calendarId = selectedCalendarId,
                                            description = eventNote,
                                            rrule = rrule,
                                            rruleUntil = rruleUntil,
                                            rruleDays = rruleDays
                                        ),
                                        reminders = selectedReminders
                                    )
                                    if (ok) {
                                        navController.popBackStack()
                                    } else {
                                        saveError = "Could not save the event."
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            TextMMD(if (isEdit) "Save" else "Create", fontWeight = FontWeight.Bold, fontSize = EinkType.Body)
                        }
                    }
                    // The 3dp header rule, then a static message where a Toast
                    // (which fades in and out) used to appear.
                    HorizontalDividerMMD(thickness = EinkTokens.HeaderRule, color = EinkColors.Ink)
                    saveError?.let { InlineMessage(it) }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
            ) {
                PagedList(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    item {
                        DateTimeField(
                            label = "Starts",
                            date = startDate,
                            time = startTime,
                            isAllDay = isAllDay,
                            useAmericanDateFormat = useAmericanDateFormat,
                            onDateClick = { dateTimeSheet = DateTimeSheet.StartDate },
                            onTimeClick = { dateTimeSheet = DateTimeSheet.StartTime }
                        )
                    }

                    item {
                        DateTimeField(
                            label = "Ends",
                            date = endDate,
                            time = endTime,
                            isAllDay = isAllDay,
                            useAmericanDateFormat = useAmericanDateFormat,
                            onDateClick = { dateTimeSheet = DateTimeSheet.EndDate },
                            onTimeClick = { dateTimeSheet = DateTimeSheet.EndTime }
                        )
                    }

                    item {
                        OptionRow(
                            icon = Icons.Outlined.Today,
                            title = "All-day event",
                            checked = isAllDay,
                            onCheckedChange = { viewModel.updateDraftIsAllDay(it) }
                        )
                    }

                    item {
                        RowDivider()
                    }

                    item {
                        OptionRow(
                            icon = Icons.Default.CalendarMonth,
                            title = recurrenceOptions.find { it.second == rrule }?.first ?: "Does not repeat",
                            hasChevron = true,
                            onClick = { showRecurrencePicker = true }
                        )
                    }

                    item {
                        RowDivider()
                    }

                    item {
                        val reminderText = if (selectedReminders.isEmpty()) {
                            "No reminder"
                        } else {
                            selectedReminders.joinToString(", ") { mins ->
                                reminderOptions.find { it.second == mins }?.first ?: formatMinutes(mins)
                            }
                        }
                        OptionRow(
                            icon = Icons.Outlined.NotificationsNone,
                            title = reminderText,
                            hasChevron = true,
                            onClick = { showReminderPicker = true },
                            enabled = !isAllDay
                        )
                    }

                    item {
                        RowDivider()
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            CompactOptionItem(
                                icon = Icons.Outlined.LocationOn,
                                title = if (location.isEmpty()) "Add location" else {
                                    if (location.length > 10) "${location.take(10)}..." else location
                                },
                                onClick = { navController.navigate("location") },
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(Color.Black)
                                    .align(Alignment.CenterVertically)
                            )
                            CompactOptionItem(
                                icon = Icons.Outlined.Description,
                                title = if (eventNote.isEmpty()) {
                                    "Add notes"
                                } else {
                                    val displayNote = eventNote.replace("\n", " ")
                                    if (displayNote.length > 10) "${displayNote.take(10)}..." else displayNote
                                },
                                onClick = { navController.navigate("notes") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        RowDivider()
                    }

                    item {
                        OptionRow(
                            icon = Icons.Default.CalendarMonth,
                            title = selectedCalendar?.let {
                                it.displayName + (it.syncSourceLabel?.let { label -> " ($label)" } ?: "")
                            } ?: "Choose calendar",
                            hasChevron = true,
                            onClick = { showCalendarPicker = true }
                        )
                    }
                }
            }
        }

        if (showCalendarPicker) {
            CalendarPickerOverlay(
                calendars = writableCalendars,
                selectedId = selectedCalendarId,
                onPick = {
                    viewModel.updateDraftCalendarId(it)
                    showCalendarPicker = false
                },
                onDismiss = { showCalendarPicker = false }
            )
        }

        if (showReminderPicker) {
            ReminderPickerOverlay(
                options = reminderOptions,
                selectedReminders = selectedReminders,
                onPick = {
                    viewModel.updateDraftReminders(it)
                },
                onCustomClick = {
                    showReminderPicker = false
                    showCustomReminderPicker = true
                },
                onDismiss = { showReminderPicker = false }
            )
        }

        if (showCustomReminderPicker) {
            CustomReminderPickerOverlay(
                onPick = { minutes ->
                    viewModel.updateDraftReminders(selectedReminders + minutes)
                    showCustomReminderPicker = false
                },
                onDismiss = { showCustomReminderPicker = false }
            )
        }

        if (showRecurrencePicker) {
            RecurrencePickerOverlay(
                options = recurrenceOptions,
                selectedRrule = rrule,
                selectedUntil = rruleUntil,
                selectedDays = viewModel.draftRruleDays.collectAsState().value,
                startDate = startDate,
                useAmericanDateFormat = useAmericanDateFormat,
                onPick = { rule, until, days ->
                    viewModel.updateDraftRrule(rule)
                    viewModel.updateDraftRruleUntil(until)
                    viewModel.updateDraftRruleDays(days)
                    showRecurrencePicker = false
                },
                onDismiss = { showRecurrencePicker = false }
            )
        }

        val closeSheet = { dateTimeSheet = DateTimeSheet.None }
        when (dateTimeSheet) {
            DateTimeSheet.None -> Unit
            DateTimeSheet.StartDate -> DatePickerSheet(
                title = "Start date",
                selected = startDate,
                startWeekOnMonday = startWeekOnMonday,
                onPick = { viewModel.updateDraftStartDate(it); closeSheet() },
                onDismiss = closeSheet
            )
            DateTimeSheet.EndDate -> DatePickerSheet(
                title = "End date",
                selected = endDate,
                startWeekOnMonday = startWeekOnMonday,
                onPick = { viewModel.updateDraftEndDate(it); closeSheet() },
                onDismiss = closeSheet
            )
            DateTimeSheet.StartTime -> TimePickerSheet(
                title = "Start time",
                initial = startTime,
                onPick = { viewModel.updateDraftStartTime(it); closeSheet() },
                onDismiss = closeSheet
            )
            DateTimeSheet.EndTime -> TimePickerSheet(
                title = "End time",
                initial = endTime,
                onPick = { viewModel.updateDraftEndTime(it); closeSheet() },
                onDismiss = closeSheet
            )
        }
    }
}

/** Which date or time sheet the event form shows. */
private enum class DateTimeSheet { None, StartDate, StartTime, EndDate, EndTime }

@Composable
private fun RecurrencePickerOverlay(
    options: List<Pair<String, String?>>,
    selectedRrule: String?,
    selectedUntil: LocalDate?,
    selectedDays: Set<Int>,
    startDate: LocalDate,
    useAmericanDateFormat: Boolean,
    onPick: (String?, LocalDate?, Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { listState.canScrollBackward } }
    val isScrollable by remember { derivedStateOf { canScrollForward || canScrollBackward } }

    var currentRrule by remember { mutableStateOf(selectedRrule) }
    var currentUntil by remember { mutableStateOf(selectedUntil) }
    var currentDays by remember { mutableStateOf(selectedDays) }

    // When switching to weekly, if no days are selected, default to the event's start day
    LaunchedEffect(currentRrule) {
        if (currentRrule?.contains("WEEKLY") == true && currentDays.isEmpty()) {
            currentDays = setOf(startDate.dayOfWeek.value)
        }
    }

    val daysOfWeek = listOf(
        "Mo" to 1, "Tu" to 2, "We" to 3, "Th" to 4, "Fr" to 5, "Sa" to 6, "Su" to 7
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(EinkTokens.HeaderGlyph))
                }
                TextMMD(
                    "Set Recurrence",
                    fontSize = EinkType.Title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                )
                ButtonMMD(
                    onClick = { onPick(currentRrule, currentUntil, currentDays) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    TextMMD("Done", fontSize = EinkType.Body, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDividerMMD(thickness = EinkTokens.HeaderRule, color = EinkColors.Ink)

            Row(modifier = Modifier.fillMaxSize()) {
                PagedList(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    items(options) { (label, rule) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentRrule = rule }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextMMD(
                                label,
                                fontSize = EinkType.TitleMedium,
                                fontWeight = if (currentRrule == rule) FontWeight.Bold else FontWeight.Normal
                            )
                            RadioButtonMMD(
                                selected = currentRrule == rule,
                                onClick = { currentRrule = rule }
                            )
                        }
                        DashedDivider()
                    }

                    if (currentRrule != null) {
                        if (currentRrule?.contains("WEEKLY") == true) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                TextMMD("Repeat on", fontSize = EinkType.TitleSmall, fontWeight = FontWeight.Bold, color = EinkColors.Ink)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    daysOfWeek.forEach { (name, dayNum) ->
                                        val isSelected = currentDays.contains(dayNum)
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    if (isSelected) Color.Black else Color.White,
                                                    CircleShape
                                                )
                                                .border(1.dp, Color.Black, CircleShape)
                                                .clickable {
                                                    currentDays = if (isSelected) {
                                                        if (currentDays.size > 1) currentDays - dayNum else currentDays
                                                    } else {
                                                        currentDays + dayNum
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TextMMD(
                                                text = name,
                                                color = if (isSelected) Color.White else Color.Black,
                                                fontSize = EinkType.Label,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextMMD("Repeat for", fontSize = EinkType.TitleSmall, fontWeight = FontWeight.Bold, color = EinkColors.Ink)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val unitName = when {
                                currentRrule?.contains("DAILY") == true -> "day"
                                currentRrule?.contains("WEEKLY") == true -> "week"
                                currentRrule?.contains("MONTHLY") == true -> "month"
                                currentRrule?.contains("YEARLY") == true -> "year"
                                else -> "unit"
                            }
                            
                            val duration = if (currentUntil == null) 0 else {
                                when (unitName) {
                                    "day" -> java.time.temporal.ChronoUnit.DAYS.between(startDate, currentUntil).toInt()
                                    "week" -> java.time.temporal.ChronoUnit.WEEKS.between(startDate, currentUntil).toInt()
                                    "month" -> java.time.temporal.ChronoUnit.MONTHS.between(startDate, currentUntil).toInt()
                                    "year" -> java.time.temporal.ChronoUnit.YEARS.between(startDate, currentUntil).toInt()
                                    else -> 0
                                }
                            }.coerceAtLeast(0)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    TextMMD(
                                        text = if (duration == 0) "Forever" else "$duration ${unitName}${if (duration > 1) "s" else ""}",
                                        fontSize = EinkType.TitleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (currentUntil != null) {
                                        val pattern = if (useAmericanDateFormat) "EEE, MMM d yyyy" else "EEE, d MMM yyyy"
                                        TextMMD(
                                            text = "Until ${currentUntil?.format(DateTimeFormatter.ofPattern(pattern, Locale.US))}",
                                            fontSize = EinkType.Small,
                                            color = EinkColors.Ink
                                        )
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        if (duration > 0) {
                                            val newDuration = duration - 1
                                            currentUntil = if (newDuration == 0) null else {
                                                when (unitName) {
                                                    "day" -> startDate.plusDays(newDuration.toLong())
                                                    "week" -> startDate.plusWeeks(newDuration.toLong())
                                                    "month" -> startDate.plusMonths(newDuration.toLong())
                                                    "year" -> startDate.plusYears(newDuration.toLong())
                                                    else -> currentUntil
                                                }
                                            }
                                        }
                                    }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Decrease duration")
                                    }
                                    
                                    IconButton(onClick = {
                                        val newDuration = duration + 1
                                        currentUntil = when (unitName) {
                                            "day" -> startDate.plusDays(newDuration.toLong())
                                            "week" -> startDate.plusWeeks(newDuration.toLong())
                                            "month" -> startDate.plusMonths(newDuration.toLong())
                                            "year" -> startDate.plusYears(newDuration.toLong())
                                            else -> currentUntil
                                        }
                                    }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Increase duration")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderPickerOverlay(
    options: List<Pair<String, Int?>>,
    selectedReminders: List<Int>,
    onPick: (List<Int>) -> Unit,
    onCustomClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { listState.canScrollBackward } }
    val isScrollable by remember { derivedStateOf { canScrollForward || canScrollBackward } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(EinkTokens.HeaderGlyph))
                }
                TextMMD(
                    "Set Reminders",
                    fontSize = EinkType.Title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                )
                ButtonMMD(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    TextMMD("Done", fontSize = EinkType.Body, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDividerMMD(thickness = EinkTokens.HeaderRule, color = EinkColors.Ink)

            Row(modifier = Modifier.fillMaxSize()) {
                val predefinedOptions = remember(options) { options.filter { it.second != null } }
                val customSelections = remember(options, selectedReminders) {
                    selectedReminders.filter { mins -> options.none { it.second == mins } }
                }

                PagedList(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(predefinedOptions) { (label, minutes) ->
                        val isSelected = selectedReminders.contains(minutes!!)
                        ReminderToggleRow(
                            label = label,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) onPick(selectedReminders - minutes)
                                else onPick(selectedReminders + minutes)
                            }
                        )
                        DashedDivider()
                    }

                    if (customSelections.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextMMD("Custom Reminders", fontSize = EinkType.TitleSmall, fontWeight = FontWeight.Bold, color = EinkColors.Ink)
                        }
                        items(customSelections) { minutes ->
                            ReminderToggleRow(
                                label = formatMinutes(minutes),
                                isSelected = true,
                                onToggle = { onPick(selectedReminders - minutes) }
                            )
                            DashedDivider()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        ButtonMMD(
                            onClick = onCustomClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            TextMMD("Add Custom Reminder", fontSize = EinkType.Body, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderToggleRow(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(
            label,
            fontSize = EinkType.TitleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        CheckboxMMD(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun CustomReminderPickerOverlay(
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amountStr by remember { mutableStateOf(TextFieldValue("010", TextRange(0))) }
    var unit by remember { mutableStateOf("minutes") }
    val units = listOf("minutes", "hours", "days", "weeks")
    
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(EinkTokens.HeaderGlyph))
                }
                TextMMD(
                    "Custom Reminder",
                    fontSize = EinkType.Title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                )
                ButtonMMD(
                    onClick = {
                        val amount = amountStr.text.toIntOrNull() ?: 0
                        val minutes = when (unit) {
                            "minutes" -> amount
                            "hours" -> amount * 60
                            "days" -> amount * 1440
                            "weeks" -> amount * 10080
                            else -> amount
                        }
                        onPick(minutes)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    TextMMD("Add", fontSize = EinkType.Body, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDividerMMD(thickness = EinkTokens.HeaderRule, color = EinkColors.Ink)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextMMD("Remind me", fontSize = EinkType.Body, color = EinkColors.Ink, modifier = Modifier.padding(bottom = 8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Numeric Input (3 digits)
                    Box(modifier = Modifier.clickable { 
                        amountStr = amountStr.copy(selection = TextRange(0))
                        focusRequester.requestFocus() 
                    }) {
                        BasicTextField(
                            value = amountStr,
                            onValueChange = { newValue ->
                                val oldStr = amountStr.text
                                val newStr = newValue.text
                                val newCursor = newValue.selection.start

                                if (newStr.length > oldStr.length) {
                                    val addedDigit = newStr.getOrNull(newCursor - 1)
                                    if (addedDigit != null && addedDigit.isDigit()) {
                                        val pos = newCursor - 1
                                        if (pos < 3) {
                                            val updatedText = oldStr.substring(0, pos) + addedDigit + oldStr.substring(pos + 1)
                                            amountStr = TextFieldValue(updatedText, TextRange((pos + 1).coerceAtMost(3)))
                                            if (pos == 2) focusManager.clearFocus()
                                        }
                                    }
                                } else if (newStr.length < oldStr.length) {
                                    amountStr = newValue.copy(text = oldStr)
                                } else {
                                    amountStr = newValue
                                }
                            },
                            modifier = Modifier
                                .size(1.dp)
                                .alpha(0f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused }
                                .onKeyEvent {
                                    if (it.key == Key.Backspace) {
                                        val pos = amountStr.selection.start
                                        if (pos > 0) {
                                            amountStr = amountStr.copy(selection = TextRange(pos - 1))
                                        }
                                        true
                                    } else false
                                },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            cursorBrush = SolidColor(Color.Transparent)
                        )

                        Row {
                            val cursor = if (isFocused) amountStr.selection.start else -1
                            DigitBox(amountStr.text.getOrNull(0)?.toString() ?: "0", cursor == 0)
                            DigitBox(amountStr.text.getOrNull(1)?.toString() ?: "0", cursor == 1)
                            DigitBox(amountStr.text.getOrNull(2)?.toString() ?: "0", cursor == 2)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Unit Selection
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                            .clickable { 
                                val nextIndex = (units.indexOf(unit) + 1) % units.size
                                unit = units[nextIndex]
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        TextMMD(unit, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextMMD("before the event", fontSize = EinkType.Body, color = EinkColors.Ink)
            }
        }
    }
}


@Composable
private fun CalendarPickerOverlay(
    calendars: List<CalendarAccount>,
    selectedId: Long?,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { listState.canScrollBackward } }
    val isScrollable by remember { derivedStateOf { canScrollForward || canScrollBackward } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(EinkTokens.HeaderGlyph))
                }
                TextMMD(
                    "Select Calendar",
                    fontSize = EinkType.Title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            HorizontalDividerMMD(thickness = EinkTokens.HeaderRule, color = EinkColors.Ink)

            Row(modifier = Modifier.fillMaxSize()) {
                PagedList(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    if (calendars.isEmpty()) {
                        item {
                            TextMMD("No writable calendars found. Tick a calendar in DecSync CC, or sign in via DAVx5 or Google to add one.")
                        }
                    } else {
                        items(calendars) { cal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(cal.id) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    TextMMD(
                                        cal.displayName,
                                        fontSize = EinkType.TitleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    TextMMD(
                                        cal.accountName + (cal.syncSourceLabel?.let { " · $it" } ?: ""),
                                        fontSize = EinkType.Small,
                                        color = EinkColors.Ink
                                    )
                                }
                                RadioButtonMMD(
                                    selected = cal.id == selectedId,
                                    onClick = { onPick(cal.id) }
                                )
                            }
                            DashedDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    return when {
        minutes % 10080 == 0 -> "${minutes / 10080} week${if (minutes / 10080 > 1) "s" else ""} before"
        minutes % 1440 == 0 -> "${minutes / 1440} day${if (minutes / 1440 > 1) "s" else ""} before"
        minutes % 60 == 0 -> "${minutes / 60} hour${if (minutes / 60 > 1) "s" else ""} before"
        else -> "$minutes minutes before"
    }
}


@Composable
fun DigitBox(char: String, isHighlighted: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 16.dp, height = 26.dp)
            .background(if (isHighlighted) Color.Black else Color.Transparent, RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {
        TextMMD(
            text = char.ifEmpty { "0" },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) Color.White else Color.Black
        )
    }
}


@Composable
fun CompactOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        TextMMD(
            text = title,
            fontSize = EinkType.Body,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun OptionRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    hasChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = EinkColors.Ink, modifier = Modifier.size(24.dp))
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextMMD(
                text = title,
                fontSize = EinkType.Body,
                color = EinkColors.Ink,
                fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1
            )
        }
        if (checked != null && onCheckedChange != null) {
            // Full-size switch: the old 0.6 scale made a 20dp target.
            SwitchMMD(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        } else if (hasChevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(28.dp))
        }
    }
}
