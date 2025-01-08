package com.app.messageapp.serviceClass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.app.messageapp.chat.view.MessageListActivity

class DeleteMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MessageListActivity.ACTION_DELETE) {
            val threadIds = intent.getStringArrayListExtra(MessageListActivity.EXTRA_THREAD_IDS)
            threadIds?.let {
                for (threadId in it) {
                    val uri = Uri.parse("content://sms/conversations/$threadId")
                    context.contentResolver.delete(uri, null, null)
                }
            }
        }
    }
}