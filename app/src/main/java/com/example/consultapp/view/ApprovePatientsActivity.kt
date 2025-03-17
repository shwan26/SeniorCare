package com.example.consultapp.view

import CustomTopAppBar
import android.app.Activity
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
import com.example.consultapp.model.AppointmentRequest
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import setLocale
import java.util.Locale

class ApprovePatientsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            ConsultAppTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                ApproveAppointmentsView(
                    navController = navController,
                    darkModeEnabled = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@Composable
fun ApproveAppointmentsView(
    navController: NavHostController,
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf("en") } // Default language

    // Callback for changing language
    val onLanguageChanged = { newLanguage: Locale ->
        currentLanguage = newLanguage.toString()
        setLocale(context, newLanguage.toString()) // Change the locale
        context.startActivity(Intent(context, ApprovePatientsActivity::class.java)) // Restart the activity to apply changes
    }

    val requests = remember { mutableStateListOf<AppointmentRequest>() }
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val doctorEmail = currentUser?.email ?: ""

    // Fetch patient requests for the current doctor
    LaunchedEffect(doctorEmail) {
        db.collection("doctorRequests")
            .whereEqualTo("doctorEmail", doctorEmail)  // Fetch by doctorEmail
            .whereEqualTo("status", "pending")  // Only fetch pending requests
            .get()
            .addOnSuccessListener { result ->
                requests.clear()  // Clear previous data
                for (document in result) {
                    val appointment = document.toObject(AppointmentRequest::class.java)
                    appointment.id = document.id  // Set the Firestore document ID as the 'id' of the request
                    requests.add(appointment)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error fetching requests: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = "Approve Patients",
                onBack = { (context as? Activity)?.finish() },
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = onToggleDarkMode,
                onLanguageChanged = onLanguageChanged // Pass the language change callback
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (requests.isEmpty()) {
                    Text("No requests available")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(requests) { request ->
                            AppointmentRequestItemUI(
                                request = request,
                                onAccept = {
                                    // Accept the request: Update the status to 'accepted'
                                    updateRequestStatus(db, context, request.id, "accepted", request.patientEmail)
                                },
                                onReject = {
                                    // Reject the request: Update the status to 'rejected'
                                    updateRequestStatus(db, context, request.id, "rejected", request.patientEmail)
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun AppointmentRequestItemUI(
    request: AppointmentRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display the patient’s name
        Text(text = request.patientName, modifier = Modifier.weight(1f))
        // Accept button (tick)
        Button(onClick = onAccept, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text("✓")
        }
        // Reject button (cross)
        Button(onClick = onReject, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text("✕")
        }
    }
}

fun updateRequestStatus(db: FirebaseFirestore, context: android.content.Context, requestId: String, newStatus: String, patientEmail: String) {
    // Update the status of the request in Firestore
    db.collection("doctorRequests").document(requestId)
        .update("status", newStatus)
        .addOnSuccessListener {
            // Handle successful status update, removing from view if accepted or rejected
            Toast.makeText(context, "Request $newStatus", Toast.LENGTH_SHORT).show()

            if (newStatus == "accepted") {
                // Send notification to the patient
                sendNotificationToPatient(patientEmail, "Your appointment request has been approved!")
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}


fun sendNotificationToPatient(patientEmail: String, message: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").whereEqualTo("email", patientEmail).get()
        .addOnSuccessListener { result ->
            for (document in result) {
                val fcmToken = document.getString("fcmToken")
                fcmToken?.let {
                    sendFCMMessage(it, message)
                }
            }
        }
}

fun sendFCMMessage(fcmToken: String, message: String) {
    val data = mapOf("message" to message)

    val fcmService = FirebaseMessaging.getInstance()
    val message = RemoteMessage.Builder("$fcmToken@gcm.googleapis.com")
        .setMessageId(System.currentTimeMillis().toString())
        .setData(data)
        .build()

    fcmService.send(message)
}
