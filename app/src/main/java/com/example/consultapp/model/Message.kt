package com.example.consultapp.model

data class Message(
    val sender: String,
    val receiver: String,
    val message: String,
    val timestamp: Long  // Timestamp should be a Long
)
