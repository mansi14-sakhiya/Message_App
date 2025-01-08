package com.app.messageapp.chat.view

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.messageapp.chat.dataModel.Contact
import com.app.messageapp.chat.adapter.ContactsAdapter
import com.app.messageapp.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var contactsAdapter: ContactsAdapter
    private val contactsList = ArrayList<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RecyclerView
        contactsAdapter = ContactsAdapter(contactsList)
        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = contactsAdapter
        }

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 1)
        } else {
            loadContacts()
        }

        // Set up search functionality
        binding.etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivBack.setOnClickListener { finish() }
    }

    private fun loadContacts() {
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        val uniqueContacts = mutableListOf<Contact>() // List to store unique contacts

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                var number = it.getString(numberIndex)

                // Normalize the phone number (remove non-numeric characters)
                number = normalizePhoneNumber(number)

                // Check if the contact is blocked
                if (isBlocked(number)) {
                    continue // Skip blocked contact
                }

                // Manually check if the contact is already in the list
                if (uniqueContacts.none { contact -> contact.name == name && contact.number == number }) {
                    uniqueContacts.add(Contact(name, number))
                }
            }
        }

        contactsList.clear() // Clear the existing list before adding new data
        contactsList.addAll(uniqueContacts) // Add unique contacts
        contactsAdapter.notifyDataSetChanged() // Notify adapter of the change
    }


    private fun normalizePhoneNumber(number: String): String {
        // Remove spaces, dashes, parentheses, and other non-numeric characters
        return number.replace(Regex("[^0-9]"), "")
    }

    private fun isBlocked(number: String): Boolean {
        val cursor: Cursor? = contentResolver.query(
            BlockedNumberContract.BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ID),
            BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER + " = ?",
            arrayOf(number),
            null
        )

        val isBlocked = cursor?.use {
            it.moveToFirst()
        } ?: false

        return isBlocked
    }

    private fun filterContacts(query: String) {
        val filteredList = contactsList.filter {
            it.name.contains(query, ignoreCase = true) || it.number.contains(query)
        } as ArrayList

        if (filteredList.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.rvContacts.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.rvContacts.visibility = View.VISIBLE
        }

        contactsAdapter.updateList(filteredList)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        } else {
            Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show()
        }
    }
}
