package com.app.messageapp.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.messageapp.R

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

                        Log.d("MySmsReceiver", "Received SMS from: $sender, Message: $body")

                        // Handle the received message
                    }
                }
            }
        }
    }
}
