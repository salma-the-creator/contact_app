package com.example.contactsapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactsapp.databinding.ActivityMainBinding
import com.example.contactsapp.databinding.ItemContactBinding

// --------------------- DATA CLASS ---------------------
data class Contact(
    val name: String,
    val phoneNumber: String,
    val initial: String
)

// --------------------- MAIN ACTIVITY ---------------------
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private val contacts = mutableListOf<Contact>()
    private val filteredContacts = mutableListOf<Contact>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ربط Toolbar مع ActionBar
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        checkPermissions()
    }

    // --------------------- RecyclerView Setup ---------------------
    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(filteredContacts) { contact ->
            makePhoneCall(contact.phoneNumber)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = contactAdapter
        }
    }

    // --------------------- Permissions ---------------------
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            loadContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadContacts()
            } else {
                Toast.makeText(this, "Permissions required to use the app", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --------------------- Load Contacts ---------------------
    private fun loadContacts() {
        contacts.clear()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: "Unknown"
                val number = it.getString(numberIndex) ?: ""
                val initial = name.firstOrNull()?.uppercase() ?: "?"
                contacts.add(Contact(name, number, initial))
            }
        }

        // أول مرة عرض كل الكنتاكتس
        filteredContacts.clear()
        filteredContacts.addAll(contacts)
        binding.emptyState.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        contactAdapter.notifyDataSetChanged()
    }

    // --------------------- Make Call ---------------------
    private fun makePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Call permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    // --------------------- Menu + Search ---------------------
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem: MenuItem? = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.queryHint = "Search by name or number"

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText.orEmpty())
                return true
            }
        })
        return true
    }

    private fun filterContacts(query: String) {
        val lowerQuery = query.lowercase()
        filteredContacts.clear()
        filteredContacts.addAll(
            contacts.filter { it.name.lowercase().contains(lowerQuery) || it.phoneNumber.contains(lowerQuery) }
        )
        binding.emptyState.visibility = if (filteredContacts.isEmpty()) View.VISIBLE else View.GONE
        contactAdapter.notifyDataSetChanged()
    }

    // --------------------- Adapter ---------------------
    inner class ContactAdapter(
        private val contacts: List<Contact>,
        private val onCallClick: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

        private val colors = listOf(
            0xFF6366F1.toInt(), // Indigo
            0xFFEC4899.toInt(), // Pink
            0xFF8B5CF6.toInt(), // Purple
            0xFF10B981.toInt(), // Emerald
            0xFFF59E0B.toInt(), // Amber
            0xFF3B82F6.toInt(), // Blue
            0xFFEF4444.toInt(), // Red
            0xFF14B8A6.toInt()  // Teal
        )

        inner class ContactViewHolder(private val binding: ItemContactBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(contact: Contact) {
                binding.tvName.text = contact.name
                binding.tvPhone.text = contact.phoneNumber
                binding.tvInitial.text = contact.initial

                // لون عشوائي لكل contact
                val randomColor = colors.random()
                binding.avatarCircle.setCardBackgroundColor(randomColor)

                binding.btnCall.setOnClickListener { onCallClick(contact) }
                binding.root.setOnClickListener { onCallClick(contact) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ContactViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            holder.bind(contacts[position])
        }

        override fun getItemCount() = contacts.size
    }
}
