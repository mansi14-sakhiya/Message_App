package com.app.messageapp.serviceClass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.messageapp.chat.view.MessageListActivity

class BlockMessageReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MessageListActivity.ACTION_BLOCK) {
            val blockedNumbers = intent.getStringArrayListExtra(MessageListActivity.EXTRA_BLOCKED_NUMBERS)
            blockedNumbers?.let {
                // Add numbers to your blocked list (e.g., store in preferences or database)
                for (number in it) {
                    blockNumber(context, number)
                }
            }
        }
    }

    private fun blockNumber(context: Context, number: String) {
        val sharedPreferences = context.getSharedPreferences("blocked_numbers", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean(number, true)
            apply()
        }
    }
}
