package com.app.messageapp.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.app.messageapp.R
import com.app.messageapp.chat.view.ChatMessageActivity


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
                val newIntent = Intent("com.app.messageapp.NEW_SMS").apply {
                    putExtra("sender", sender)
                    putExtra("body", messageBody)
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent)
                saveSmsToDatabase(context, sender!!, messageBody)
                showNotification(context, sender, messageBody)
            }
        }
    }

    private fun saveSmsToDatabase(context: Context, sender: String, content: String) {
        val uri = Uri.parse("content://sms/inbox")
        val cursor = context.contentResolver.query(uri, arrayOf("address", "body"), "address = ? AND body = ?", arrayOf(sender, content), null)
        if (cursor?.count == 0) {
            val values = ContentValues()
            values.put(Telephony.Sms.ADDRESS, sender)
            values.put(Telephony.Sms.BODY, content)
            values.put(Telephony.Sms.DATE, System.currentTimeMillis())
            values.put(Telephony.Sms.READ, 0) // Unread
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            context.contentResolver.insert(Uri.parse("content://sms/inbox"), values)
        }
        cursor?.close()
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

        val userName = getContactName(context, sender)

        // Create an Intent to open the chat screen
        val chatIntent = Intent(context, ChatMessageActivity::class.java).apply {
            putExtra("ADDRESS", sender)
            putExtra("message", message)
            putExtra("type", "message")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Create a PendingIntent for the notification
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            chatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE is required on Android 12+
        )

        // Create the notification
        val notification = NotificationCompat.Builder(context, "sms_channel")
            .setContentTitle(userName)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(sender.hashCode(), notification)
    }
    private fun getContactName(context: Context, phoneNumber: String): String {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return phoneNumber
    }
}