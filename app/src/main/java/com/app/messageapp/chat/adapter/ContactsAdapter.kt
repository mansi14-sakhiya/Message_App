package com.app.messageapp.chat.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.messageapp.chat.view.ChatMessageActivity
import com.app.messageapp.chat.dataModel.Contact
import com.app.messageapp.databinding.ItemConversationBinding

class ContactsAdapter(private var contacts: ArrayList<Contact>) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.binding.tvMessageTime.visibility = View.GONE
        holder.binding.textViewAddress.text = contact.name
        holder.binding.textViewSnippet.text = contact.number

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ChatMessageActivity::class.java)
            intent.putExtra("ADDRESS", contact.number) // Pass the contact's number
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateList(newContacts: ArrayList<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    class ContactViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root)
}