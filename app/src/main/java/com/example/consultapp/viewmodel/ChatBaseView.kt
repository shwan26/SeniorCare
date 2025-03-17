// ChatBaseView.kt - A common base chat component
package com.example.consultapp.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore
import com.example.consultapp.ui.components.MessageBubble
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBaseView(
    userType: String, // "doctor" or "patient"
    chatPartnerName: String,
    messagesList: MutableList<Pair<String, String>>,
    onSendMessage: (String, Uri?, Uri?) -> Unit, // Uri for image and file
    onAttachImage: () -> Unit, // Callback for image attachment
    onAttachFile: () -> Unit  // Callback for file attachment
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val message = remember { mutableStateOf("") }
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }
    val selectedFileUri = remember { mutableStateOf<Uri?>(null) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserName = currentUser?.displayName ?: "Unknown User"

    // Sanitize the chat partner name for Firebase path
    val sanitizedChatPartnerName = chatPartnerName.replace(".", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "")

    val database = FirebaseFirestore.getInstance().collection("chats").document(sanitizedChatPartnerName).collection("messages")

    // Listen for new messages
    LaunchedEffect(Unit) {
        database.orderBy("timestamp").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Chat", "Error fetching messages", error)
            } else {
                snapshot?.documents?.forEach { document ->
                    val sender = document.getString("sender") ?: "Unknown"
                    val receiver = document.getString("receiver") ?: "Unknown"
                    val messageText = document.getString("message") ?: ""
                    // Pass sender and receiver names as senderReceiverInfo
                    messagesList.add(Pair("$sender -> $receiver", messageText))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with $chatPartnerName") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messagesList) { (senderReceiverInfo, msg) ->
                        // Check if the current user is the sender (patient or doctor)
                        MessageBubble(
                            message = msg,
                            sender = senderReceiverInfo.split(" -> ")[0], // Extract sender name from senderReceiverInfo
                            currentUser = currentUserName
                        )
                    }
                }

                // Row for text field and action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Message input field
                    OutlinedTextField(
                        value = message.value,
                        onValueChange = { message.value = it },
                        label = { Text("Enter your message") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    // Button for image upload
                    IconButton(
                        onClick = { onAttachImage() },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add image")
                    }

                    // Button for file upload (PDF or other)
                    IconButton(
                        onClick = { onAttachFile() },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
                    }
                }

                // Send button
                Button(
                    onClick = {
                        if (message.value.isNotEmpty() || selectedImageUri.value != null || selectedFileUri.value != null) {
                            onSendMessage(message.value, selectedImageUri.value, selectedFileUri.value)

                            // Send notification to the patient after a message is sent
                            sendNotificationToPatient("patient@example.com", message.value)

                            // Clear input fields
                            message.value = ""
                            selectedImageUri.value = null
                            selectedFileUri.value = null
                        }
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Send")
                }
            }
        }
    )
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
