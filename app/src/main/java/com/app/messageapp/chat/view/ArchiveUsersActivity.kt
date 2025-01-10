package com.app.messageapp.chat.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.fitnessgym.utils.MyPreferences
import com.app.fitnessgym.utils.MyPreferences.Companion.getBlockedUsers
import com.app.fitnessgym.utils.MyPreferences.Companion.removeArchivedChats
import com.app.messageapp.chat.ConfirmationCallback
import com.app.messageapp.chat.SelectionCallback
import com.app.messageapp.chat.adapter.ConversationsAdapter
import com.app.messageapp.chat.dataModel.SmsConversation
import com.app.messageapp.chat.view.MessageListActivity.Companion.ACTION_ARCHIVE
import com.app.messageapp.chat.view.MessageListActivity.Companion.ACTION_DELETE
import com.app.messageapp.chat.view.MessageListActivity.Companion.ACTION_PIN
import com.app.messageapp.chat.view.MessageListActivity.Companion.EXTRA_READ_MESSAGE
import com.app.messageapp.chat.view.MessageListActivity.Companion.NEW_MESSAGE
import com.app.messageapp.databinding.ActivityArchiveUsersBinding
import com.app.messageapp.dialog.ConfirmationDialogFragment
import com.app.myapplication.utils.Constant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class ArchiveUsersActivity : AppCompatActivity(), SelectionCallback, ConfirmationCallback {

    private lateinit var binding: ActivityArchiveUsersBinding
    private lateinit var archiveUserAdapter: ConversationsAdapter

    private var isLoading = false
    private var currentPage = 0

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshConversations()
        }
    }
    private val smsContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            refreshConversations()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadConversations()

        val archivedConversations = getConversations(0, 50, true)
        // Setup RecyclerView
        binding.recyclerViewConversations.layoutManager = LinearLayoutManager(this)
        archiveUserAdapter = ConversationsAdapter(archivedConversations, this)
        binding.recyclerViewConversations.adapter = archiveUserAdapter

        binding.ivBack.setOnClickListener { finish() }

        binding.icUnArchive.setOnClickListener {
            ConfirmationDialogFragment(Constant.MessageType.UnArchive.toString(), this).show(supportFragmentManager, "BlurDialog")
        }

        if (archiveUserAdapter.itemCount < 0) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.recyclerViewConversations.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.recyclerViewConversations.visibility = View.VISIBLE
        }
    }

    private fun loadConversations() {
        if (isLoading) return
        isLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            val smsConversations = getConversations(0, 50, includeArchived = true)
            withContext(Dispatchers.Main) {
                archiveUserAdapter.updateOrAddConversation(smsConversations)
                archiveUserAdapter.notifyDataSetChanged()

                isLoading = false
            }
        }
    }

    private fun getConversations(page: Int, pageSize: Int, includeArchived: Boolean = false): ArrayList<SmsConversation> {
        val smsConversations = ArrayList<SmsConversation>()
        val offset = page * pageSize
        val uri = Uri.parse("content://sms/")
        val projection = arrayOf("thread_id", "address", "date", "body", "status", "read")
        val sortOrder = "date DESC LIMIT $pageSize OFFSET $offset"

        val archivedChats = MyPreferences.getArchivedChats(this)
        val blockedUsers = getBlockedUsers(this)

        val cursor = contentResolver.query(uri, projection, null, null, sortOrder)

        cursor?.use {
            val threadIdIndex = it.getColumnIndexOrThrow("thread_id")
            val bodyIndex = it.getColumnIndexOrThrow("body")
            val dateIndex = it.getColumnIndexOrThrow("date")
            val addressIndex = it.getColumnIndexOrThrow("address")
            val readIndex = it.getColumnIndex("read")

            val uniqueConversations = mutableSetOf<String>() // To avoid duplicates

            while (it.moveToNext()) {
                val threadId = it.getString(threadIdIndex)
                val body = it.getString(bodyIndex)
                val date = it.getLong(dateIndex)
                val address = it.getString(addressIndex)
                val isRead = if (readIndex != -1) it.getInt(readIndex) == 1 else false

                if (blockedUsers.contains(address)) {
                    continue // Skip blocked users
                }

                if (uniqueConversations.contains(threadId)) {
                    continue
                }

                uniqueConversations.add(threadId)

                val formattedTime = formatTimestamp(date)

                val isArchived = archivedChats.contains(threadId)
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

    private fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            packageName == Telephony.Sms.getDefaultSmsPackage(this)
        } else { true }
    }

    override fun onResume() {
        super.onResume()
        if (isDefaultSmsApp()) {

            val filter = IntentFilter().apply {
                addAction(ACTION_PIN)
                addAction(ACTION_ARCHIVE)
                addAction(ACTION_DELETE)
                addAction(EXTRA_READ_MESSAGE)
                addAction(NEW_MESSAGE)
            }
            LocalBroadcastManager.getInstance(this).registerReceiver(actionReceiver, filter)

            // Reload conversations
            if (::archiveUserAdapter.isInitialized) {
                loadConversations()
                refreshConversations()
            }
        }
    }

    private fun refreshConversations() {
        currentPage = 0
        archiveUserAdapter.clearSelection()
        loadConversations()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(actionReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(smsContentObserver)
    }

    override fun onSelectionModeChanged(enabled: Boolean) {
        binding.icUnArchive.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    override fun onSelectionCountChanged(count: Int) { }

    override fun confirmData(type: String) {
      if (type == Constant.MessageType.UnArchive.toString()) {
          val selectedItems = archiveUserAdapter.getSelectedItems()
          val threadIdsToUnarchive = selectedItems.map { it.threadId }

          if (threadIdsToUnarchive.isNotEmpty()) {
              removeArchivedChats(this,threadIdsToUnarchive)
              Toast.makeText(this, "Unarchived ${threadIdsToUnarchive.size} chat(s).", Toast.LENGTH_SHORT).show()

              // Refresh conversations
              refreshConversations()

              // Clear selection
              archiveUserAdapter.clearSelection()
          } else {
              Toast.makeText(this, "No chats selected.", Toast.LENGTH_SHORT).show()
          }
      }
    }
}