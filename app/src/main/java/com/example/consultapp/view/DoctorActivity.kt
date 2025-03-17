package com.example.consultapp.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class DoctorActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        auth = Firebase.auth

        setContent {
            ConsultAppTheme {
                val navController = rememberNavController()
                val user = FirebaseAuth.getInstance().currentUser
                val userName = user?.displayName ?: "Unknown Doctor"

                // Fetch and store the FCM token for push notifications
                getAndStoreFcmToken()

                // Show the doctor view with user name
                DoctorView(navController, userName)
            }
        }
    }

    private fun getAndStoreFcmToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get the FCM token and store it in Firestore
                val token = task.result
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(currentUser.uid)

                userRef.update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "FCM Token stored successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.w("FCM", "Error storing FCM token", e)
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorView(navController: NavHostController, userName: String) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome, Dr. $userName") }  // Display user name
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
                Button(onClick = {
                    val intent = Intent(context, ApprovePatientsActivity::class.java)
                    context.startActivity(intent)
                }, modifier = Modifier.padding(8.dp)) {
                    Text(text = "Approve Patients")
                }
                Button(onClick = {
                    val intent = Intent(context, PatientListActivity::class.java)
                    context.startActivity(intent)
                }, modifier = Modifier.padding(8.dp)) {
                    Text(text = "Patient List")
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
                    Text(text = "Logout")
                }
            }
        }
    )
}