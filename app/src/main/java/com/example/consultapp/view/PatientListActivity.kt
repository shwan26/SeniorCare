package com.example.consultapp.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.consultapp.model.Patient
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class PatientListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsultAppTheme {
                PatientListActivityView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListActivityView() {
    val context = LocalContext.current
    val patients = remember { mutableStateListOf<Patient>() }
    val db = FirebaseFirestore.getInstance()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val doctorEmail = currentUser?.email ?: ""

    // Fetch approved patients for the doctor
    LaunchedEffect(Unit) {
        db.collection("doctorRequests")
            .whereEqualTo("doctorEmail", doctorEmail)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                patients.clear() // Clear any existing data
                result.forEach { document ->
                    val patientName = document.getString("patientName") ?: "Unknown"
                    val patientEmail = document.getString("patientEmail") ?: "Unknown"
                    patients.add(Patient(name = patientName, email = patientEmail))
                }
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Approved Patients") },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
                items(patients) { patient ->
                    Text(
                        text = patient.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(context, ChatActivityDoctor::class.java)
                                intent.putExtra("chatPartner", patient.name)
                                context.startActivity(intent)
                            }
                            .padding(16.dp)
                    )
                }
            }
        }
    )
}


