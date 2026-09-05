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
import com.shadowcalc.app.databinding.ActivityNotesBinding
import java.util.UUID

class NotesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotesBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var noteManager: NoteManager
    private var notes = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        noteManager = NoteManager(this, securityManager)
        notes = noteManager.loadNotes().toMutableList()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showNoteDialog(null) }
        refresh()
    }

    private fun refresh() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = NoteAdapter(notes)
        binding.tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showNoteDialog(note: Note?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_note, null)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTitle)
        val etContent = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etContent)
        note?.let { etTitle.setText(it.title); etContent.setText(it.content) }

        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle(if (note == null) "New Note" else "Edit Note")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().ifEmpty { "Untitled" }
                val content = etContent.text.toString()
                if (note != null) {
                    notes.remove(note)
                    notes.add(0, Note(note.id, title, content, System.currentTimeMillis(), note.folder))
                } else {
                    notes.add(0, Note(UUID.randomUUID().toString(), title, content, System.currentTimeMillis(), ""))
                }
                noteManager.saveNotes(notes)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private inner class NoteAdapter(private val list: List<Note>) : RecyclerView.Adapter<NoteAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tvTitle)
            val preview: TextView = v.findViewById(R.id.tvPreview)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_note, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val n = list[i]
            h.title.text = n.title
            h.preview.text = n.content.take(60) + if (n.content.length > 60) "..." else ""
            h.itemView.setOnClickListener { showNoteDialog(n) }
            h.itemView.setOnLongClickListener {
                AlertDialog.Builder(this@NotesActivity, R.style.DarkAlertDialog)
                    .setTitle("Delete note?")
                    .setPositiveButton("Delete") { _, _ -> notes.remove(n); noteManager.saveNotes(notes); refresh() }
                    .setNegativeButton("Cancel", null).show()
                true
            }
        }
        override fun getItemCount() = list.size
    }
}
