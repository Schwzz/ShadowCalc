package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class AudioVaultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private var actionMode: ActionMode? = null
    private val selectedFiles = mutableSetOf<File>()
    private var isSelectionMode = false
    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { importFile(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Audio"
        binding.btnAdd.setOnClickListener { pickAudio.launch("audio/*") }
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }
    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        selectedFiles.clear(); isSelectionMode = false; actionMode?.finish()
        val files = vaultManager.getAudio()
        binding.recyclerView.adapter = AudioAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun importFile(uri: android.net.Uri) {
        if (vaultManager.encryptAndStore(uri, this, "audio")) {
            Toast.makeText(this, "Audio hidden", Toast.LENGTH_SHORT).show(); refresh()
        } else { Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show() }
    }

    private fun startSelectionMode() {
        if (isSelectionMode) return
        isSelectionMode = true
        actionMode = startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menuInflater.inflate(R.menu.menu_selection, menu); return true
            }
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_delete -> confirmDeleteSelected()
                    R.id.action_unhide -> confirmUnhideSelected()
                }
                return true
            }
            override fun onDestroyActionMode(mode: ActionMode?) {
                isSelectionMode = false; selectedFiles.clear()
                (binding.recyclerView.adapter as? AudioAdapter)?.notifyDataSetChanged()
            }
        })
    }

    private fun confirmDeleteSelected() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Move ${selectedFiles.size} item(s) to Trash?")
            .setPositiveButton("Trash") { _, _ -> selectedFiles.forEach { vaultManager.moveToTrash(it, "audio") }; refresh() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmUnhideSelected() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Restore ${selectedFiles.size} item(s)?")
            .setPositiveButton("Restore") { _, _ ->
                var success = 0
                selectedFiles.forEach { if (vaultManager.unhideToPublic(this, it, "audio")) success++ }
                Toast.makeText(this, "$success restored", Toast.LENGTH_SHORT).show(); refresh()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateActionModeTitle() { actionMode?.title = "${selectedFiles.size} selected" }

    private inner class AudioAdapter(private val files: List<File>) : RecyclerView.Adapter<AudioAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val check: ImageView = v.findViewById(R.id.ivCheck)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name
            val isSelected = selectedFiles.contains(file)
            h.check.visibility = if (isSelected) View.VISIBLE else View.GONE
            h.itemView.alpha = if (isSelected) 0.6f else 1.0f

            h.itemView.setOnClickListener {
                if (isSelectionMode) { toggleSelection(file); notifyItemChanged(i) }
                else { startActivity(Intent(this@AudioVaultActivity, AudioPlayerActivity::class.java).putExtra("path", file.absolutePath)) }
            }
            h.itemView.setOnLongClickListener {
                if (!isSelectionMode) startSelectionMode()
                toggleSelection(file); notifyItemChanged(i); true
            }
        }
        override fun getItemCount() = files.size
        private fun toggleSelection(file: File) {
            if (selectedFiles.contains(file)) selectedFiles.remove(file) else selectedFiles.add(file)
            updateActionModeTitle(); if (selectedFiles.isEmpty()) actionMode?.finish()
        }
    }
}
