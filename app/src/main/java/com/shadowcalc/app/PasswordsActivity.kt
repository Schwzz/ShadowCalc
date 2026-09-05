package com.shadowcalc.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shadowcalc.app.databinding.ActivityPasswordsBinding
import java.util.UUID

class PasswordsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordsBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var passwordManager: PasswordManager
    private var entries = mutableListOf<PasswordEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        passwordManager = PasswordManager(this, securityManager)
        entries = passwordManager.loadEntries().toMutableList()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showEntryDialog(null) }
        refresh()
    }

    private fun refresh() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = PasswordAdapter(entries)
        binding.tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showEntryDialog(entry: PasswordEntry?) {
        val view = layoutInflater.inflate(R.layout.dialog_password, null)
        val etTitle = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTitle)
        val etUsername = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUsername)
        val etPassword = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val etUrl = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUrl)
        val etNotes = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNotes)

        entry?.let {
            etTitle.setText(it.title)
            etUsername.setText(it.username)
            etPassword.setText(it.password)
            etUrl.setText(it.url)
            etNotes.setText(it.notes)
        }

        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle(if (entry == null) "New Password" else "Edit Password")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val newEntry = PasswordEntry(
                    entry?.id ?: UUID.randomUUID().toString(),
                    etTitle.text.toString().ifEmpty { "Untitled" },
                    etUsername.text.toString(),
                    etPassword.text.toString(),
                    etUrl.text.toString(),
                    etNotes.text.toString(),
                    System.currentTimeMillis()
                )
                if (entry != null) entries.remove(entry)
                entries.add(0, newEntry)
                passwordManager.saveEntries(entries)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private inner class PasswordAdapter(private val list: List<PasswordEntry>) : RecyclerView.Adapter<PasswordAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tvTitle)
            val username: TextView = v.findViewById(R.id.tvUsername)
            val copy: ImageView = v.findViewById(R.id.btnCopy)
            val del: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_password, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val e = list[i]
            h.title.text = e.title
            h.username.text = e.username.ifEmpty { "No username" }
            h.itemView.setOnClickListener { showEntryDialog(e) }
            h.copy.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("password", e.password))
                Toast.makeText(this@PasswordsActivity, "Password copied", Toast.LENGTH_SHORT).show()
            }
            h.del.setOnClickListener {
                AlertDialog.Builder(this@PasswordsActivity, R.style.DarkAlertDialog)
                    .setTitle("Delete password?")
                    .setPositiveButton("Delete") { _, _ -> entries.remove(e); passwordManager.saveEntries(entries); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = list.size
    }
}
