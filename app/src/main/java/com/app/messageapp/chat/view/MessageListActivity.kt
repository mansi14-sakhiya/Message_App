package com.app.messageapp.chat.view

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.fitnessgym.utils.MyPreferences
import com.app.messageapp.R
import com.app.messageapp.chat.ConfirmationCallback
import com.app.messageapp.chat.SelectionCallback
import com.app.messageapp.chat.adapter.ConversationsAdapter
import com.app.messageapp.chat.dataModel.SmsConversation
import com.app.messageapp.databinding.ActivityMessageListBinding
import com.app.messageapp.dialog.ConfirmationDialogFragment
import com.app.messageapp.language.view.LanguageActivity
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
class MessageListActivity : AppCompatActivity(), SelectionCallback, ConfirmationCallback {
    private lateinit var binding: ActivityMessageListBinding

    private val SMS_PERMISSIONS = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private var isLoading = false
    private var currentPage = 0
    private val pageSize = 20

    private val PERMISSION_REQUEST_CODE = 101

    private lateinit var conversationsAdapter: ConversationsAdapter

    companion object {
        const val ACTION_PIN = "com.example.ACTION_PIN"
        const val ACTION_ARCHIVE = "com.example.ACTION_ARCHIVE"
        const val ACTION_DELETE = "com.example.ACTION_DELETE"
        const val ACTION_BLOCK = "com.example.ACTION_BLOCK"
        const val EXTRA_BLOCKED_NUMBERS = "com.example.EXTRA_BLOCKED_NUMBERS"
        const val EXTRA_THREAD_IDS = "com.example.EXTRA_THREAD_IDS"
        const val EXTRA_READ_MESSAGE = "com.example.ACTION_SMS_READ_STATUS_UPDATED"
        const val NEW_MESSAGE = "com.app.messageapp.NEW_SMS"
    }

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_DELETE -> {
                    val threadIds = intent.getStringArrayListExtra(EXTRA_THREAD_IDS)
                    threadIds?.forEach { threadId ->
                        val uri = Uri.withAppendedPath(Telephony.Sms.Conversations.CONTENT_URI, threadId)
                        val rowsDeleted = context.contentResolver.delete(uri, null, null)
                        if (rowsDeleted > 0) {
                           conversationsAdapter.clearSelection()
                        }
                    }
                    refreshConversations()
                }

                ACTION_BLOCK -> {
                    val blockedNumbers = intent.getStringArrayListExtra(EXTRA_BLOCKED_NUMBERS)
                    blockedNumbers?.forEach { number ->
                        // Handle blocked number (you could add them to a block list or mark them as blocked)
                        blockNumber(this@MessageListActivity, number)
                    }
                    Toast.makeText(context, "Chats blocked", Toast.LENGTH_SHORT).show()
                }

                ACTION_ARCHIVE -> {
                    val threadIds = intent.getStringArrayListExtra(EXTRA_THREAD_IDS)
                    threadIds?.forEach {
                        // Archive the conversations (hide or move them to an archive folder)
//                        conversationsAdapter.archiveConversation(threadId)
                    }
                    Toast.makeText(context, "Chats archived", Toast.LENGTH_SHORT).show()
                }

                NEW_MESSAGE -> {
                    // Handle new SMS
                    val sender = intent.getStringExtra("sender") ?: ""
                    val body = intent.getStringExtra("body") ?: ""
                    Log.d("MessageListActivity", "New SMS received: $sender - $body")
                }

                else -> {
                    val threadId = intent.getStringExtra("THREAD_ID")
                    val isRead = intent.getBooleanExtra("IS_READ", false)
                    if (threadId != null) {
                        updateMessageReadStatus(threadId, isRead)
                    }
                }
            }
            refreshConversations()
        }
    }

    private val smsContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            refreshConversations()
        }
    }

    private val setDefaultSmsAppLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            ActivityCompat.requestPermissions(this, SMS_PERMISSIONS, PERMISSION_REQUEST_CODE)
            checkAndRequestPermissions()
        } else {
            binding.recyclerViewConversations.visibility = View.GONE
            binding.clDefaultApp.visibility = View.VISIBLE
            binding.ivMenu.isEnabled = false
            binding.icSearch.isEnabled = false
            checkDefaultSmsApp(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setOnClickViews()
//        checkAndRequestPermissions()
    }

    private fun initData() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            binding.recyclerViewConversations.visibility = View.GONE
            binding.clDefaultApp.visibility = View.VISIBLE
            binding.ivMenu.isEnabled = false
            binding.icSearch.isEnabled = false
            checkDefaultSmsApp(this)
        } else {
            binding.recyclerViewConversations.visibility = View.VISIBLE
            binding.clDefaultApp.visibility = View.GONE
            binding.ivMenu.isEnabled = true
            binding.icSearch.isEnabled = true
            loadConversations()
        }
        binding.recyclerViewConversations.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewConversations.setHasFixedSize(true)

        // Initialize Adapter
        conversationsAdapter = ConversationsAdapter(ArrayList(), this)
        binding.recyclerViewConversations.adapter = conversationsAdapter

        // Register ContentObserver
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, smsContentObserver)

        binding.recyclerViewConversations.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && totalItemCount <= (lastVisibleItem + 5)) {
                    currentPage++
                    loadConversations()
                }
            }
        })
    }

    private fun setOnClickViews() {
        binding.ivMenu.setOnClickListener {
            if (binding.etSearchBar.visibility == View.VISIBLE) {
                binding.ivMenu.setImageResource(R.drawable.ic_header_menu)
                binding.etSearchBar.visibility = View.GONE
            } else {
                binding.clMenuBar.visibility = View.VISIBLE
            }
        }

        binding.clMenuBar.setOnClickListener {
            binding.clMenuBar.visibility = View.GONE
        }

        binding.clStartChat.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.icSearch.setOnClickListener {
            binding.etSearchBar.visibility = View.VISIBLE
            binding.ivMenu.setImageResource(R.drawable.ic_arrow_back)
        }

        binding.ivDeleteMsg.setOnClickListener {
            ConfirmationDialogFragment(Constant.MessageType.Delete.toString(), this).show(supportFragmentManager, "BlurDialog")
        }

        binding.ivBlockMsg.setOnClickListener {
            ConfirmationDialogFragment(Constant.MessageType.Block.toString(), this).show(supportFragmentManager, "BlurDialog")
        }

        binding.tvArchive.setOnClickListener {
            startActivity(Intent(this, ArchiveUsersActivity::class.java))
        }

        binding.ivArchiveMsg.setOnClickListener {
            ConfirmationDialogFragment(Constant.MessageType.Archive.toString(), this).show(supportFragmentManager, "BlurDialog")
        }

        binding.icPinMsg.setOnClickListener {
            val selectedItems = conversationsAdapter.getSelectedItems()
            val threadIds = selectedItems.map { it.threadId }

            threadIds.forEach {
                conversationsAdapter.togglePin(it, this)
            }
        }

        binding.btnSetDefaultApp.setOnClickListener {
            checkDefaultSmsApp(this)
        }

        binding.tvLanguage.setOnClickListener {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        binding.tvShareApp.setOnClickListener { shareApp() }

        binding.tvRateUs.setOnClickListener { openRateUs() }

//        binding.etSearchBar.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if (s!!.isNotEmpty()) {
//                    filterContacts(s.toString())
//                } else {
//                    refreshConversations()
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//        })
    }


    private fun filterContacts(query: String) {
        val filteredList = conversationsAdapter.conversations.filter {
            it.address.contains(query, ignoreCase = true) || it.address.contains(query)
        } as ArrayList

        if (filteredList.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.recyclerViewConversations.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.recyclerViewConversations.visibility = View.VISIBLE
        }

        conversationsAdapter.updateOrAddConversation(filteredList)
    }

    private fun checkDefaultSmsApp(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                setDefaultSmsAppLauncher.launch(intent)
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            setDefaultSmsAppLauncher.launch(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = SMS_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            binding.recyclerViewConversations.visibility = View.VISIBLE
            binding.clDefaultApp.visibility = View.GONE
            binding.ivMenu.isEnabled = true
            binding.icSearch.isEnabled = true
            loadConversations()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                binding.recyclerViewConversations.visibility = View.VISIBLE
                binding.clDefaultApp.visibility = View.GONE
                binding.ivMenu.isEnabled = true
                binding.icSearch.isEnabled = true
                loadConversations()
            } else {
                Toast.makeText(this, "Permissions denied. The app cannot function properly.", Toast.LENGTH_SHORT).show()
                binding.recyclerViewConversations.visibility = View.GONE
                binding.clDefaultApp.visibility = View.VISIBLE
                binding.ivMenu.isEnabled = false
                binding.icSearch.isEnabled = false
            }
        }
    }

    private fun loadConversations() {
        if (isLoading) return
        isLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            val smsConversations = getConversations(currentPage, pageSize)
            withContext(Dispatchers.Main) {
                if (currentPage == 0) {
                    conversationsAdapter.updateOrAddConversation(smsConversations)
                } else {
                    conversationsAdapter.addConversations(smsConversations)
                }
                isLoading = false
            }
        }
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

            val uniqueConversations = mutableSetOf<String>() // To track unique threadIds and avoid duplicates

            while (it.moveToNext()) {
                val threadId = it.getString(threadIdIndex)
                val body = it.getString(bodyIndex)
                val date = it.getLong(dateIndex)
                val address = it.getString(addressIndex)
                val isRead = if (readIndex != -1) it.getInt(readIndex) == 1 else false

                // Skip duplicates based on threadId
                if (uniqueConversations.contains(threadId)) {
                    continue
                }

                uniqueConversations.add(threadId)

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

    private fun refreshConversations() {
        currentPage = 0
        loadConversations()
        binding.clTool.visibility = View.GONE
        binding.clHeader.visibility = View.VISIBLE
    }

    private fun updateMessageReadStatus(threadId: String, isRead: Boolean) {
        conversationsAdapter.updateMessageReadStatus(threadId, isRead)
    }

    private fun openRateUs() {
        try {
            // Try opening Google Play Store
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to opening in a web browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun shareApp() {
        val appLink = "https://play.google.com/store/apps/details?id=$packageName"
        val shareMessage = "Check out this amazing app: $appLink"
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "My App")
        intent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(intent, "Share App via"))
    }

    private fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            packageName == Telephony.Sms.getDefaultSmsPackage(this)
        } else {
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDefaultSmsApp()) {
            binding.ivMenu.setImageResource(R.drawable.ic_header_menu)
            binding.etSearchBar.visibility = View.GONE

            val filter = IntentFilter().apply {
                addAction(ACTION_PIN)
                addAction(ACTION_ARCHIVE)
                addAction(ACTION_DELETE)
                addAction(EXTRA_READ_MESSAGE)
                addAction(NEW_MESSAGE)
            }
            LocalBroadcastManager.getInstance(this).registerReceiver(actionReceiver, filter)

            // Reload conversations
            if (::conversationsAdapter.isInitialized) {
                refreshConversations()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(actionReceiver)
    }

    private fun blockNumber(context: Context, phoneNumber: String) {
        try {
            // Query the contacts to find the contact ID for the phone number
            val uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val cursor = context.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup._ID), null, null, null)

            cursor?.use {
                // Ensure the cursor is not empty and the column exists
                val contactIdIndex = it.getColumnIndex(android.provider.ContactsContract.PhoneLookup._ID)
                if (contactIdIndex != -1 && it.moveToFirst()) {
                    val contactId = it.getString(contactIdIndex)

                    // Add a "Blocked" note to the contact
                    val values = ContentValues().apply {
                        put(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        put(android.provider.ContactsContract.Data.DATA1, "Blocked")
                    }

                    // Update the contact with the blocked status
                    val contactUri = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, contactId)
                    context.contentResolver.update(contactUri, values, null, null)

                    Log.d("MessageListActivity", "Blocked number: $phoneNumber")
                } else {
                    Log.w("MessageListActivity", "Contact not found for number: $phoneNumber")
                }
            }
        } catch (e: Exception) {
            Log.e("MessageListActivity", "Error blocking number: $phoneNumber", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(smsContentObserver)
    }

    override fun onSelectionModeChanged(enabled: Boolean) {
        binding.clTool.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.clHeader.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun onSelectionCountChanged(count: Int) {
        binding.tvSelectedTotal.text = "$count ${getString(R.string.selectd)}"
    }

    override fun confirmData(type: String) {
//        conversationsAdapter.clearSelection()
        when(type) {
            Constant.MessageType.Delete.toString() -> {
                val selectedItems = conversationsAdapter.getSelectedItems()
                val threadIds = selectedItems.map { it.threadId }
                Log.e("mansi", "----$threadIds, ${selectedItems}")
                val intent = Intent(ACTION_DELETE).apply {
                    putStringArrayListExtra(EXTRA_THREAD_IDS, ArrayList(threadIds))
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }

            Constant.MessageType.Block.toString() -> {
                val selectedItems = conversationsAdapter.getSelectedItems()
                val blockedNumbers = selectedItems.map { it.address }

                val intent = Intent(ACTION_BLOCK).apply {
                    putStringArrayListExtra(EXTRA_BLOCKED_NUMBERS, ArrayList(blockedNumbers))
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }

            Constant.MessageType.Archive.toString() -> {
                val selectedItems = conversationsAdapter.getSelectedItems()
                val threadIds = selectedItems.map { it.threadId }

                val intent = Intent(ACTION_ARCHIVE).apply {
                    putStringArrayListExtra(EXTRA_THREAD_IDS, ArrayList(threadIds))
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }
        }
    }
}