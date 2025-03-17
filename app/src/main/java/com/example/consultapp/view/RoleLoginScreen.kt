package com.example.consultapp.view

import CustomTopAppBar
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.consultapp.R

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import setLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleLoginScreen(
    role: String,
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    onNavigateToCreateAccount: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var currentLanguage by remember { mutableStateOf("en") }

    val context = LocalContext.current // Get the context using LocalContext.current

    // Scaffold to display the UI
    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Login as $role",
                onBack = onBack,
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = onToggleDarkMode,
                onLanguageChanged = {
                    val newLanguage = if (currentLanguage == "en") "th" else "en"
                    currentLanguage = newLanguage
                    setLocale(context, newLanguage) // Use the context from LocalContext
                    context.startActivity(Intent(context, PatientActivity::class.java)) // Restart activity
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Outlined text fields for email and password input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.email)) }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(id = R.string.password)) },
                    visualTransformation = PasswordVisualTransformation()
                )

                // Display error message if present
                errorMessage?.let { msg ->
                    Text(text = msg, color = Color.Red)
                }

                // Login button
                Button(
                    onClick = {
                        // Handle login logic using Firebase Authentication
                        coroutineScope.launch {
                            try {
                                FirebaseAuth.getInstance()
                                    .signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            // Trigger login success action
                                            onLoginSuccess()
                                        } else {
                                            // Handle error during login
                                            errorMessage = task.exception?.message ?: "Login failed"
                                        }
                                    }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Login failed"
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(id = R.string.login))
                }

                // Button to navigate to Create Account screen
                Button(
                    onClick = onNavigateToCreateAccount,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(id = R.string.create_account))
                }
            }
        }
    )
}
