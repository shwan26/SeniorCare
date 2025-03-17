package com.example.consultapp.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            var selectedLocale by remember { mutableStateOf(Locale("en")) }

            // Pass the dark mode state into your custom theme.
            ConsultAppTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                getAndStoreFcmToken()

                // Language selection callback
                val onLanguageSelected: (Locale) -> Unit = { locale ->
                    selectedLocale = locale
                    val config = resources.configuration
                    Locale.setDefault(locale)
                    config.setLocale(locale)
                    resources.updateConfiguration(config, resources.displayMetrics)
                }

                NavHost(navController = navController, startDestination = "roleSelection") {
                    composable("roleSelection") {
                        RoleSelectionScreen(
                            darkModeEnabled = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode },
                            onRoleSelected = { role ->
                                navController.navigate("login/$role")
                            },
                            onLanguageSelected = onLanguageSelected // Pass the language callback
                        )
                    }
                    composable(
                        "login/{role}",
                        arguments = listOf(navArgument("role") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: ""
                        RoleLoginScreen(
                            role = role,
                            darkModeEnabled = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode },
                            onLoginSuccess = { navController.navigate(role) },
                            onBack = { navController.popBackStack() },
                            onNavigateToCreateAccount = { navController.navigate("createAccount/$role") }
                        )
                    }
                    composable(
                        "createAccount/{role}",
                        arguments = listOf(navArgument("role") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: ""
                        CreateAccountScreen(
                            role = role,
                            darkModeEnabled = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode },
                            onCreateAccountSuccess = { navController.navigate(role) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    // Role-specific Home Screens
                    val user = FirebaseAuth.getInstance().currentUser
                    val userName = user?.displayName ?: "Unknown"

                    composable("patient") { PatientView(navController, userName) }
                    composable("doctor") { DoctorView(navController, userName) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("MainActivity", "User already signed in: ${currentUser.email}")
            navigateToHomeScreen(currentUser, this) // Pass the context (this)
        } else {
            Log.d("MainActivity", "No user signed in.")
        }
    }

    private fun getAndStoreFcmToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Fetch the FCM token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // If token retrieval fails, log the error
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get the FCM token
                val token = task.result

                // Send an FCM notification after successful login
                sendLoginSuccessNotification(token)
            }
        }
    }

    private fun sendLoginSuccessNotification(fcmToken: String) {
        // FCM message structure
        val message = mapOf(
            "to" to fcmToken,
            "data" to mapOf(
                "title" to "Login Successful",
                "message" to "You have logged in successfully to the app."
            )
        )

        // Send the notification via Firebase Cloud Messaging API
        FirebaseFirestore.getInstance().collection("fcm_notifications")
            .add(message)
            .addOnSuccessListener {
                Log.d("FCM", "Login success notification sent successfully.")
            }
            .addOnFailureListener { e ->
                Log.w("FCM", "Error sending login success notification", e)
            }
    }


    private fun navigateToHomeScreen(user: FirebaseUser, context: Context) {
        Firebase.firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                val name = document.getString("name") ?: "Unknown User"  // Fetch name from Firestore

                if (role != null) {
                    if (role == "patient") {
                        val intent = Intent(context, PatientActivity::class.java)
                        intent.putExtra("userName", name)  // Pass the user name
                        context.startActivity(intent)
                    } else {
                        val intent = Intent(context, DoctorActivity::class.java)
                        intent.putExtra("userName", name)  // Pass the user name
                        context.startActivity(intent)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("MainActivity", "Failed to get user role", exception)
            }
    }
}
