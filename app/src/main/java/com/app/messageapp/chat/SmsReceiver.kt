package com.app.messageapp.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import com.app.messageapp.R

@Suppress("DEPRECATION")
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as Array<*>
            for (pdu in pdus) {
                val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                val sender = smsMessage.originatingAddress
                val messageBody = smsMessage.messageBody

                // Show a notification when an SMS is received
                showNotification(context, sender!!, messageBody)
            }
        }
    }

    private fun showNotification(context: Context, sender: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "sms_channel"
            val channelName = "SMS Notifications"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification
        val notification = NotificationCompat.Builder(context, "sms_channel")
            .setContentTitle("New SMS from $sender")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(0, notification)
    }
}