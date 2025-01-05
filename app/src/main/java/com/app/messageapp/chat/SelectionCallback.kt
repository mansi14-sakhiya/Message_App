package com.app.messageapp.chat

interface SelectionCallback {
    fun onSelectionModeChanged(enabled: Boolean)
    fun onSelectionCountChanged(count: Int)
}