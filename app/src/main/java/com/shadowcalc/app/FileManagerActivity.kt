package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class FileManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private var actionMode: ActionMode? = null
    private val selectedFiles = mutableSetOf<File>()
    private var isSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { pickFile() }
        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        selectedFiles.clear(); isSelectionMode = false; actionMode?.finish()
        val files = vaultManager.getFiles().sortedByDescending { it.lastModified() }
        binding.tvCount.text = "${files.size} files"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = FileAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
        startActivityForResult(Intent.createChooser(intent, "Select file"), 1002)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                if (vaultManager.encryptAndStore(uri, this, "file")) {
                    Toast.makeText(this, "File hidden", Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
        }
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
                    R.id.action_delete -> confirmDelete()
                    R.id.action_unhide -> confirmUnhide()
                }; return true
            }
            override fun onDestroyActionMode(mode: ActionMode?) {
                isSelectionMode = false; selectedFiles.clear()
                (binding.recyclerView.adapter as? FileAdapter)?.notifyDataSetChanged()
            }
        })
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Move ${selectedFiles.size} file(s) to Trash?")
            .setPositiveButton("Trash") { _, _ ->
                selectedFiles.forEach { vaultManager.moveToTrash(it, "file") }
                refresh()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun confirmUnhide() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Restore ${selectedFiles.size} file(s)?")
            .setPositiveButton("Restore") { _, _ ->
                var success = 0
                selectedFiles.forEach { if (vaultManager.unhideToPublic(this, it, "file")) success++ }
                Toast.makeText(this, "$success restored", Toast.LENGTH_SHORT).show(); refresh()
            }.setNegativeButton("Cancel", null).show()
    }

    private inner class FileAdapter(private val files: List<File>) : RecyclerView.Adapter<FileAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: android.widget.TextView = v.findViewById(R.id.tvName)
            val check: android.widget.ImageView = v.findViewById(R.id.ivCheck)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name.removePrefix("file_").removeSuffix(".enc")
            val isSelected = selectedFiles.contains(file)
            h.check.visibility = if (isSelected) View.VISIBLE else View.GONE
            h.itemView.alpha = if (isSelected) 0.6f else 1.0f
            h.itemView.setOnClickListener {
                if (isSelectionMode) { toggle(file); notifyItemChanged(i) }
                else { /* open file */ }
            }
            h.itemView.setOnLongClickListener {
                if (!isSelectionMode) startSelectionMode()
                toggle(file); notifyItemChanged(i); true
            }
        }
        override fun getItemCount() = files.size
        private fun toggle(file: File) {
            if (selectedFiles.contains(file)) selectedFiles.remove(file) else selectedFiles.add(file)
            actionMode?.title = "${selectedFiles.size} selected"
            if (selectedFiles.isEmpty()) actionMode?.finish()
        }
    }
}
