package com.example.consultapp.view

import CustomTopAppBar
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.consultapp.R
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.example.consultapp.viewmodel.CustomBottomBar
import com.google.firebase.auth.FirebaseAuth
import setLocale
import java.util.*

class PatientActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve passed data from MainActivity (username)
        val userName = intent.getStringExtra("userName") ?: "Unknown Patient"
        val selectedLocaleString = intent.getStringExtra("selectedLocale") ?: "en"
        val selectedLocale = Locale(selectedLocaleString)

        setContent {
            // Local state for dark mode
            var darkModeEnabled = intent.getBooleanExtra("darkModeEnabled", false) // Default to false (light mode)

            var currentLanguage by remember { mutableStateOf(selectedLocale) }

            // Wrap in theme based on dark mode state
            ConsultAppTheme(darkTheme = darkModeEnabled) {
                PatientView(
                    navController = rememberNavController(),
                    userName = userName
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientView(
    navController: NavController,
    userName: String
) {
    val context = LocalContext.current
    var darkModeEnabled by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf("en") }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Welcome, $userName",
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = {
                    darkModeEnabled = !darkModeEnabled
                },
                onLanguageChanged = { newLanguage ->
                    currentLanguage = newLanguage.toString()
                    setLocale(context, newLanguage.toString()) // Change the locale
                    context.startActivity(Intent(context, PatientActivity::class.java)) // Restart activity to apply changes
                }
            )
        },
        bottomBar = {
            CustomBottomBar(
                currentRoute = "home",
                onNavigateToHome = {
                    // current page (no navigation needed)
                },
                onNavigateToSelection = {
                    // Navigate to Select Doctor
                    context.startActivity(Intent(context, SelectDoctorActivity::class.java))
                },
                onNavigateToChat = {
                    context.startActivity(Intent(context, LiveConsultationActivity::class.java))
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
                // YouTube Video embedded using WebView
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    factory = {
                        WebView(context).apply {
                            webViewClient = WebViewClient()  // Handle links within WebView
                            webChromeClient = WebChromeClient()  // Enable JS and video support
                            loadUrl("https://youtu.be/un4ZgxT-cHM?si=hRZg1zAHaowoc2iV")  // YouTube embed link
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true  // Enable DOM storage (required for video)
                            settings.cacheMode = WebSettings.LOAD_DEFAULT  // Let WebView handle caching automatically
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons for navigation
                Button(
                    onClick = {
                        val intent = Intent(context, SelectDoctorActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = stringResource(id = R.string.select_doctor))
                }
                Button(
                    onClick = {
                        val intent = Intent(context, LiveConsultationActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = stringResource(id = R.string.live_consultation))
                }
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = stringResource(id = R.string.logout))
                }
            }
        }
    )
}
