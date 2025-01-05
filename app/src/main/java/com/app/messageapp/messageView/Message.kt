package com.app.messageapp.messageView

import androidx.room.PrimaryKey

data class Message(
    val text: String,
    val isSent: Boolean,
    val timestamp: Long,
    val isDateSeparator: Boolean = false)
