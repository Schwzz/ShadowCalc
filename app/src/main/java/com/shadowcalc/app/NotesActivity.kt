package com.shadowcalc.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shadowcalc.app.databinding.ActivityVaultBinding
import com.shadowcalc.app.databinding.DialogNoteBinding

class NotesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var noteManager: NoteManager
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        noteManager = NoteManager(this, securityManager)
        binding.tvTitle.text = "Notes"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showAddNoteDialog() }
        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val notes = noteManager.loadNotes().sortedByDescending { it.id }
        binding.tvCount.text = "${notes.size} notes"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = NoteAdapter(notes)
        binding.tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddNoteDialog(note: Note? = null) {
        val dialogBinding = DialogNoteBinding.inflate(layoutInflater)
        if (note != null) {
            dialogBinding.etTitle.setText(note.title)
            dialogBinding.etContent.setText(note.content)
        }
        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle(if (note == null) "New Note" else "Edit Note")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val title = dialogBinding.etTitle.text.toString()
                val content = dialogBinding.etContent.text.toString()
                if (title.isNotEmpty()) {
                    if (note == null) noteManager.addNote(title, content)
                    else noteManager.updateNote(note.id, title, content)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private inner class NoteAdapter(private val notes: List<Note>) : RecyclerView.Adapter<NoteAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tvName)
            val content: TextView = v.findViewById(R.id.tvUrl)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_note, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val note = notes[i]
            h.title.text = note.title
            h.content.text = note.content.take(100)
            h.itemView.setOnClickListener { showAddNoteDialog(note) }
            h.itemView.setOnLongClickListener {
                AlertDialog.Builder(this@NotesActivity, R.style.DarkAlertDialog)
                    .setTitle("Delete note?")
                    .setPositiveButton("Delete") { _, _ -> noteManager.deleteNote(note.id); refresh() }
                    .setNegativeButton("Cancel", null).show()
                true
            }
        }
        override fun getItemCount() = notes.size
    }
}
