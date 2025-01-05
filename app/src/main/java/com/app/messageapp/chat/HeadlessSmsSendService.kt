package com.app.messageapp.chat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class HeadlessSmsSendService : Service() {

    companion object {
        private const val TAG = "HeadlessSmsSendService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service does not support binding
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            handleQuickResponse(it)
        }
        return START_NOT_STICKY
    }

    private fun handleQuickResponse(intent: Intent) {
        // Extract the recipient and message content from the Intent
        val recipient = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        val message = intent.getStringExtra(Intent.EXTRA_TEXT)

        Log.d(TAG, "Quick Response to: $recipient with message: $message")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}
