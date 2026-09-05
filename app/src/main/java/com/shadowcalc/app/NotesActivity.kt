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
import com.shadowcalc.app.databinding.ActivityNotesBinding
import com.shadowcalc.app.databinding.DialogNoteBinding

class NotesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotesBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var noteManager: NoteManager
    private var editingNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        noteManager = NoteManager(this, securityManager)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showNoteDialog(null) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        refresh()
    }

    private fun refresh() {
        val notes = noteManager.loadNotes()
        binding.recyclerView.adapter = NoteAdapter(notes)
        binding.tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showNoteDialog(note: Note?) {
        editingNote = note
        val dialogBinding = DialogNoteBinding.inflate(layoutInflater)
        note?.let {
            dialogBinding.etTitle.setText(it.title)
            dialogBinding.etContent.setText(it.content)
        }
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle(if (note == null) "New Note" else "Edit Note")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val title = dialogBinding.etTitle.text.toString()
                val content = dialogBinding.etContent.text.toString()
                if (title.isNotEmpty()) {
                    if (note == null) noteManager.saveNote(title, content)
                    else noteManager.updateNote(note.id, title, content)
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .apply { if (note != null) setNeutralButton("Delete") { _, _ -> noteManager.deleteNote(note.id); refresh() } }
            .show()
    }

    private inner class NoteAdapter(private val notes: List<Note>) : RecyclerView.Adapter<NoteAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tvTitle)
            val preview: TextView = v.findViewById(R.id.tvPreview)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_note, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val n = notes[i]
            h.title.text = n.title
            h.preview.text = n.content
            h.itemView.setOnClickListener { showNoteDialog(n) }
        }
        override fun getItemCount() = notes.size
    }
}