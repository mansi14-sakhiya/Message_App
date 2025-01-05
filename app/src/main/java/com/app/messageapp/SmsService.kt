package com.app.messageapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class SmsService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("SmsService", "SmsService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SmsService", "SmsService started.")
        // Handle SMS-related logic here (if needed)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("SmsService", "SmsService binding.")
        // Return null because we do not need to bind this service to a component
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SmsService", "SmsService destroyed.")
    }
}