package com.example.consultapp.view

import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.example.consultapp.ui.theme.ConsultAppTheme
import com.example.consultapp.viewmodel.ChatBaseView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.GenericTypeIndicator

class ChatActivityPatient : ComponentActivity() {

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadImageToFirebase(it)
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadFileToFirebase(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsultAppTheme {
                // Get the doctor name from intent
                val doctorName = intent.getStringExtra("doctorName") ?: "Unknown Doctor"
                val currentUser = FirebaseAuth.getInstance().currentUser
                val patientName = currentUser?.displayName ?: "Unknown Patient"
                val messagesList = remember { mutableStateListOf<Pair<String, String>>() }

                // Generate unique chat room ID (e.g., Far-Lucas)
                val chatRoomId = getChatRoomId(patientName, doctorName)
                val db = FirebaseDatabase.getInstance().reference.child("chats").child(chatRoomId)

                // Using GenericTypeIndicator to fix deserialization issue
                val mapType = object : GenericTypeIndicator<Map<String, Any>>() {}
                db.addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val messageMap = snapshot.getValue(mapType)
                        val sender = messageMap?.get("sender") as? String ?: "Unknown"
                        val receiver = messageMap?.get("receiver") as? String ?: "Unknown"
                        val message = messageMap?.get("message") as? String ?: ""
                        val timestamp = messageMap?.get("timestamp") as? Long ?: 0L

                        val formattedTimestamp = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(timestamp))
                        messagesList.add(Pair("$sender -> $receiver", "$message at $formattedTimestamp"))
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: DataSnapshot) {}
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {}
                })

                ChatBaseView(
                    userType = "Patient",
                    chatPartnerName = doctorName,
                    messagesList = messagesList,
                    onSendMessage = { message, imageUri, fileUri ->
                        val senderName = patientName
                        val receiverName = doctorName

                        val messageData = mapOf(
                            "sender" to senderName,
                            "receiver" to receiverName,
                            "message" to message,
                            "timestamp" to System.currentTimeMillis()
                        )

                        db.push().setValue(messageData)

                        if (imageUri != null) {
                            uploadImageToFirebase(imageUri)
                        }

                        if (fileUri != null) {
                            uploadFileToFirebase(fileUri)
                        }
                    },
                    onAttachImage = {
                        pickImageLauncher.launch("image/*")
                    },
                    onAttachFile = {
                        pickFileLauncher.launch("*/*")
                    }
                )
            }
        }
    }

    private fun getChatRoomId(user1: String, user2: String): String {
        return "${user1}-${user2}"  // This will create a dynamic chat room ID like "Far-Lucas"
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("chat_images/${imageUri.lastPathSegment}")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val messageData = mapOf(
                        "sender" to "Patient",
                        "message" to uri.toString()
                    )
                    val db = FirebaseDatabase.getInstance().reference.child("chats")
                    db.push().setValue(messageData)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadFileToFirebase(fileUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("chat_files/${fileUri.lastPathSegment}")
        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val messageData = mapOf(
                        "sender" to "Patient",
                        "message" to uri.toString()
                    )
                    val db = FirebaseDatabase.getInstance().reference.child("chats")
                    db.push().setValue(messageData)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "File upload failed", Toast.LENGTH_SHORT).show()
            }
    }
}
