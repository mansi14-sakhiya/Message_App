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
import androidx.recyclerview.widget.RecyclerView
import com.app.messageapp.chat.dataModel.Contact
import com.app.messageapp.chat.adapter.ContactsAdapter
import com.app.messageapp.databinding.ActivitySearchBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(DelicateCoroutinesApi::class)
class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var contactsAdapter: ContactsAdapter
    private val contactsList = ArrayList<Contact>()
    private val allContacts = ArrayList<Contact>()
    private var isLoading = false
    private var currentPage = 0
    private val pageSize = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setOnClickViews()
    }

    private fun initData() {
        contactsAdapter = ContactsAdapter(contactsList)

        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = contactsAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (!isLoading && lastVisibleItem == contactsList.size - 1) {
                        loadMoreContacts()
                    }
                }
            })
        }

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 1)
        } else {
            loadAllContacts()
        }
    }

    private fun setOnClickViews() {
        binding.etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivBack.setOnClickListener { finish() }
    }

    private fun loadAllContacts() {
        GlobalScope.launch(Dispatchers.IO) {
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

            val uniqueContacts = mutableListOf<Contact>()

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    var number = it.getString(numberIndex)
                    number = normalizePhoneNumber(number)
                    if (!isBlocked(number) && uniqueContacts.none { c -> c.name == name && c.number == number }) {
                        uniqueContacts.add(Contact(name, number))
                    }
                }
            }

            cursor?.close()

            allContacts.addAll(uniqueContacts)

            withContext(Dispatchers.Main) {
                loadMoreContacts() // Update UI on the main thread
            }
        }
    }

    private fun loadMoreContacts() {
        if (currentPage * pageSize >= allContacts.size) return // No more contacts to load

        isLoading = true
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, allContacts.size)

        contactsList.addAll(allContacts.subList(start, end))
        contactsAdapter.notifyDataSetChanged()

        currentPage++
        isLoading = false
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
        val filteredList = if (query.isEmpty()) {
            allContacts.take(currentPage * pageSize)
        } else {
            allContacts.filter {
                it.name.contains(query, ignoreCase = true) || it.number.contains(query)
            }
        }

        contactsList.clear()
        contactsList.addAll(filteredList)

        if (contactsList.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.rvContacts.visibility = View.GONE
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.rvContacts.visibility = View.VISIBLE
        }

        contactsAdapter.notifyDataSetChanged()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAllContacts()
        }
    }
}
