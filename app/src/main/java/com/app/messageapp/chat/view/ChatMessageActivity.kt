package com.app.messageapp.chat.view

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.messageapp.R
import com.app.messageapp.chat.adapter.ChatAdapter
import com.app.messageapp.databinding.ActivityChatMessageBinding
import com.app.messageapp.messageView.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class ChatMessageActivity: AppCompatActivity() {
    private lateinit var binding: ActivityChatMessageBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private var recipientNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setOnClickViews()
    }

    private fun initData() {
        recipientNumber = intent.getStringExtra("ADDRESS") ?: ""

        val userName = getContactName(this, recipientNumber)
        binding.tvUserName.text = userName
        markMessageAsRead(intent.getStringExtra("THREAD_ID").toString())
        chatAdapter = ChatAdapter(messages)
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this@ChatMessageActivity)
        binding.recyclerViewMessages.adapter = chatAdapter

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS),
                1
            )
        } else { loadMessages() }

        val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)
        createNotificationChannel()
    }

    private fun setOnClickViews() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString()
            if (text.isNotBlank()) {
                sendMessage(recipientNumber, text)
                val timestamp = System.currentTimeMillis()
                addMessageWithDateSeparator(Message(text, true, timestamp))
                binding.messageInput.text!!.clear()
            }
        }

        binding.ivBack.setOnClickListener { finish() }
    }

    // Load old messages for the given contact
    private fun loadMessages() {
        val uri: Uri = Uri.parse("content://sms/")
        val cursor: Cursor? = contentResolver.query(
            uri,
            arrayOf("body", "type", "date"),
            "address = ?",
            arrayOf(recipientNumber),
            "date ASC"
        )

        cursor?.use {
            val bodyIndex = it.getColumnIndex("body")
            val typeIndex = it.getColumnIndex("type")
            val dateIndex = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val body = it.getString(bodyIndex)
                val type = it.getInt(typeIndex)
                val timestamp = it.getLong(dateIndex)

                addMessageWithDateSeparator(
                    Message(
                        text = body,
                        isSent = (type == 2), // Corrected type for sent messages
                        timestamp = timestamp
                    )
                )
            }
        }
    }

    // Add a message with a date separator if needed
    private fun addMessageWithDateSeparator(newMessage: Message) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) // Ensure year is included
        val currentMessageDate = dateFormat.format(Date(newMessage.timestamp))

        // Add a separator only if the date differs from the last message's date
        if (messages.isEmpty() || dateFormat.format(Date(messages.last().timestamp)) != currentMessageDate) {
            messages.add(
                Message(
                    text = currentMessageDate, // Use the formatted date as text
                    isSent = false,            // Not a sent or received message
                    timestamp = newMessage.timestamp, // Use the actual timestamp
                    isDateSeparator = true     // Indicate it's a separator
                )
            )
        }

        messages.add(newMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
    }

    // Send SMS using SmsManager
    private fun sendMessage(recipient: String, text: String) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(recipient, null, text, null, null)
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
        }
    }

    // SMS Receiver for real-time updates
    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle["pdus"] as Array<*>
                for (pdu in pdus) {
                    val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = sms.originatingAddress
                    val messageBody = sms.messageBody
                    val timestamp = sms.timestampMillis

                    if (sender == recipientNumber) {
                        showNotification(sender, sms.messageBody)
                        addMessageWithDateSeparator(Message(messageBody, false, timestamp))
                        // Refresh layout
                        runOnUiThread { chatAdapter.notifyDataSetChanged() }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SMS_CHANNEL_ID",
                "SMS Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for incoming messages"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(sender: String, messageBody: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this@ChatMessageActivity, ChatMessageActivity::class.java)
        intent.putExtra("ADDRESS", sender)
        val pendingIntent = PendingIntent.getActivity(
            this@ChatMessageActivity,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this@ChatMessageActivity, "SMS_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle("New message from $sender")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // Get contact name from phone number
    private fun getContactName(context: Context, phoneNumber: String): String {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return phoneNumber // Return the number if no name is found
    }

    // Handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadMessages()
        }
    }

    fun markMessageAsRead(threadId: String) {
        val contentValues = ContentValues().apply {
            put("read", 1) // Set message as read
        }

        val selection = "thread_id = ?"
        val selectionArgs = arrayOf(threadId)

        contentResolver.update(Uri.parse("content://sms"), contentValues, selection, selectionArgs)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}