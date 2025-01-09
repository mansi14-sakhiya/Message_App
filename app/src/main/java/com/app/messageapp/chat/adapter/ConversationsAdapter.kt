package com.app.messageapp.chat.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.messageapp.R
import com.app.messageapp.chat.SelectionCallback
import com.app.messageapp.chat.view.ChatMessageActivity
import com.app.messageapp.chat.dataModel.SmsConversation

class ConversationsAdapter( val conversations: ArrayList<SmsConversation>,
                            private val callback: SelectionCallback
) : RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {

    private val selectedItems = ArrayList<Int>() // Tracks selected positions
    private var isSelectionMode = false
    private var pinnedChats = ArrayList<String>()

    // ViewHolder class to bind views
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
        val context = holder.itemView.context

        // Update contact name and message details
        holder.addressTextView.text = getContactName(context, conversation.address)
        holder.snippetTextView.text = conversation.snippet
        holder.tvMessageTime.text = conversation.time

        // Update read status visibility
        holder.ivMessageRead.visibility = if (conversation.isRead) View.GONE else View.VISIBLE

        // Highlight selection
        updateSelectionUI(holder, position)

        // Item click listeners
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                openChatActivity(context, conversation)
            }
        }

        holder.itemView.setOnLongClickListener {
            startSelectionMode(position)
            true
        }

        val isPinned = pinnedChats.contains(conversation.threadId)
        holder.itemView.findViewById<ImageView>(R.id.ivPinChat).visibility = if (isPinned) View.VISIBLE else View.GONE

    }

    override fun getItemCount(): Int = conversations.size

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)  // Deselect item
        } else {
            selectedItems.add(position)  // Select item
        }

        // Notify item change to update the UI
        notifyItemChanged(position)

        // Update the selection count for the callback
        callback.onSelectionCountChanged(selectedItems.size)

        // If no items are selected, exit selection mode
        if (selectedItems.isEmpty()) {
            isSelectionMode = false
            callback.onSelectionModeChanged(false)
        }

    }

    // Starts selection mode
    private fun startSelectionMode(position: Int) {
        if (!isSelectionMode) {
            isSelectionMode = true
            toggleSelection(position)
            callback.onSelectionModeChanged(true)
        }
    }

    // Updates UI for selection state
    private fun updateSelectionUI(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        if (selectedItems.contains(position)) {
            holder.ivMessageSelected.visibility = View.VISIBLE
            setTextColor(holder, context.getColor(R.color.wizardBlue))
        } else {
            holder.ivMessageSelected.visibility = View.GONE
            setTextColor(holder, context.getColor(R.color.black), context.getColor(R.color.gray_text))
        }
    }

    private fun setTextColor(holder: ViewHolder, addressColor: Int, snippetColor: Int = addressColor) {
        holder.addressTextView.setTextColor(addressColor)
        holder.snippetTextView.setTextColor(snippetColor)
        holder.tvMessageTime.setTextColor(snippetColor)
    }

    // Opens the chat activity for the given conversation
    private fun openChatActivity(context: Context, conversation: SmsConversation) {
        val intent = Intent(context, ChatMessageActivity::class.java).apply {
            putExtra("THREAD_ID", conversation.threadId)
            putExtra("ADDRESS", conversation.address)
        }
        context.startActivity(intent)
    }

    fun reorderConversations() {
        // Sort conversations so that pinned chats come first
        conversations.sortWith { conv1, conv2 ->
            val isPinned1 = pinnedChats.contains(conv1.threadId)
            val isPinned2 = pinnedChats.contains(conv2.threadId)

            when {
                isPinned1 && !isPinned2 -> -1  // Pin conv1 to top
                !isPinned1 && isPinned2 -> 1   // Pin conv2 to top
                else -> 0                      // Keep order unchanged
            }
        }
    }

    // Clears all selections
    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    // Updates or adds a conversation
    fun updateOrAddConversation(newConversation: List<SmsConversation>) {
        conversations.clear()
        conversations.addAll(newConversation)
        reorderConversations()
        notifyDataSetChanged()
    }

    // Returns the list of selected conversations
    fun getSelectedItems(): ArrayList<SmsConversation> {
        return selectedItems.mapNotNull { position ->
            if (position in conversations.indices) {
                conversations[position]
            } else {
                null
            }
        } as ArrayList<SmsConversation>
    }

    // Retrieves the contact name for a given phone number
    private fun getContactName(context: Context, phoneNumber: String): String {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return phoneNumber
    }

    // Adds new conversations, filtering out duplicates
    fun addConversations(newConversations: List<SmsConversation>) {
        val existingIds = conversations.map { it.threadId }.toSet()
        val filteredConversations = newConversations.filterNot { it.threadId in existingIds }
        conversations.addAll(filteredConversations)
        notifyDataSetChanged()
    }

    fun setPinnedChats(pinnedChats: ArrayList<String>) {
        this.pinnedChats.clear()
        this.pinnedChats.addAll(pinnedChats)
        notifyDataSetChanged()
    }

    fun removeConversations(threadIds: List<String>) {
        // Remove all conversations with the given threadIds from the current list
        conversations.removeAll { threadIds.contains(it.threadId) }
    }

    // Updates the read status of a conversation
    fun updateMessageReadStatus(threadId: String, isRead: Boolean) {
        val position = conversations.indexOfFirst { it.threadId == threadId }
        if (position != -1) {
            conversations[position].isRead = isRead
            notifyItemChanged(position)
        }
    }
}


