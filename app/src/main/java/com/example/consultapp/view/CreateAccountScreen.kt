package com.example.consultapp.view

import CustomTopAppBar
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.consultapp.R
import com.example.consultapp.viewmodel.CustomBottomBar
import com.example.consultapp.model.AppointmentRequest
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import setLocale
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    role: String,
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    onCreateAccountSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var currentLanguage by remember { mutableStateOf("en") } // Default language

    val context = LocalContext.current

    // Callback for changing language
    val onLanguageChanged: (Locale) -> Unit = { newLanguage ->
        currentLanguage = newLanguage.toString()
        setLocale(context, newLanguage.toString()) // Change the locale
        // Instead of restarting the activity, simply update the UI with the new locale
        Toast.makeText(context, "Language changed to ${newLanguage.displayName}", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Create Account as $role",
                onBack = onBack,
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = onToggleDarkMode,
                onLanguageChanged = onLanguageChanged // Pass the language change callback
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.name)) }
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.email)) }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(id = R.string.password)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(id = R.string.confirm_password)) },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )
                errorMessage?.let { msg ->
                    Text(text = msg, color = Color.Red)
                }
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                        } else {
                            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                                        val userMap = mapOf(
                                            "name" to name,
                                            "email" to email,
                                            "role" to role
                                        )
                                        if (uid != null) {
                                            FirebaseFirestore.getInstance().collection("users")
                                                .document(uid)
                                                .set(userMap)
                                                .addOnCompleteListener { roleTask ->
                                                    if (roleTask.isSuccessful) {
                                                        onCreateAccountSuccess()
                                                    } else {
                                                        errorMessage = roleTask.exception?.message ?: "Failed to save user info."
                                                    }
                                                }
                                        }
                                    } else {
                                        errorMessage = task.exception?.message ?: "Account creation failed"
                                    }
                                }
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(id = R.string.create_account))
                }
            }
        }
    )
}
