package com.app.messageapp.chat.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.messageapp.databinding.ItemChatMessageBinding
import com.app.messageapp.databinding.ItemDateSeparatorBinding
import com.app.messageapp.messageView.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val messages: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DATE_SEPARATOR = 0
        private const val VIEW_TYPE_MESSAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isDateSeparator) VIEW_TYPE_DATE_SEPARATOR else VIEW_TYPE_MESSAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_SEPARATOR -> {
                val binding = ItemDateSeparatorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateSeparatorViewHolder(binding)
            }
            else -> {
                val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChatViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DateSeparatorViewHolder) {
            holder.binding.tvMessageTime.text = messages[position].text
        } else if (holder is ChatViewHolder) {
            if (messages[position].isSent) {
                holder.binding.tvSenderMessage.text = messages[position].text
                holder.binding.clReceiver.visibility = View.GONE
                holder.binding.clSender.visibility = View.VISIBLE
            } else {
                holder.binding.tvReceiverMessage.text = messages[position].text
                holder.binding.clReceiver.visibility = View.VISIBLE
                holder.binding.clSender.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    class ChatViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    class DateSeparatorViewHolder(val binding: ItemDateSeparatorBinding) : RecyclerView.ViewHolder(binding.root) {
    }
}