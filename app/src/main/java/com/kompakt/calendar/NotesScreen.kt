package com.kompakt.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val initialNote by viewModel.eventNote.collectAsState()
    var note by remember { mutableStateOf(initialNote) }

    Scaffold(
        topBar = {
            ScreenHeader(
                title = "Notes",
                navigationIcon = {
                    HeaderAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", {
                        viewModel.updateEventNote(note)
                        navController.popBackStack()
                    })
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Black and white only: the placeholder is black, not grey.
            TextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxSize(),
                placeholder = { TextMMD("Enter your notes", color = EinkColors.Ink, fontSize = EinkType.TitleMedium) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(fontSize = EinkType.TitleMedium, color = EinkColors.Ink)
            )
        }
    }
}
