package com.app.messageapp.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.messageapp.R
import com.app.messageapp.chat.view.ChatMessageActivity

@Suppress("DEPRECATION")
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as? Array<*>
                if (pdus != null) {
                    for (pdu in pdus) {
                        val message = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = message.displayOriginatingAddress
                        val body = message.messageBody

                        Log.d("SmsReceiver", "SMS received from: $sender - $body")

                        // Handle the SMS (e.g., save to database, show notification)
                        handleSms(context, sender, body)
                    }
                }
            }
        }
    }

    private fun handleSms(context: Context, sender: String, messageBody: String) {
        // Show notification for the received message
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, ChatMessageActivity::class.java).apply {
            putExtra("ADDRESS", sender)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            sender.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SMS_CHANNEL_ID",
                "SMS Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for incoming messages"
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "SMS_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle("New message from $sender")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}