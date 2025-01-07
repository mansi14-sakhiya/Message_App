package com.app.messageapp.messageView

data class Message(
    val text: String,
    val isSent: Boolean,
    val timestamp: Long,
    val isDateSeparator: Boolean = false)
