package com.example.consultapp.view

import CustomTopAppBar
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.consultapp.R
import setLocale

import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    onRoleSelected: (String) -> Unit,
    onLanguageSelected: (Locale) -> Unit
) {
    var selectedLocale by remember { mutableStateOf(Locale("en")) }  // Default language to English
    val context = LocalContext.current

    // When the selectedLocale changes, update the language in the app
    LaunchedEffect(selectedLocale) {
        setLocale(context, selectedLocale.toString())  // Apply locale change
    }

    // Scaffold to display the UI
    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = stringResource(id = R.string.role_selection),
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = onToggleDarkMode,
                onLanguageChanged = {
                    val newLanguage = if (selectedLocale.language == "en") "th" else "en"
                    selectedLocale = Locale(newLanguage)
                    onLanguageSelected(selectedLocale)  // Notify language change
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(id = R.string.login_as))
                Button(
                    onClick = { onRoleSelected("patient") },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(stringResource(id = R.string.patient))
                }
                Button(
                    onClick = { onRoleSelected("doctor") },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(stringResource(id = R.string.doctor))
                }
            }
        }
    )
}
