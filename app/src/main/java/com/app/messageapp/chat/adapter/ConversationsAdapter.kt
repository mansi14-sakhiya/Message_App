package com.app.messageapp.chat.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.messageapp.R
import com.app.messageapp.chat.SelectionCallback
import com.app.messageapp.chat.view.ChatMessageActivity
import com.app.messageapp.chat.dataModel.SmsConversation

class ConversationsAdapter(   private val conversations: ArrayList<SmsConversation>,
                              private val callback: SelectionCallback
) : RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<Int>() // Tracks selected positions
    private var isSelectionMode = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val addressTextView: TextView = view.findViewById(R.id.textViewAddress)
        val snippetTextView: TextView = view.findViewById(R.id.textViewSnippet)
        val tvMessageTime: TextView = view.findViewById(R.id.tvMessageTime)
        val ivMessageRead: ImageView = view.findViewById(R.id.ivMessageRead)
        val ivMessageSelected: ImageView = view.findViewById(R.id.ivMessageSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        val userName = getContactName(holder.itemView.context, conversation.address)
        holder.addressTextView.text = userName
        holder.snippetTextView.text = conversation.snippet
        holder.tvMessageTime.text = conversation.time

        Log.e("getReadStatus","---${conversation.isRead}")
        if (!conversation.isRead) {
            holder.ivMessageRead.visibility = View.VISIBLE
        } else {
            holder.ivMessageRead.visibility = View.GONE
        }

        // Highlight if selected
        if (selectedItems.contains(position)) {
            holder.ivMessageSelected.visibility = View.VISIBLE
            holder.addressTextView.setTextColor(holder.itemView.context.getColor(R.color.wizardBlue))
            holder.snippetTextView.setTextColor(holder.itemView.context.getColor(R.color.wizardBlue))
            holder.tvMessageTime.setTextColor(holder.itemView.context.getColor(R.color.wizardBlue))
        } else {
            holder.ivMessageSelected.visibility = View.GONE
            holder.addressTextView.setTextColor(holder.itemView.context.getColor(R.color.black))
            holder.snippetTextView.setTextColor(holder.itemView.context.getColor(R.color.gray_text))
            holder.tvMessageTime.setTextColor(holder.itemView.context.getColor(R.color.gray_text))
        }

        // Click to open ChatActivity (if not in selection mode)
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                val context = holder.itemView.context
                val intent = Intent(context, ChatMessageActivity::class.java)
                intent.putExtra("THREAD_ID", conversation.threadId)
                intent.putExtra("ADDRESS", conversation.address)
                context.startActivity(intent)
            }
        }

        // Long click to start selection mode
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                toggleSelection(position)
                callback.onSelectionModeChanged(true)
            }
            true
        }
    }

    override fun getItemCount(): Int = conversations.size

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        notifyItemChanged(position)
        callback.onSelectionCountChanged(selectedItems.size)

        if (selectedItems.isEmpty()) {
            isSelectionMode = false
            callback.onSelectionModeChanged(false)
        }
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    fun updateOrAddConversation(newConversation: SmsConversation): Boolean {
        val position = conversations.indexOfFirst { it.threadId == newConversation.threadId }
        return if (position != -1) {
            // Update existing conversation
            conversations[position] = newConversation
            notifyItemChanged(position)
            true // Indicates that the conversation was updated
        } else {
            // Add new conversation
            conversations.add(0, newConversation) // Add to the top of the list
            notifyItemInserted(0)
            true // Indicates that a new conversation was added
        }
    }

    fun getSelectedItems(): List<SmsConversation> =
        selectedItems.map { conversations[it] }

    private fun getContactName(context: Context, phoneNumber: String): String {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return phoneNumber // Return the number if no name is found
    }

    fun addConversations(newConversations: List<SmsConversation>) {
        val existingIds = conversations.map { it.threadId }.toSet() // Get existing thread IDs
        val filteredConversations = newConversations.filterNot { it.threadId in existingIds } // Exclude duplicates
        conversations.addAll(filteredConversations)
        notifyDataSetChanged()
    }

    fun removeConversation(threadId: String) {
        val index = conversations.indexOfFirst { it.threadId == threadId }
        if (index >= 0) {
            conversations.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateMessageReadStatus(threadId: String, isRead: Boolean) {
        val position = conversations.indexOfFirst { it.threadId == threadId }
        if (position != -1) {
            conversations[position].isRead = isRead
            notifyItemChanged(position)
        }
    }
}
