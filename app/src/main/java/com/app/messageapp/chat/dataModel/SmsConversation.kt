package com.app.messageapp.chat.dataModel

data class SmsConversation(
    val threadId: String,
    val address: String,
    val snippet: String,
    val time: String,
    var isRead: Boolean
)
