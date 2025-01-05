package com.app.messageapp.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Check if the intent action is for MMS delivery
        if (intent.action == "android.provider.Telephony.WAP_PUSH_DELIVER") {
            val mimeType = intent.type

            if (mimeType == "application/vnd.wap.mms-message") {
                Log.d(TAG, "MMS received")

                // Process the MMS data
                handleMms(intent)
            } else {
                Log.w(TAG, "Unexpected MIME type: $mimeType")
            }
        }
    }

    private fun handleMms(intent: Intent) {
        // Extract MMS data from the intent
        try {
            // Access the PDU data (Protocol Data Unit) from the intent
            val pdu = intent.getByteArrayExtra("data")
            if (pdu != null) {

                Log.d(TAG, "MMS PDU data received: ${pdu.size} bytes")
            } else {
                Log.w(TAG, "No PDU data found in the MMS intent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process MMS", e)
        }
    }
}