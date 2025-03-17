package com.example.consultapp.view

import CustomTopAppBar
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.consultapp.model.Doctor
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.example.consultapp.viewmodel.CustomBottomBar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import setLocale

class LiveConsultationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsultAppTheme {
                ConsultationHistoryView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationHistoryView() {
    val context = LocalContext.current
    val doctors = remember { mutableStateListOf<Doctor>() }
    val db = FirebaseFirestore.getInstance()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val patientEmail = currentUser?.email ?: ""
    var darkModeEnabled by remember { mutableStateOf(false) }

    var currentLanguage by remember { mutableStateOf("en") } // Default language

    // Callback for changing language
    val onLanguageChanged = @androidx.compose.runtime.Composable {
        currentLanguage = if (currentLanguage == "en") "th" else "en"
        setLocale(LocalContext.current, currentLanguage) // Change the locale for language update
    }

    // Fetch doctors who have accepted the patient
    LaunchedEffect(Unit) {
        db.collection("doctorRequests")
            .whereEqualTo("patientEmail", patientEmail)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                doctors.clear() // Clear any existing data
                result.forEach { document ->
                    val doctorName = document.getString("doctorName") ?: "Unknown"
                    val doctorEmail = document.getString("doctorEmail") ?: "Unknown"
                    // Add the doctor's name and email to the list
                    doctors.add(Doctor(name = doctorName, email = doctorEmail))
                }
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Live Consultations",
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = { darkModeEnabled = !darkModeEnabled },
                onLanguageChanged = {
                    val newLanguage = if (currentLanguage == "en") "th" else "en"
                    currentLanguage = newLanguage
                    setLocale(context, newLanguage)
                    context.startActivity(Intent(context, PatientActivity::class.java)) // Restart activity
                }
            )
        },
        bottomBar = {
            CustomBottomBar(
                currentRoute = "chat",
                onNavigateToHome = {
                    // Navigate to Home (Patient View)
                    context.startActivity(Intent(context, PatientActivity::class.java))
                },
                onNavigateToSelection = {
                    // Navigate to Select Doctor
                    context.startActivity(Intent(context, SelectDoctorActivity::class.java))
                },
                onNavigateToChat = {
                    // Stay on Chat page (current page)
                }
            )
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(doctors) { doctor ->
                    Text(
                        text = "Dr. " + doctor.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(context, ChatActivityPatient::class.java)
                                intent.putExtra("doctorName", doctor.name)
                                context.startActivity(intent)
                            }
                            .padding(16.dp)
                    )
                }
            }
        }
    )
}
