package com.example.consultapp.model

data class AppointmentRequest(
    var id: String = "",  // Firestore document ID
    val patientName: String = "",
    val patientEmail: String = "",
    val doctorEmail: String = "",
    val status: String = "pending"  // Default status is "pending"
)
