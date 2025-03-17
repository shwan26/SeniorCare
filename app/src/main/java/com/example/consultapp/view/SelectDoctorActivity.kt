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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.consultapp.viewmodel.CustomBottomBar
import com.example.consultapp.model.Doctor
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import setLocale
import java.util.Locale

class SelectDoctorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsultAppTheme {
                val navController = rememberNavController()
                SelectDoctorView(navController)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectDoctorView(navController: NavHostController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    var requestStatusMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var selectedLocale by remember { mutableStateOf(Locale("en")) }  // Default language to English

    val currentUser = FirebaseAuth.getInstance().currentUser
    val patientEmail = currentUser?.email ?: ""

    // Fetch doctors and request status from Firestore
    LaunchedEffect(true) {
        db.collection("users")
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { result ->
                doctors = result.map { document ->
                    Doctor(
                        name = document.getString("name") ?: "Unknown",
                        email = document.getString("email") ?: "Unknown"
                    )
                }
            }

        // Fetch request status for each doctor from Firestore
        db.collection("doctorRequests")
            .whereEqualTo("patientEmail", patientEmail)
            .get()
            .addOnSuccessListener { result ->
                val statusMap = result.associate { document ->
                    val doctorEmail = document.getString("doctorEmail") ?: ""
                    val status = document.getString("status") ?: "pending"
                    doctorEmail to status
                }
                requestStatusMap = statusMap
            }
    }

    // Apply the selected language when it changes
    LaunchedEffect(selectedLocale) {
        setLocale(context, selectedLocale.toString())
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Select Doctor",
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = { darkModeEnabled = !darkModeEnabled },
                onLanguageChanged = { newLanguage ->
                    selectedLocale = newLanguage
                }
            )
        },
        bottomBar = {
            CustomBottomBar(
                currentRoute = "selection",
                onNavigateToHome = {
                    context.startActivity(Intent(context, PatientActivity::class.java))
                },
                onNavigateToSelection = {
                    // Stay on this page (Select Doctor)
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(doctors) { doctor ->
                        val requestStatus = requestStatusMap[doctor.email] ?: "pending"
                        DoctorItem(
                            doctor = doctor,
                            requestStatus = requestStatus,
                            onRequestClicked = { requestDoctor(context, doctor) },
                            onRequestAgainClicked = { requestAgain(context, doctor) }
                        )
                    }
                }
            }
        }
    )
}


@Composable
fun DoctorItem(
    doctor: Doctor,
    requestStatus: String,
    onRequestClicked: () -> Unit,
    onRequestAgainClicked: () -> Unit
) {
    val context = LocalContext.current
    val buttonText = when (requestStatus) {
        "pending" -> "Requested"
        "accepted" -> "Accepted"
        "rejected" -> "Request Again"
        else -> "Request"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Dr. " + doctor.name)
        Button(
            onClick = {
                if (requestStatus == "accepted") {
                    Toast.makeText(context, "Doctor already accepted!", Toast.LENGTH_SHORT).show()
                } else {
                    if (requestStatus == "rejected") {
                        onRequestAgainClicked()
                    } else {
                        onRequestClicked()
                    }
                }
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(text = buttonText)
        }
    }
}

fun requestAgain(context: Context, doctor: Doctor) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val patientEmail = currentUser?.email ?: "Unknown"

    db.collection("doctorRequests")
        .whereEqualTo("patientEmail", patientEmail)
        .whereEqualTo("doctorEmail", doctor.email)
        .get()
        .addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val document = result.documents.first()
                document.reference.update("status", "pending")
                    .addOnSuccessListener {
                        Toast.makeText(context, "Request status updated to pending", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to update request status: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "No previous request found to update", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to fetch request: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun requestDoctor(context: Context, doctor: Doctor) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val patientEmail = currentUser?.email ?: "Unknown"

    db.collection("users")
        .whereEqualTo("email", patientEmail)
        .get()
        .addOnSuccessListener { result ->
            val patientName = result.firstOrNull()?.getString("name") ?: "Unknown"

            db.collection("users")
                .whereEqualTo("email", doctor.email)
                .get()
                .addOnSuccessListener { doctorResult ->
                    val doctorName = doctorResult.firstOrNull()?.getString("name") ?: "Unknown"

                    db.collection("doctorRequests")
                        .whereEqualTo("patientEmail", patientEmail)
                        .whereEqualTo("doctorEmail", doctor.email)
                        .get()
                        .addOnSuccessListener { result ->
                            if (result.isEmpty) {
                                val requestMap = mapOf(
                                    "patientEmail" to patientEmail,
                                    "doctorEmail" to doctor.email,
                                    "doctorName" to doctorName,
                                    "patientName" to patientName,
                                    "status" to "pending"
                                )

                                db.collection("doctorRequests")
                                    .add(requestMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Request sent to Dr. ${doctor.name}", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Error sending request: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(context, "Request already sent to Dr. ${doctor.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to fetch doctor's name: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to fetch patient name: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
