package com.app.messageapp.chat.view

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.fitnessgym.utils.MyPreferences
import com.app.messageapp.R
import com.app.messageapp.chat.SelectionCallback
import com.app.messageapp.chat.adapter.ArchiveUserAdapter
import com.app.messageapp.chat.dataModel.SmsConversation
import com.app.messageapp.databinding.ActivityArchiveUsersBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class ArchiveUsersActivity : AppCompatActivity(), SelectionCallback {

    private lateinit var binding: ActivityArchiveUsersBinding
    private lateinit var archiveUserAdapter: ArchiveUserAdapter
    private val conversationList = mutableListOf<SmsConversation>()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.app.messageapp.ACTION_ARCHIVE_USERS") {
                val archivedConversations = intent.getParcelableArrayListExtra<SmsConversation>("archivedConversations")
                archivedConversations?.let {
                    conversationList.clear()
                    conversationList.addAll(it)
                    archiveUserAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val archivedConversations = getConversations(0, 50, true)
        // Setup RecyclerView
        binding.recyclerViewConversations.layoutManager = LinearLayoutManager(this)
        archiveUserAdapter = ArchiveUserAdapter(archivedConversations, this)
        binding.recyclerViewConversations.adapter = archiveUserAdapter

    }

    private fun getConversations(
        page: Int,
        pageSize: Int,
        includeArchived: Boolean = false
    ): ArrayList<SmsConversation> {
        val smsConversations = ArrayList<SmsConversation>()
        val offset = page * pageSize
        val uri = Uri.parse("content://sms/")
        val projection = arrayOf("thread_id", "address", "date", "body", "status", "read")
        val sortOrder = "date DESC LIMIT $pageSize OFFSET $offset"

        val cursor = contentResolver.query(uri, projection, null, null, sortOrder)

        val archivedThreadIds = MyPreferences.getArchivedUsers(this)

        cursor?.use {
            val threadIdIndex = it.getColumnIndexOrThrow("thread_id")
            val bodyIndex = it.getColumnIndexOrThrow("body")
            val dateIndex = it.getColumnIndexOrThrow("date")
            val addressIndex = it.getColumnIndexOrThrow("address")
            val readIndex = it.getColumnIndex("read")

            while (it.moveToNext()) {
                val threadId = it.getString(threadIdIndex)
                val body = it.getString(bodyIndex)
                val date = it.getLong(dateIndex)
                val address = it.getString(addressIndex)
                val isRead = if (readIndex != -1) it.getInt(readIndex) == 1 else false

                val formattedTime = formatTimestamp(date)

                val isArchived = archivedThreadIds.contains(threadId)
                if (includeArchived == isArchived) {
                    smsConversations.add(SmsConversation(threadId, address, body, formattedTime, isRead))
                }
            }
        }

        return smsConversations
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        return when {
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) -> {
                timeFormat.format(Date(timestamp))
            }
            now.timeInMillis - messageTime.timeInMillis < 7 * 24 * 60 * 60 * 1000 -> {
                dayFormat.format(Date(timestamp))
            }
            else -> {
                dateFormat.format(Date(timestamp))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onSelectionModeChanged(enabled: Boolean) { }

    override fun onSelectionCountChanged(count: Int) { }
}
