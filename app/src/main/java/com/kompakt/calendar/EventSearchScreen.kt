package com.kompakt.calendar

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kompakt.calendar.calendar.CalendarEvent
import com.kompakt.calendar.ui.common.DashedDivider
import com.kompakt.calendar.ui.mmd.PagedList
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkRowTokens
import com.kompakt.calendar.ui.mmd.RowDivider
import com.kompakt.calendar.ui.mmd.SectionHeader
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.PageLoading
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSearchScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    // Open with the cursor in the search box, so typing works at once.
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { searchFocus.requestFocus() }

    val listState = rememberLazyListState()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { listState.canScrollBackward } }
    val isScrollable by remember { derivedStateOf { canScrollForward || canScrollBackward } }

    // A new query cancels the previous search, so an old result never replaces a newer one.
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300)
        results = viewModel.search(searchQuery)
        isSearching = false
    }
    val now = remember(results) { LocalDateTime.now() }
    val upcoming = remember(results) { results.filter { !it.end.isBefore(now) }.sortedBy { it.start } }
    val past = remember(results) { results.filter { it.end.isBefore(now) }.sortedByDescending { it.start } }

    Scaffold(
        topBar = {
            ScreenHeader(
                navigationIcon = {
                    HeaderAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", { navController.popBackStack() })
                },
                title = {
                    // Black and white only: the placeholder is black text, not grey.
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { TextMMD("Search events", color = EinkColors.Ink, fontSize = EinkType.TitleMedium) },
                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp).focusRequester(searchFocus),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = EinkType.TitleMedium, color = EinkColors.Ink)
                    )
                },
                actions = {
                    if (searchQuery.isNotEmpty()) {
                        HeaderAction(Icons.Default.Close, "Clear", { searchQuery = "" })
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextMMD(
                        "Search titles, notes, and places",
                        fontSize = EinkType.TitleMedium
                    )
                }
            } else if (isSearching) {
                PageLoading(label = "Searching")
            } else if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextMMD("No events found", fontSize = EinkType.TitleMedium)
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    PagedList(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        resultSection("Upcoming", upcoming, navController)
                        resultSection("Past", past, navController)
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.resultSection(title: String, events: List<CalendarEvent>, navController: NavController) {
    if (events.isEmpty()) return
    item { SectionHeader(title) }
    itemsIndexed(events) { index, event ->
        if (index > 0) RowDivider()
        EventSearchResultItem(
            event = event,
            onClick = {
                val time = event.start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                navController.navigate("event_detail/${event.id}?instanceTime=$time")
            }
        )
    }
}

@Composable
private fun EventSearchResultItem(event: CalendarEvent, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
    val datePattern = if (event.start.year == LocalDate.now().year) "EEE, d MMM" else "EEE, d MMM yyyy"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .heightIn(min = EinkRowTokens.MinHeight)
            .padding(horizontal = EinkRowTokens.Inset, vertical = EinkRowTokens.VerticalPadding),
        verticalArrangement = Arrangement.Center
    ) {
        TextMMD(
            text = event.title,
            fontSize = EinkType.TitleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        TextMMD(
            text = if (event.allDay)
                event.start.toLocalDate().format(DateTimeFormatter.ofPattern(datePattern, Locale.US))
            else
                event.start.format(DateTimeFormatter.ofPattern("$datePattern · $timePattern", Locale.US)),
            fontSize = EinkType.Body,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (!event.location.isNullOrBlank()) {
            TextMMD(
                text = event.location!!,
                fontSize = EinkType.Body,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
