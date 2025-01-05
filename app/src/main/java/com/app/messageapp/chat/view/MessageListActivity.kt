package com.app.messageapp.chat.view

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.messageapp.R
import com.app.messageapp.chat.SelectionCallback
import com.app.messageapp.chat.adapter.ConversationsAdapter
import com.app.messageapp.chat.dataModel.SmsConversation
import com.app.messageapp.databinding.ActivityMessageListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Suppress("DEPRECATION")
class MessageListActivity : AppCompatActivity(), SelectionCallback {
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

    val startActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

    private lateinit var conversationsAdapter : ConversationsAdapter

    companion object {
        const val ACTION_PIN = "com.example.ACTION_PIN"
        const val ACTION_ARCHIVE = "com.example.ACTION_ARCHIVE"
        const val ACTION_DELETE = "com.example.ACTION_DELETE"
        const val EXTRA_THREAD_IDS = "com.example.EXTRA_THREAD_IDS"
        const val EXTRA_READ_MESSAGE = "com.example.ACTION_SMS_READ_STATUS_UPDATED"
    }

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val threadId = intent.getStringExtra("THREAD_ID")
            val isRead = intent.getBooleanExtra("IS_READ", false)
            if (threadId != null) {
                updateMessageReadStatus(threadId, isRead)
            }
            refreshConversations()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setOnClickViews()
        listInstalledSmsApps()
    }

    private fun initData() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            binding.recyclerViewConversations.visibility = View.GONE
            binding.clDefaultApp.visibility = View.VISIBLE
            binding.ivMenu.isEnabled = false
            binding.icSearch.isEnabled = false
        } else {
            binding.recyclerViewConversations.visibility = View.VISIBLE
            binding.clDefaultApp.visibility = View.GONE
            binding.ivMenu.isEnabled = true
            binding.icSearch.isEnabled = true
            loadConversations()
        }
        binding.recyclerViewConversations.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewConversations.setHasFixedSize(true)

        val intent = Intent("UPDATE_CONVERSATIONS")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

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
            binding.clMenuBar.visibility = View.VISIBLE
        }

        binding.clMenuBar.setOnClickListener {
            binding.clMenuBar.visibility = View.GONE
        }

        binding.icSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.ivDeleteMsg.setOnClickListener {
            val selectedItems = conversationsAdapter.getSelectedItems()
            val threadIds = selectedItems.map { it.threadId }

            val intent = Intent(ACTION_DELETE).apply {
                putStringArrayListExtra(EXTRA_THREAD_IDS, ArrayList(threadIds))
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        binding.ivArchiveMsg.setOnClickListener {
            val selectedItems = conversationsAdapter.getSelectedItems()
            val threadIds = selectedItems.map { it.threadId }

            val intent = Intent(ACTION_ARCHIVE).apply {
                putStringArrayListExtra(EXTRA_THREAD_IDS, ArrayList(threadIds))
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        binding.icPinMsg.setOnClickListener {
            val selectedItems = conversationsAdapter.getSelectedItems()
            val threadIds = selectedItems.map { it.threadId }

            val intent = Intent(ACTION_PIN).apply {
                putStringArrayListExtra(EXTRA_THREAD_IDS, ArrayList(threadIds))
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        binding.btnSetDefaultApp.setOnClickListener {
            checkDefaultSmsApp(this)
        }
    }

    private fun listInstalledSmsApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
        val smsApps = pm.queryIntentActivities(intent, 0)

        for (app in smsApps) {
            Log.d("InstalledSMSApp", "App: ${app.activityInfo.packageName}")
        }
    }

    private fun checkDefaultSmsApp(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                startActivityForResult.launch(intent)
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            context.startActivity(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        SMS_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadConversations()
                binding.recyclerViewConversations.visibility = View.VISIBLE
                binding.clDefaultApp.visibility = View.GONE
                binding.ivMenu.isEnabled = true
                binding.icSearch.isEnabled = true
            } else {
                Toast.makeText(this, "Permissions denied. The app cannot function properly.", Toast.LENGTH_SHORT).show()
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
                    conversationsAdapter = ConversationsAdapter(smsConversations, this@MessageListActivity)
                    binding.recyclerViewConversations.adapter = conversationsAdapter
                } else {
                    conversationsAdapter.addConversations(smsConversations)
                }
                isLoading = false
            }
        }
    }

    private fun getConversations(page: Int, pageSize: Int): ArrayList<SmsConversation> {
        val smsConversations = ArrayList<SmsConversation>()
        val offset = page * pageSize
        val uri = Uri.parse("content://sms/")
        val projection = arrayOf("thread_id", "address", "date", "body", "status", "read") // "read" might not always be available
        val sortOrder = "date DESC LIMIT $pageSize OFFSET $offset"

        val cursor = contentResolver.query(uri, projection, null, null, sortOrder)

        cursor?.use {
            val readColumnIndex = it.getColumnIndexOrThrow("read")
            while (it.moveToNext()) {
                val threadId = it.getString(it.getColumnIndexOrThrow("thread_id"))
                val snippet = it.getString(it.getColumnIndexOrThrow("body"))

                // Check if the column exists and retrieve the value
                val isRead = try {
                    it.getInt(readColumnIndex) == 1 // 1 means read, 0 means unread
                } catch (e: Exception) {
                    false // Default to false if the column is not available
                }

                val address = it.getString(it.getColumnIndexOrThrow("address"))
                val timestamp = it.getLong(it.getColumnIndexOrThrow("date"))
                val formattedTime = formatTimestamp(timestamp)

                smsConversations.add(SmsConversation(threadId, address, snippet, formattedTime, isRead))
            }
        }
        return smsConversations

    }

    private fun getTimestampFromThreadId(threadId: String): Long {
        val uri = Uri.parse("content://sms")
        val projection = arrayOf("date")
        val selection = "thread_id = ?"
        val selectionArgs = arrayOf(threadId)

        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, "date DESC")
        var timestamp: Long = 0
        cursor?.use {
            if (it.moveToFirst()) {
                timestamp = it.getLong(it.getColumnIndexOrThrow("date"))
            }
        }
        return timestamp
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
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

    private fun getAddressFromThreadId(threadId: String): String {
        val uri = Uri.parse("content://sms")
        val projection = arrayOf("address")
        val selection = "thread_id = ?"
        val selectionArgs = arrayOf(threadId)

        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, "date DESC")
        var address = "Unknown"
        cursor?.use {
            if (it.moveToFirst()) {
                address = it.getString(it.getColumnIndexOrThrow("address"))
            }
        }
        return address
    }

    private fun refreshConversations() {
        loadConversations()
        binding.clTool.visibility = View.GONE
        binding.clHeader.visibility = View.VISIBLE
        conversationsAdapter.notifyDataSetChanged()
    }

    private fun updateMessageReadStatus(threadId: String, isRead: Boolean) {
        conversationsAdapter.updateMessageReadStatus(threadId, isRead)
    }

    //rate us option click
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

  // share app click
  private fun shareApp() {
        val appLink = "https://play.google.com/store/apps/details?id=$packageName"
        val shareMessage = "Check out this amazing app: $appLink"
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "My App")
        intent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(intent, "Share App via"))
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(ACTION_PIN)
            addAction(ACTION_ARCHIVE)
            addAction(ACTION_DELETE)
            addAction(EXTRA_READ_MESSAGE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(actionReceiver, filter)

        // Reload conversations
        if (::conversationsAdapter.isInitialized) {
            refreshConversations()
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(actionReceiver)
    }

    override fun onSelectionModeChanged(enabled: Boolean) {
        binding.clTool.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.clHeader.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun onSelectionCountChanged(count: Int) {
        binding.tvSelectedTotal.text = count.toString() + " " + getString(R.string.selectd)
    }
}
