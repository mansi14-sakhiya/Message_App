package com.app.messageapp.chat.view

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony.Sms
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
    private val conversationList = ArrayList<SmsConversation>()

    private val ACTION_ARCHIVE = "com.example.ACTION_ARCHIVE"
    private val ACTION_PIN = "com.example.ACTION_PIN"
    private val EXTRA_THREAD_IDS = "com.example.EXTRA_THREAD_IDS"

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_ARCHIVE -> {
                    val archivedChats = getArchivedChats()
                    updateArchivedList(archivedChats)
                }
                ACTION_PIN -> {
                    // Handle PIN action
                }
            }
        }
    }

    // BroadcastReceiver to listen for updates on archived users
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

        val archivedConversations = getConversations(0, 50, true) as ArrayList
        // Setup RecyclerView
        binding.recyclerViewConversations.layoutManager = LinearLayoutManager(this)
        archiveUserAdapter = ArchiveUserAdapter(archivedConversations, this)
        binding.recyclerViewConversations.adapter = archiveUserAdapter
    }

    private fun getArchivedChats(): List<String> {
        val sharedPreferences = getSharedPreferences("ChatPrefs", MODE_PRIVATE)
        return sharedPreferences.getStringSet("ARCHIVED_CHATS", emptySet())!!.toList()
    }

    // Update UI for archived chats
    private fun updateArchivedList(archivedChats: List<String>) {
        // Fetch the full chat details based on threadIds
        val chats = fetchChatsByThreadIds(archivedChats) as ArrayList
        archiveUserAdapter.updateChats(chats)
    }

    // Fetch chat details by threadIds
    private fun fetchChatsByThreadIds(threadIds: List<String>): List<SmsConversation> {
        // Example implementation - replace with actual logic to fetch chat details
        return threadIds.mapNotNull { threadId ->
            getChatDetails(threadId)
        }
    }

    private fun getConversations(offset: Int, limit: Int, archived: Boolean): List<SmsConversation> {
        val sharedPreferences = getSharedPreferences("ChatPrefs", MODE_PRIVATE)
        val conversations = mutableListOf<SmsConversation>()

        for (i in offset until (offset + limit)) {
            val threadId = sharedPreferences.getString("threadId_$i", null) ?: continue
            val address = sharedPreferences.getString("address_$i", "") ?: "Unknown"
            val snippet = sharedPreferences.getString("snippet_$i", "") ?: "No snippet"
            val time = sharedPreferences.getString("time_$i", "") ?: "Unknown"
            val isRead = sharedPreferences.getBoolean("isRead_$i", true)
            val isArchived = sharedPreferences.getBoolean("isArchived_$i", archived)
            val isPinned = sharedPreferences.getBoolean("isPinned_$i", false)

            val conversation = SmsConversation(threadId, address, snippet, time, isRead, isArchived, isPinned)
            if (archived == isArchived) {
                conversations.add(conversation)
            }
        }

        return conversations
    }

    private fun getChatDetails(threadId: String): SmsConversation? {
        // Fetch chat details from the database or SharedPreferences
        val sharedPreferences = getSharedPreferences("ChatPrefs", MODE_PRIVATE)
        val address = sharedPreferences.getString("$threadId.address", "") ?: "Unknown"
        val snippet = sharedPreferences.getString("$threadId.snippet", "") ?: "No snippet"
        val time = sharedPreferences.getString("$threadId.time", "") ?: "Unknown"
        val isRead = sharedPreferences.getBoolean("$threadId.isRead", true)
        val isArchived = sharedPreferences.getBoolean("$threadId.isArchived", false)
        val isPinned = sharedPreferences.getBoolean("$threadId.isPinned", false)

        return SmsConversation(threadId, address, snippet, time, isRead, isArchived, isPinned)
    }

    override fun onResume() {
        super.onResume()
        // Register receiver for receiving updates
        val filter = IntentFilter().apply {
            addAction(ACTION_ARCHIVE)
            addAction(ACTION_PIN)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter)

        // Load initial data
        updateArchivedList(getArchivedChats())
    }

    override fun onPause() {
        super.onPause()
        // Unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
    }

    override fun onSelectionModeChanged(enabled: Boolean) { }

    override fun onSelectionCountChanged(count: Int) { }
}