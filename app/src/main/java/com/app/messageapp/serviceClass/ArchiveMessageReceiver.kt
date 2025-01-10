package com.app.messageapp.serviceClass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.app.fitnessgym.utils.MyPreferences

class ArchiveMessageReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.app.messageapp.ACTION_ARCHIVE") {
//            val threadIds = intent.getStringArrayListExtra("THREAD_IDS") ?: return

            // Save thread IDs to preferences
//            val archivedIds = MyPreferences.getArchivedUsers(context).toMutableSet()
//            archivedIds.addAll(threadIds)
//            MyPreferences.saveArchivedUsers(context, archivedIds)


        Toast.makeText(context, "Chats archived", Toast.LENGTH_SHORT).show()
        }
    }
}