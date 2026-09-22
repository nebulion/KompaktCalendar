package com.kompakt.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kompakt.calendar.calendar.CalendarAccount
import com.kompakt.calendar.calendar.pickDefaultCalendar
import com.kompakt.calendar.ui.mmd.PagedList
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkRowTokens
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.RowChevron
import com.kompakt.calendar.ui.mmd.RowDivider
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.kompakt.calendar.ui.mmd.SectionHeader
import com.kompakt.calendar.ui.mmd.SettingsRow
import com.mudita.mmd.components.radio_button.RadioButtonMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch

/** A list choice that Settings opens as its own sheet (pattern P4). */
private enum class SettingsPicker { None, StartView, DefaultCalendar, DefaultReminder }

/** One radio row in a choice sheet. */
private data class Choice<T>(val label: String, val value: T, val subtitle: String? = null)

private val reminderChoices = listOf<Choice<Int?>>(
    Choice("No reminder", null),
    Choice("5 minutes before", 5),
    Choice("10 minutes before", 10),
    Choice("15 minutes before", 15),
    Choice("1 hour before", 60),
    Choice("1 day before", 1440),
    Choice("1 week before", 10080),
)

private val startViewChoices = listOf(
    Choice("Month view", "calendar"),
    Choice("Agenda view", "agenda"),
)

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val context = LocalContext.current
    val showWeekNumbers by viewModel.showWeekNumbers.collectAsState()
    val startDayMonday by viewModel.startWeekOnMonday.collectAsState()
    val useAmericanDateFormat by viewModel.useAmericanDateFormat.collectAsState()
    val startDestination by viewModel.startDestination.collectAsState()
    val calendars by viewModel.calendarsLive.collectAsState()
    val defaultCalendarId by viewModel.defaultCalendarId.collectAsState()
    val defaultReminderMinutes by viewModel.defaultReminderMinutes.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val scope = rememberCoroutineScope()
    var picker by remember { mutableStateOf(SettingsPicker.None) }

    val resumeCount = rememberResumeCount { viewModel.refreshPermission() }
    LaunchedEffect(Unit) { viewModel.refreshPermission() }
    val permissionSummary = remember(resumeCount, hasPermission) {
        permissionSummary(permissionItems(context, hasPermission))
    }

    val writableCalendars = calendars.filter { it.isWritable }
    val savedDefault = writableCalendars.find { it.id == defaultCalendarId }
    val effectiveDefault = pickDefaultCalendar(calendars, defaultCalendarId)

    when (picker) {
        SettingsPicker.None -> Unit
        SettingsPicker.StartView -> {
            ChoiceSheet(
                title = "Open to",
                choices = startViewChoices,
                selected = startDestination,
                onPick = { scope.launch { viewModel.setStartDestination(it); picker = SettingsPicker.None } },
                onDismiss = { picker = SettingsPicker.None },
            )
            return
        }
        SettingsPicker.DefaultCalendar -> {
            ChoiceSheet(
                title = "Default calendar",
                choices = writableCalendars.map { Choice<Long?>(it.displayName, it.id, calendarSourceLine(it)) },
                selected = effectiveDefault?.id,
                onPick = { id -> scope.launch { id?.let { viewModel.setDefaultCalendar(it) }; picker = SettingsPicker.None } },
                onDismiss = { picker = SettingsPicker.None },
            )
            return
        }
        SettingsPicker.DefaultReminder -> {
            ChoiceSheet(
                title = "Default reminder",
                choices = reminderChoices,
                selected = defaultReminderMinutes,
                onPick = { scope.launch { viewModel.setDefaultReminderMinutes(it); picker = SettingsPicker.None } },
                onDismiss = { picker = SettingsPicker.None },
            )
            return
        }
    }

    Scaffold(
        topBar = {
            ScreenHeader(
                title = "Settings",
                navigationIcon = {
                    HeaderAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", { navController.popBackStack() })
                },
                actions = {
                    // Asks DecSync CC / DAVx5 to sync now; the result appears as a static line below.
                    HeaderAction(Icons.Default.Sync, "Sync now", { viewModel.requestSyncNow() })
                }
            )
        }
    ) { paddingValues ->
        PagedList(modifier = Modifier.padding(paddingValues)) {
            syncStatus?.let { status ->
                item {
                    TextMMD(
                        text = status,
                        fontSize = EinkType.Body,
                        color = EinkColors.Ink,
                        modifier = Modifier.padding(
                            start = EinkRowTokens.Inset,
                            end = EinkRowTokens.Inset,
                            top = 12.dp,
                        ),
                    )
                }
            }

            item { SectionHeader("Calendars") }
            if (calendars.isEmpty()) {
                item {
                    SettingsRow(
                        title = "No calendars found",
                        subtitle = "Tick a calendar in DecSync CC, add a CalDAV account in DAVx5, " +
                            "or sign in with Google.",
                    )
                }
            } else {
                rowGroup(calendars) { cal ->
                    SettingsRow(
                        title = cal.displayName,
                        subtitle = calendarSourceLine(cal),
                        onClick = { scope.launch { viewModel.setCalendarVisibility(cal.id, !cal.isVisible) } },
                    ) {
                        SwitchMMD(
                            checked = cal.isVisible,
                            onCheckedChange = { visible ->
                                scope.launch { viewModel.setCalendarVisibility(cal.id, visible) }
                            },
                        )
                    }
                }
            }

            item { SectionHeader("Display") }
            item {
                ToggleRow("Show week numbers", showWeekNumbers) {
                    scope.launch { viewModel.setShowWeekNumbers(it) }
                }
            }
            item {
                RowDivider()
                ToggleRow("Start week on Monday", startDayMonday) {
                    scope.launch { viewModel.setStartWeekOnMonday(it) }
                }
            }
            item {
                RowDivider()
                ToggleRow("American date format", useAmericanDateFormat) {
                    scope.launch { viewModel.setUseAmericanDateFormat(it) }
                }
            }
            item {
                RowDivider()
                PickerRow(
                    title = "Open to",
                    value = startViewChoices.find { it.value == startDestination }?.label ?: "Month view",
                    onClick = { picker = SettingsPicker.StartView },
                )
            }

            item { SectionHeader("New events") }
            item {
                PickerRow(
                    title = "Default calendar",
                    value = when {
                        writableCalendars.isEmpty() -> "No writable calendars"
                        savedDefault != null -> savedDefault.displayName
                        else -> "${effectiveDefault?.displayName} (automatic)"
                    },
                    onClick = if (writableCalendars.isEmpty()) null else ({ picker = SettingsPicker.DefaultCalendar }),
                )
            }
            item {
                RowDivider()
                PickerRow(
                    title = "Default reminder",
                    value = reminderChoices.find { it.value == defaultReminderMinutes }?.label ?: "No reminder",
                    onClick = { picker = SettingsPicker.DefaultReminder },
                )
            }

            item { SectionHeader("System") }
            item {
                PickerRow(
                    title = "Permissions & battery",
                    value = permissionSummary,
                    onClick = { navController.navigate("permissions") },
                )
            }

            item { Spacer(modifier = Modifier.height(EinkRowTokens.SectionTop)) }
        }
    }
}

/** "DecSync Calendars", or "PC Sync · phone only": the account, plus the sync source when the name lacks it. */
private fun calendarSourceLine(cal: CalendarAccount): String =
    cal.accountName + (
        cal.syncSourceLabel
            ?.takeUnless { cal.accountName.contains(it, ignoreCase = true) }
            ?.let { " · $it" } ?: ""
        )

/** Adds [items] as rows with a dotted divider between each pair. */
private fun <T> LazyListScope.rowGroup(items: List<T>, row: @Composable (T) -> Unit) {
    itemsIndexed(items) { index, item ->
        if (index > 0) RowDivider()
        row(item)
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingsRow(title = title, onClick = { onCheckedChange(!checked) }) {
        SwitchMMD(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A row that shows its current value and opens a sheet or page to change it. */
@Composable
private fun PickerRow(title: String, value: String, onClick: (() -> Unit)?) {
    SettingsRow(title = title, subtitle = value, onClick = onClick) {
        if (onClick != null) RowChevron()
    }
}

/** A full-screen sheet of radio rows; picking a row closes it. */
@Composable
private fun <T> ChoiceSheet(
    title: String,
    choices: List<Choice<T>>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EinkColors.Paper,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(
                title = title,
                navigationIcon = { HeaderAction(Icons.Default.Close, "Close", onDismiss) },
            )
            PagedList {
                rowGroup(choices) { choice ->
                    SettingsRow(
                        title = choice.label,
                        subtitle = choice.subtitle,
                        selected = choice.value == selected,
                        onClick = { onPick(choice.value) },
                    ) {
                        RadioButtonMMD(
                            selected = choice.value == selected,
                            onClick = { onPick(choice.value) },
                        )
                    }
                }
            }
        }
    }
}
